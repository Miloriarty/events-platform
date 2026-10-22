# Events Platform

Микросервисное приложение для управления мероприятиями на Spring Cloud: Config Server, Eureka, Spring Cloud Gateway, Circuit Breaker (Resilience4j), Actuator, Swagger для каждого сервиса.

Стек: Java 25, Spring Boot 4.0, Spring Cloud 2025.1, PostgreSQL.

## Общая картина

| Сервис | Порт | БД | Владеет | Ходит в |
|---|---|---|---|---|
| config-server ✅ | 8888 | нет (git) | конфиги | нет |
| discovery-server ✅ | 8761 | нет | реестр сервисов | нет |
| user-service ✅ | 8081 | `user_db` | Users, Roles, выдача JWT | нет |
| event-service ✅ | 8082 | `event_db` | Events, Categories, Locations | нет |
| booking-service ✅ | 8083 | `booking_db` | Tickets, Orders, OrderItems, Registrations | event-service |
| api-gateway ✅ | 8080 | нет | маршруты, проверка JWT и ролей, общий Swagger | все (через Eureka) |
| notification-service (по желанию) | 8084 | `notification_db` | история уведомлений | слушает брокер |

```
Клиент -> api-gateway :8080 --> user-service    :8081
          (JWT, роли,       |--> event-service   :8082
           X-User-*)        |--> booking-service :8083 -> HTTP Interface + Circuit Breaker -> event-service

Все сервисы: конфиг <- config-server :8888 (git: ../events-config-repo),  регистрация -> discovery-server :8761
```

### Соответствие ТЗ

| Требование | Где сделано |
|---|---|
| Минимум 3 микросервиса | `user-service`, `event-service`, `booking-service` |
| Spring Cloud Config | `config-server` + отдельный git-репозиторий `events-config-repo` |
| Eureka | `discovery-server`, все сервисы и gateway регистрируются в нём |
| API Gateway | `api-gateway`: маршруты `lb://...`, проверка JWT, правила по ролям |
| * Circuit Breaker | `booking-service`: вызовы `event-service` идут через `EventLookup` (Resilience4j) |
| * Мониторинг и логирование | Actuator (`health`, `info`, `metrics`, на gateway ещё `gateway`), общий формат логов из config-repo, лог каждого запроса в gateway |
| * API-документация | Swagger в каждом сервисе и общий Swagger UI на gateway |

## user-service

Пользователи и роли. Выдаёт JWT (`POST /api/auth/login`) и регистрирует новых пользователей с ролью `USER` (`POST /api/auth/register`). В токене `sub` содержит id пользователя, а claim `role` содержит его роль. `/api/users` доступен только `ADMIN`.

При старте `DataInitializer` создаёт роли `ADMIN`, `MANAGER`, `USER` и пользователя `admin` / `admin123`.

## event-service

Каталог мероприятий: `Events`, `Categories`, `Locations`. Между ними обычные JPA-связи (`Event → Category`, `Event → Location`), потому что все три сущности лежат в одной БД.

- `organizerId` — простой `Long`, а не ссылка на пользователя. Он берётся из заголовка `X-User-Id`, который проставляет gateway, поэтому указать чужой id нельзя.
- Менять и удалять мероприятие может только организатор (`organizerId == X-User-Id`) или `ADMIN`. Это проверяет сам сервис.
- `GET /api/events/my` возвращает мероприятия текущего пользователя.
- Сервис хранит вместимость (`maxParticipants`), но не знает, сколько мест занято. Занятость считает `booking-service`, так у каждого факта один владелец.

## booking-service

Билеты, заказы и участие: `Tickets`, `Orders`, `OrderItems`, `Registrations`. Они меняются вместе в одной транзакции, поэтому живут в одном сервисе.

- `userId` и `eventId` — простые поля `Long`, без `@ManyToOne` на чужие сущности.
- `Tickets` — типы билетов с ценой и остатком (`quantity`).
- `OrderItems` хранит тип и **цену на момент покупки**. Если цену потом изменят, старые заказы останутся прежними.

**Заказ** (`POST /api/orders`) в `event-service` не обращается. Для каждой позиции он атомарно списывает билеты (`update ... where quantity >= :amount`), фиксирует цену и считает итог. Если билетов не хватает, вся транзакция откатывается и клиент получает `409`. При отмене билеты возвращаются в остаток.

**Регистрация** (`POST /api/registrations`) привязывает купленный билет к мероприятию:
1. Проверяет, что позиция заказа принадлежит пользователю, заказ не отменён и билеты в позиции ещё остались.
2. Спрашивает у `event-service`, существует ли мероприятие, не закончилось ли оно и какова его вместимость.
3. Считает подтверждённые регистрации на это мероприятие и сравнивает с вместимостью.
4. Сохраняет регистрацию с копией названия мероприятия.

