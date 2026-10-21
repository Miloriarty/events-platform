Кроме трёх уже готовых (`config-server`, `discovery-server`, `user-service`) остаются три обязательных сервиса и один необязательный.

## Общая картина
(остался get away)
| Сервис | Порт | БД | Владеет | Ходит в |
|---|---|---|---|---|
| config-server ✅ | 8888 | нет (git) | конфиги | нет |
| discovery-server ✅ | 8761 | нет | реестр сервисов | нет |
| user-service ✅ | 8081 | `user_db` | Users, Roles, выдача JWT | нет |
| **event-service** | 8082 | `event_db` | Events, Categories, Locations | нет |
| **booking-service** | 8083 | `booking_db` | Orders, OrderItems, Tickets, Registrations | event-service |
| **api-gateway** | 8080 | нет | маршруты, проверка JWT | все (через Eureka) |
| notification-service (по желанию) | 8084 | `notification_db` | история уведомлений | слушает брокер |

```
Клиент -> api-gateway :8080 --> user-service    :8081
           (проверка JWT)   |--> event-service   :8082
                            |--> booking-service :8083 ->Feign-> event-service
                                       |
                                       |->(по желанию, событие)-> notification-service

Все сервисы: конфиг <- config-server :8888,  регистрация -> discovery-server :8761
```

## event-service

**Ответственность:** каталог мероприятий. Здесь живут `Events`, `Categories`, `Locations`. Они связаны между собой через обычные JPA-связи (`Event → Category`, `Event → Location`), потому что все три сущности в одном сервисе и в одной БД.

**Что меняется по сравнению с монолитом:**
- `EventsModel.organizer` (ссылка на `UsersModel`) становится полем `organizerId` типа `Long`. Организатор берётся из заголовка `X-User-Id`, который проставит Gateway, поэтому клиент не может указать чужой id.
- Обратные списки (`registrations`, `tickets` внутри `EventsModel`, если они есть) удаляются, потому что эти сущности уедут в `booking-service`.

**Эндпоинты (примерно):**
- `GET/POST/PUT/DELETE /api/events`, поиск по категории, локации, дате;
- `/api/categories`, `/api/locations`: справочники;
- `GET /api/events/{id}`: им будет пользоваться `booking-service` (существует ли мероприятие, вместимость, цена).

**Правила доступа** (проверит Gateway): читать могут все, создавать и менять только `MANAGER` и `ADMIN`. Менеджер может менять только свои мероприятия, это проверяется внутри сервиса: `organizerId == X-User-Id` (или роль `ADMIN`).

**Ключевое решение:** сервис хранит **вместимость** (`capacity`), но не знает, сколько мест занято. Это знает `booking-service`. Так у каждого факта один владелец.

## booking-service

**Ответственность:** всё, что связано с покупкой и участием: `Orders`, `OrderItems`, `Tickets`, `Registrations`. Эти сущности тесно связаны и должны меняться в одной транзакции (заказ и билеты создаются вместе), поэтому их нельзя разносить по разным сервисам.

**Что меняется:**
- `userId` и `eventId` становятся простыми полями `Long`, без `@ManyToOne`.
- `OrderItems` хранит **цену на момент покупки**, а не ссылку на текущую цену мероприятия. Если менеджер потом изменит цену, старые заказы не должны поменяться.

**Как проходит оформление заказа:**
1. Gateway проверил токен и передал `X-User-Id`.
2. `booking-service` через **Feign** спрашивает `event-service`: есть ли мероприятие, какова вместимость и цена.
3. Считает у себя уже занятые места (`count` по `eventId`) и сравнивает с вместимостью.
4. В одной транзакции создаёт `Order`, `OrderItems`, `Tickets`/`Registration`.

Здесь видно, зачем нужен **Circuit Breaker**: если `event-service` не отвечает, вызов оборачивается в Resilience4j, а при срабатывании fallback клиент получает понятное «сервис временно недоступен» вместо зависшего запроса.

**Проверка пользователя:** в `user-service` ходить на каждый заказ не нужно. Раз токен валиден и `userId` пришёл от Gateway, пользователь существует. Feign в `user-service` понадобится только там, где нужны данные о пользователе (например, имя в списке регистраций для менеджера). Меньше связей между сервисами означает меньше мест, которые могут сломаться.

**Доступ к данным:** обычный пользователь видит только свои заказы (`userId == X-User-Id`), `ADMIN` видит все. Это тоже проверка внутри сервиса по заголовку роли.

**Открытый вопрос (из шага user-service):** что делать с заказами удалённого пользователя. Предлагаю самый простой вариант: запретить удалять пользователя, у которого есть заказы, или перейти на «мягкое удаление» (флаг `active=false`). Решим, когда дойдём до кода.

## api-gateway

**Ответственность:** единственная точка входа для клиента. Своей БД нет.

**Что делает:**
1. **Маршрутизация** через Eureka (`lb://имя-сервиса`):
    - `/api/auth/**`, `/api/users/**`, `/api/roles/**` → `user-service`
    - `/api/events/**`, `/api/categories/**`, `/api/locations/**` → `event-service`
    - `/api/orders/**`, `/api/registrations/**`, `/api/tickets/**` → `booking-service`
2. **Проверка JWT** (глобальный фильтр): берёт секрет из того же config-repo, проверяет подпись и срок, достаёт `sub` и `role`.
3. **Правила по путям и ролям** (то, что раньше было в `SecurityConfig`):
    - `/api/auth/**`: всем;
    - `GET /api/events/**`, категории, локации: всем (аналог вашего `/public-events`);
    - изменение мероприятий: `MANAGER`, `ADMIN`;
    - `/api/users/**`, `/api/roles/**`: только `ADMIN`;
    - заказы, билеты, регистрации: любой авторизованный.