**Вызов event-service** сделан через HTTP Interface (`@HttpExchange` + `RestClient`), а не через Feign. Балансировщик Eureka подключён только к этому клиенту (`ClientConfig`). Вызов обёрнут в **Circuit Breaker** (`EventLookup`, настройки в `ResilienceConfig`):
- таймаут 3 с;
- окно из 10 вызовов, порог ошибок 50 %;
- после срабатывания пауза 15 с.

Если `event-service` недоступен, клиент получает `503` с понятным сообщением вместо зависшего запроса. Ответ `404` от `event-service` превращается в `404 Event not found` и не засчитывается брейкеру как сбой.

**Доступ к данным:** пользователь видит только свои заказы и регистрации, `ADMIN` видит все. Список участников мероприятия (`GET /api/registrations/event/{eventId}`) доступен организатору и `ADMIN`.

## api-gateway

Единственная точка входа для клиента. Он реактивный (WebFlux), своей БД у него нет. Маршруты и настройки Swagger лежат в `events-config-repo/api-gateway.yml`.

**Маршрутизация** через Eureka:
- `/api/auth/**`, `/api/users/**` → `user-service`
- `/api/events/**`, `/api/categories/**`, `/api/locations/**` → `event-service`
- `/api/orders/**`, `/api/registrations/**`, `/api/tickets/**` → `booking-service`
- `/<service>/v3/api-docs` → `/v3/api-docs` нужного сервиса (для общего Swagger UI)

**Проверка JWT** (`AuthFilter`). Подпись проверяется тем же секретом, которым её ставит `user-service`: `jwt.secret` лежит в общем `application.yml` config-repo. Фильтр всегда **удаляет** присланные клиентом `X-User-Id` и `X-User-Role` и ставит свои. Иначе любой мог бы прислать `X-User-Role: ADMIN`.

**Правила доступа** (`AccessRules`, срабатывает первое подходящее):

| Путь | Доступ |
|---|---|
| `/api/auth/**`, документация сервисов | всем |
| `GET /api/events/my` | любой авторизованный |
| `GET` мероприятий, категорий, локаций | всем |
| изменение мероприятий, категорий, локаций | `MANAGER`, `ADMIN` |
| `/api/users/**` | `ADMIN` |
| `GET /api/tickets/**` | любой авторизованный |
| изменение билетов | `MANAGER`, `ADMIN` |
| заказы, регистрации, всё остальное | любой авторизованный |

Без токена на закрытом пути gateway вернёт `401`, с неверным или просроченным токеном тоже `401`, при нехватке роли `403`. `RequestLoggingFilter` пишет в лог одну строку на каждый запрос: метод, путь, маршрут, статус и время.

Сервисы по-прежнему доступны напрямую на своих портах, и там заголовки никто не проверяет. Защищён только вход через gateway.

## notification-service (необязательный)

**Идея:** после создания заказа или регистрации пользователю уходит уведомление. Это показало бы **асинхронное** взаимодействие: `booking-service` публикует событие `order.created` в брокер (RabbitMQ), а `notification-service` читает его и пишет в лог (или «отправляет письмо»). Заказ при этом не ждёт отправки.

**Цена:** нужно поднять брокер (проще всего в Docker) и добавить `spring-boot-starter-amqp`. Для зачёта он не нужен.

## Что осталось сделать

1. **Закоммитить `events-config-repo`.** Config Server читает только закоммиченное. Без `api-gateway.yml` и `jwt` в `application.yml` gateway не стартует, а `user-service` не сможет выпускать токены.
2. **Проверить Circuit Breaker вживую** (раздел 8 ниже).
3. **Пройти полный сценарий через gateway:**
   - менеджер создаёт категорию, локацию, мероприятие и тип билета;
   - пользователь покупает билет и регистрируется;
   - менеджер смотрит участников;
   - пользователь отменяет заказ, билеты возвращаются в остаток.
4. **Билеты не привязаны к мероприятию.** У `Tickets` нет `eventId`, поэтому один купленный билет можно использовать для регистрации на любое мероприятие. Если так задумано, это стоит явно описать. Если нет, добавить `eventId` в `Tickets` и проверять его при регистрации.
5. **Удаление пользователя с заказами.** Сейчас `DELETE /api/users/{id}` удаляет пользователя без проверок, и его заказы остаются с несуществующим `userId`. Варианты: запретить удаление при наличии заказов или перейти на «мягкое удаление» (`active=false`). Кроме того, для несуществующего id `findById` бросает `RuntimeException`, и клиент получает `500` вместо `404`.
6. **Тесты.** В `pom.xml` пяти модулей нет `spring-boot-starter-test`, поэтому их `contextLoads` не компилируются. Нужно либо добавить зависимость, либо удалить эти тесты: им всё равно нужны запущенный Config Server и PostgreSQL. Отдельно работает только юнит-тест `api-gateway/.../AccessRulesTest`.
7. **Убрать из git ранее закоммиченные `target/` и `.idea/`** (раздел 10).
8. По желанию: **notification-service**, Circuit Breaker с fallback-маршрутом на самом gateway.