4. **Передача личности дальше:** добавляет заголовки `X-User-Id` и `X-User-Role`.

**Важная деталь безопасности:** до добавления своих заголовков Gateway должен **удалить** такие же заголовки, пришедшие от клиента. Иначе кто угодно отправит `X-User-Role: ADMIN` и обманет сервисы. Это обязательная часть фильтра.

**Особенности реализации:**
- Gateway работает на реактивном стеке (WebFlux), поэтому ваш старый сервлетный `JwtFilter` не подойдёт. Библиотеку JJWT использовать можно, фильтр пишется заново.
- В Spring Cloud 2025.x стартеры Gateway переименовали, точное имя зависимости и префикс настроек сверим при реализации.
- Здесь же удобно собрать единый Swagger UI из документаций всех сервисов.

## notification-service (необязательный)

**Идея:** после создания заказа или регистрации пользователю уходит уведомление. Это единственный сервис, который показывает **асинхронное** взаимодействие: `booking-service` публикует событие `order.created` в брокер (RabbitMQ), а `notification-service` его читает и пишет в лог (или «отправляет письмо»). Заказ при этом не ждёт отправки.

**Цена:** нужно поднять брокер (проще всего в Docker) и добавить зависимость `spring-boot-starter-amqp`. Для зачёта обязательных требований он не нужен. Я бы делал его последним, если останется время.

## Порядок реализации

1. ✅ config-server, discovery-server, user-service
2. event-service
3. booking-service (с Feign и Circuit Breaker)
4. api-gateway (JWT, маршруты, роли)
5. Сквозная проверка, Swagger через Gateway
6. По желанию: notification-service

---

## Всё для запуска и работы (кроме того, что подтягивается из git-репозитория конфигов)

### 1. Проверка инструментов

```bash
java -version      # должна быть 25
mvn -version       # Maven 3.9+ (или запускать из IDEA, она использует встроенный)
psql --version
git --version
```

### 2. PostgreSQL: 

Создание баз, по одной на сервис (три нужны обязательно, четвёртая для необязательного сервиса):

```bash
CREATE DATABASE user_db;
CREATE DATABASE event_db;
CREATE DATABASE booking_db;
CREATE DATABASE notification_db;   # только если будете делать notification-service
```


### 3. Пароль БД

В конфигах стоит `password: ${DB_PASSWORD:}`: пароль берётся из переменной окружения и по умолчанию пустой.

```bash
export DB_PASSWORD=postgres
```

В IntelliJ IDEA это делается в Run Configuration каждого сервиса: *Environment variables → `DB_PASSWORD=postgres`*.

### 4. Таблицы и начальные данные

Таблицы создавать вручную **не нужно**: в конфигах `ddl-auto: update`, Hibernate создаст их при первом запуске сервиса. Начальные данные создаёт `DataInitializer` в `user-service`: роли `ADMIN`, `MANAGER`, `USER` и пользователь `admin` / `admin123`. Категории и локации (в `event_db`) будут пустыми, добавите их через API после готовности `event-service`.

### 6. Порядок запуска

Каждый сервис в своём терминале (или Run Configuration в IDEA). Порядок важен:

```bash
# 1. Config Server
mvn -pl config-server spring-boot:run

# 2. Eureka (когда Config Server поднялся)
mvn -pl discovery-server spring-boot:run

# 3. Бизнес-сервисы (в любом порядке, когда Eureka поднялась)
mvn -pl user-service spring-boot:run
mvn -pl event-service spring-boot:run       # после реализации
mvn -pl booking-service spring-boot:run     # после реализации

# 4. Gateway запускается последним
mvn -pl api-gateway spring-boot:run         # после реализации
```

Почему именно такой порядок: сервисы при старте забирают конфиг у Config Server (без `optional:` они упадут, если его нет) и регистрируются в Eureka.

### 7. Проверка, что всё живо

| Что | Адрес |
|---|---|
| Конфиг для сервиса | http://localhost:8888/user-service/default |
| Дашборд Eureka (все сервисы в списке) | http://localhost:8761 |
| Здоровье сервиса (Actuator) | http://localhost:8081/actuator/health |
| Swagger сервиса | http://localhost:8081/swagger-ui.html |

После появления Gateway все обращения идут через порт 8080:

```bash
# получить токен
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# запрос с токеном
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/users

# без токена должно вернуться 401
curl -i localhost:8080/api/users
```

### 8. Проверка Circuit Breaker (когда будет готов booking-service)

Остановите `event-service` (Ctrl+C) и отправьте запрос на создание заказа. Ожидаемое поведение: быстрый ответ с понятным сообщением от fallback, а не зависание и не `500` с трассировкой.

### 9. Если меняете конфиги в `events-config-repo`

Config Server читает только закоммиченное:

```bash
cd path-to-events-config-repo
git add . && git commit -m "Описание изменения"
```

Сервисы подхватывают новое значение при следующем запуске (перезапустите нужный сервис).

### 10. `.gitignore` для `events-platform`

Если проект будет в git, создайте в корне `events-platform/.gitignore`:

```
target/
.idea/
*.iml
.vscode/
```

Репозиторий `events-config-repo` остаётся отдельным. Чтобы он не оказался вложенным в `events-platform`, держите его рядом.