---

## Запуск

### 1. Инструменты

```bash
java -version      # нужен JDK 25+ (именно JDK с javac, а не только JRE)
psql --version
git --version
```

Отдельный Maven не нужен: в каждом модуле есть wrapper `mvnw`. Можно запускать и из IDEA.

### 2. PostgreSQL

По одной базе на сервис:

```sql
CREATE DATABASE user_db;
CREATE DATABASE event_db;
CREATE DATABASE booking_db;
CREATE DATABASE notification_db;   -- только для notification-service
```

### 3. Пароль БД

В конфигах стоит `password: ${DB_PASSWORD:}`: пароль берётся из переменной окружения и по умолчанию пустой.

```bash
export DB_PASSWORD=postgres
```

В IntelliJ IDEA это делается в Run Configuration каждого сервиса: *Environment variables → `DB_PASSWORD=postgres`*.

### 4. Таблицы и начальные данные

Таблицы создавать вручную **не нужно**: в конфигах `ddl-auto: update`, Hibernate создаст их при первом запуске. `DataInitializer` в `user-service` создаёт роли и пользователя `admin` / `admin123`. Категории, локации, мероприятия и билеты изначально пустые, их нужно добавить через API.

### 5. Сборка

```bash
# из корня events-platform; JAVA_HOME должен указывать на JDK
api-gateway/mvnw -f pom.xml package -Dmaven.test.skip=true

# только юнит-тест правил доступа gateway
api-gateway/mvnw -f pom.xml -pl api-gateway test -Dtest=AccessRulesTest
```

`-Dmaven.test.skip=true` нужен, пока не решён пункт 6 из «Что осталось сделать».

### 6. Порядок запуска

Каждый сервис запускается в своём терминале (или своей Run Configuration в IDEA). Порядок важен:

```bash
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar        # 1. Config Server
java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar  # 2. Eureka

# 3. Бизнес-сервисы, в любом порядке
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar
java -jar event-service/target/event-service-0.0.1-SNAPSHOT.jar
java -jar booking-service/target/booking-service-0.0.1-SNAPSHOT.jar

java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar            # 4. Gateway
```

При старте сервисы забирают конфиг у Config Server (без `optional:`, поэтому без него они падают) и регистрируются в Eureka. Gateway узнаёт о сервисах через Eureka с задержкой до ~30 секунд. Всё это время запросы через 8080 могут возвращать `503`.

### 7. Проверка, что всё живо

| Что | Адрес |
|---|---|
| Конфиг для сервиса | http://localhost:8888/api-gateway/default |
| Дашборд Eureka (4 сервиса в списке) | http://localhost:8761 |
| Здоровье (Actuator) | http://localhost:8080/actuator/health, http://localhost:8081/actuator/health, … |
| Маршруты gateway | http://localhost:8080/actuator/gateway/routes |
| Общий Swagger (все сервисы) | http://localhost:8080/swagger-ui.html |
| Swagger отдельного сервиса | http://localhost:8081/swagger-ui.html, :8082, :8083 |

В общем Swagger сервис выбирается в выпадающем списке справа вверху. Токен из `/api/auth/login` вставляется через кнопку **Authorize**.

```bash
# получить токен
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/users    # 200
curl -i localhost:8080/api/users                                   # 401: нет токена
curl -i localhost:8080/api/events                                  # 200: публичный путь
```

### 8. Проверка Circuit Breaker

Брейкер срабатывает при регистрации на мероприятие: только она обращается к `event-service`.
1. Под обычным пользователем купите билет (`POST /api/orders`) и запомните `id` позиции заказа.
2. Остановите `event-service` (Ctrl+C).
3. Отправьте `POST /api/registrations` с этим `orderItemId` и любым `eventId`.

Ожидаемый результат: быстрый ответ `503` с сообщением «Event service is temporarily unavailable», а не зависание и не `500` с трассировкой. После нескольких неудачных вызовов брейкер открывается, и ответы приходят сразу, без попытки соединения. После повторного запуска `event-service` и паузы в 15 секунд регистрация снова работает.

### 9. Если меняете конфиги в `events-config-repo`

Config Server читает только закоммиченное:

```bash
cd ../events-config-repo
git add . && git commit -m "Описание изменения"
```

Сервисы подхватывают новые значения при следующем запуске, поэтому нужный сервис надо перезапустить.

### 10. `.gitignore`

`.gitignore` исключает `target/` и `.idea/`. Файлы, которые были закоммичены раньше, git продолжает отслеживать, поэтому их нужно один раз убрать из индекса (на диске они останутся):

```bash
git rm -r --cached */target .idea */.idea
git commit -m "Stop tracking build output and IDE files"
```

Репозиторий `events-config-repo` остаётся отдельным и лежит рядом с `events-platform`, а не внутри него.
