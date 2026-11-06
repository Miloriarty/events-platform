# Events Platform

Микросервисное приложение для управления мероприятиями на Spring Cloud: Config Server, Eureka, Spring Cloud Gateway, Circuit Breaker (Resilience4j), Actuator, Swagger для каждого сервиса.

Стек: Java 25, Spring Boot 4.0, Spring Cloud 2025.1, PostgreSQL, RabbitMQ.

## Общая картина
(остался get away)
| Сервис | Порт | БД | Владеет | Ходит в |
|---|---|---|---|---|
| config-server ✅ | 8888 | нет (git) | конфиги | нет |
| discovery-server ✅ | 8761 | нет | реестр сервисов | нет |
| user-service ✅ | 8081 | `user_db` | Users, Roles, выдача JWT | нет |
| event-service ✅ | 8082 | `event_db` | Events, Categories, Locations | нет |
| booking-service ✅ | 8083 | `booking_db` | Tickets, Orders, OrderItems, Registrations | event-service, публикует события в RabbitMQ |
| notification-service ✅ | 8084 | `notification_db` | история уведомлений | читает RabbitMQ, ходит в user-service |
| ticket-pass-service ✅ | 8085 | нет | QR-коды, PDF, подпись токенов билетов | booking-service, event-service |
| reminder-service ✅ | 8086 | нет | расписание напоминаний | event-service, booking-service, публикует события в RabbitMQ |
| api-gateway ✅ | 8080 | нет | маршруты, проверка JWT и ролей, общий Swagger | все (через Eureka) |

```
Клиент -> api-gateway :8080 --> user-service    :8081
          (JWT, роли,       |--> event-service   :8082
           X-User-*)        |--> booking-service :8083 -> HTTP Interface + Circuit Breaker -> event-service
                            |        |
                            |        | RabbitMQ (booking.events)
                            |        v
                            |--> notification-service :8084 -> HTTP Interface + Circuit Breaker -> user-service
                            |        |--> браузер: SSE через gateway
                            |        |--> почта: SMTP
                            |        ^
                            |        | RabbitMQ (reminder.events)
                            |        |
                            |--> reminder-service :8086 -> Circuit Breaker -> event-service, booking-service
                            |--> ticket-pass-service :8085 -> Circuit Breaker -> booking-service, event-service

Все сервисы: конфиг <- config-server :8888 (git: ../events-config-repo),  регистрация -> discovery-server :8761
```

### Соответствие ТЗ

| Требование | Где сделано |
|---|---|
| Минимум 3 микросервиса | `user-service`, `event-service`, `booking-service`, `notification-service`, `ticket-pass-service`, `reminder-service` |
| Spring Cloud Config | `config-server` + отдельный git-репозиторий `events-config-repo` |
| Eureka | `discovery-server`, все сервисы и gateway регистрируются в нём |
| API Gateway | `api-gateway`: маршруты `lb://...`, проверка JWT, правила по ролям |
| * Circuit Breaker | `booking-service`: вызовы `event-service` идут через `EventLookup`; `notification-service`: вызовы `user-service` идут через `UserLookup`; `ticket-pass-service` и `reminder-service`: вызовы `event-service` и `booking-service` идут через `EventLookup` и `BookingLookup` (Resilience4j) |
| * Мониторинг и логирование | Actuator (`health`, `info`, `metrics`, на gateway ещё `gateway`), общий формат логов из config-repo, лог каждого запроса в gateway; в бизнес-сервисах (SLF4J) — `INFO` на изменяющие операции (вход, регистрация, создание/изменение/удаление, заказы и их отмена), `WARN` на неудачный вход, отказ в доступе и ошибки `4xx`/валидации, `ERROR` на `5xx` в `ApiExceptionHandler`, `WARN` при сбое вызова event-service в `EventLookup` |
| * API-документация | Swagger в каждом сервисе и общий Swagger UI на gateway |

## user-service

Пользователи и роли. Выдаёт JWT (`POST /api/auth/login`) и регистрирует новых пользователей с ролью `USER` (`POST /api/auth/register`). В токене `sub` содержит id пользователя, а claim `role` содержит его роль. `/api/users` доступен только `ADMIN`.

При старте `DataInitializer` создаёт роли `ADMIN`, `MANAGER`, `USER` и пользователя `admin` / `admin123`.

## event-service

Каталог мероприятий: `Events`, `Categories`, `Locations`. Между ними обычные JPA-связи (`Event → Category`, `Event → Location`), потому что все три сущности лежат в одной БД.

- `organizerId` — простой `Long`, а не ссылка на пользователя. Он берётся из заголовка `X-User-Id`, который проставляет gateway, поэтому указать чужой id нельзя.
- Менять и удалять мероприятие может только организатор (`organizerId == X-User-Id`) или `ADMIN`. Это проверяет сам сервис.
- `GET /api/events/my` возвращает мероприятия текущего пользователя.
- `GET /api/events/starting?date=yyyy-MM-dd` возвращает мероприятия, начинающиеся в этот день (нужен `reminder-service`).
- Дата начала не может быть в прошлом: при создании и при её изменении сервис отвечает `400 Start date must not be in the past`. Уже прошедшее мероприятие можно редактировать, если не менять его дату начала.
- **Минимальная цена билета** (`minTicketPrice`) обязательна при создании и изменении мероприятия, `0` означает, что подходит любой билет. У мероприятий, созданных раньше, она равна `0`. Проверяет её `booking-service` при регистрации.
- Сервис хранит вместимость (`maxParticipants`), но не знает, сколько мест занято. Занятость считает `booking-service`, так у каждого факта один владелец.
- **Картинка мероприятия** (одна на мероприятие) хранится в БД, в отдельной таблице `event_images`. Поэтому списки мероприятий не загружают байты картинок. `PUT /api/events/{id}/image` (multipart, поле `file`) загружает или заменяет картинку: JPEG, PNG, WebP или GIF до 5 МБ, иначе `415`/`413`. Загружать может организатор или `ADMIN`. `DELETE /api/events/{id}/image` удаляет картинку. `GET /api/events/{id}/image` публичный. В ответе мероприятия поле `imageUrl` содержит `/api/events/{id}/image?v=<версия>` или `null`. Версия меняется при каждой загрузке, поэтому картинку можно кешировать на 30 дней. Лимит multipart задан в `event-service.yml` config-repo (`spring.servlet.multipart`). При удалении мероприятия удаляется и его картинка.

## booking-service

Билеты, заказы и участие: `Tickets`, `Orders`, `OrderItems`, `Registrations`. Они меняются вместе в одной транзакции, поэтому живут в одном сервисе.

- `userId` и `eventId` — простые поля `Long`, без `@ManyToOne` на чужие сущности.
- `Tickets` — типы билетов с ценой и остатком (`quantity`).
- `OrderItems` хранит тип и **цену на момент покупки**. Если цену потом изменят, старые заказы останутся прежними.

**Заказ** (`POST /api/orders`) в `event-service` не обращается. Для каждой позиции он атомарно списывает билеты (`update ... where quantity >= :amount`), фиксирует цену и считает итог. Если билетов не хватает, вся транзакция откатывается и клиент получает `409`. При отмене билеты возвращаются в остаток.

**Регистрация** (`POST /api/registrations`) привязывает купленный билет к мероприятию:
1. Проверяет, что позиция заказа принадлежит пользователю, заказ не отменён и билеты в позиции ещё остались.
2. Спрашивает у `event-service`, существует ли мероприятие, не закончилось ли оно и какова его вместимость.
3. Сравнивает цену билета с минимальной ценой мероприятия: берётся цена на момент покупки, более дешёвый билет даёт `409`.
4. Считает подтверждённые регистрации на это мероприятие и сравнивает с вместимостью.
5. Сохраняет регистрацию с копией названия мероприятия.

`GET /api/registrations/available-items?eventId=` возвращает билеты, которыми текущий пользователь может воспользоваться для этого мероприятия: свои позиции неотменённых заказов, где использованы не все билеты, а цена не ниже минимальной. Это верно и для `ADMIN`: в `GET /api/orders` он видит чужие заказы, но регистрироваться по ним не может.

**Проход на мероприятие** (`POST /api/registrations/{id}/check-in`) отмечает организатор мероприятия или `ADMIN`, обычно через `ticket-pass-service`. Отметка ставится атомарно (`update ... where checked_in_at is null`), поэтому из двух одновременных проверок одного билета пройдёт только одна, вторая получит `409`. Время прохода лежит в `Registrations.checkedInAt`. Одна регистрация — это один билет на вход.

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
- `/api/notifications/**` → `notification-service`
- `/api/passes/**` → `ticket-pass-service`
- `/api/reminders/**` → `reminder-service`
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
| `POST /api/registrations/*/check-in`, `POST /api/passes/check-in` | `MANAGER`, `ADMIN` (организатора проверяет `booking-service`) |
| `/api/reminders/**` | `ADMIN` |
| заказы, регистрации, уведомления, билеты с QR, всё остальное | любой авторизованный |

Без токена на закрытом пути gateway вернёт `401`, с неверным или просроченным токеном тоже `401`, при нехватке роли `403`. `RequestLoggingFilter` пишет в лог одну строку на каждый запрос: метод, путь, маршрут, статус и время.

Сервисы по-прежнему доступны напрямую на своих портах, и там заголовки никто не проверяет. Защищён только вход через gateway.

## notification-service

Сообщает пользователю о его заказах и регистрациях: в браузере и на почту. Заказ не ждёт доставки, потому что сервисы связаны **асинхронно** через RabbitMQ.

**Откуда берутся уведомления.** После коммита транзакции `booking-service` публикует событие в topic exchange `booking.events` (`order.created`, `order.cancelled`, `registration.created`, `registration.cancelled`). Адресат — владелец заказа или регистрации, даже если отменил их `ADMIN`. Откатившаяся операция ничего не публикует. Отмена заказа даёт одно уведомление, а не отдельное на каждую его регистрацию. Напоминания приходят из `reminder-service` через exchange `reminder.events` в ту же очередь.

Старые таблицы содержат `CHECK` со списком типов уведомлений, а `ddl-auto: update` его не обновляет. Поэтому `NotificationSchemaFix` удаляет это ограничение при запуске.

**Обработка** (`NotificationsServiceImpl.deliver`):
1. Проверяет, не обрабатывалось ли сообщение с таким `messageId`: брокер может доставить его повторно.
2. Сохраняет уведомление в `notification_db`. Текст формирует этот сервис (`NotificationTemplates`).
3. Отправляет его в открытые вкладки пользователя через SSE.
4. Запрашивает адрес почты у `user-service` и отправляет письмо. Адрес здесь не хранится: у него один владелец. Вызов обёрнут в Circuit Breaker (`UserLookup`).

**API** (только свои уведомления, чужой id даёт `404`, `ADMIN` здесь не отличается от других):

| Запрос | Что делает |
|---|---|
| `GET /api/notifications` | история, сначала новые |
| `GET /api/notifications/unread-count` | количество непрочитанных |
| `GET /api/notifications/stream` | поток новых уведомлений (SSE, событие `notification`, раз в 25 с комментарий `ping`) |
| `POST /api/notifications/{id}/read` | отметить прочитанным |
| `POST /api/notifications/read-all` | отметить все прочитанными |

Соединение SSE закрывается через 30 минут, и клиент подключается заново: так gateway снова проверяет токен.

**Что происходит при сбоях:**

| Сбой | Результат |
|---|---|
| Брокер недоступен при заказе | заказ создаётся, событие **теряется**, в логе `booking-service` `WARN` (outbox не сделан) |
| `notification-service` остановлен | события ждут в очереди и обрабатываются после запуска |
| Почта или `user-service` недоступны | уведомление остаётся в истории и в браузере, в БД `email_status = FAILED`, письмо **повторно не отправляется** |
| Сообщение не удаётся обработать | 3 попытки, затем очередь `notification.booking-events.dlq` |

Очередь объявляет `notification-service`, поэтому до его **самого первого** запуска события некуда складывать и они пропадают.

Открытые соединения SSE хранятся в памяти процесса, поэтому сервис рассчитан на один экземпляр. При нескольких экземплярах событие получат только вкладки, подключённые к обработавшему его.

### Настройка почты

По умолчанию письма уходят в **Mailpit**: тестовый SMTP-сервер, который ничего не доставляет, а показывает письма на http://localhost:8025.

Для настоящей почты `notification-service` при запуске сам читает необязательный файл `~/.config/events-platform/notification-service.env`, поэтому вводить `export` не нужно. Файл лежит вне репозиториев, потому что в нём пароль. Значения пишутся без кавычек. Пример для Gmail (нужен [пароль приложения](https://myaccount.google.com/apppasswords), обычный пароль не подойдёт):

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=пароль приложения
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_FROM=you@gmail.com
```

После правки файла перезапустите сервис. Если файла нет, письма снова идут в Mailpit. Те же имена можно задать и как обычные переменные окружения. Адрес клиента в ссылках из писем задаёт `FRONTEND_URL` (по умолчанию `http://localhost:4200`).

## ticket-pass-service

Электронные билеты: QR-код и PDF для участника, проверка на входе для организатора. БД нет: подлинность билета доказывает подпись, а отметка о проходе хранится в `booking-service`, потому что он владеет регистрациями.

| Запрос | Доступ | Что делает |
|---|---|---|
| `GET /api/passes/{registrationId}` | владелец регистрации, `ADMIN` | данные билета, токен, ссылка на проверку и QR-код (data URI с PNG) |
| `GET /api/passes/{registrationId}/pdf` | владелец регистрации, `ADMIN` | билет в PDF (A5) |
| `POST /api/passes/check-in` `{token}` | организатор мероприятия, `ADMIN` | проверяет токен и отмечает проход |

**Токен** — JWT со своим секретом `ticket-pass.secret` (в `ticket-pass-service.yml` config-repo). Секрет отделён от `jwt.secret`: токен лежит в QR-коде и не должен подходить для входа в систему. Срок действия заканчивается вместе с последним днём мероприятия. QR-код содержит ссылку `<FRONTEND_URL>/check-in?token=...`, поэтому его можно считать камерой телефона: страница откроется у менеджера и сразу проверит билет. Для проверки с телефона `FRONTEND_URL` должен быть доступен по сети, а не быть `localhost`.

**Права** проверяет `booking-service`: `X-User-Id` и `X-User-Role` передаются ему дальше. Поэтому чужая регистрация даёт `404`, а менеджер чужого мероприятия при проверке получает `403`.

| Ситуация | Ответ |
|---|---|
| Токен неверный, подделанный или истёк | `400` |
| Билет уже использован, регистрация отменена | `409` |
| Запрос билета по отменённой регистрации или завершившемуся мероприятию | `409` |
| Недоступен `booking-service` или `event-service` | `503` (Circuit Breaker, настройки как в `booking-service`) |

PDF встраивает шрифт Liberation Sans из `src/main/resources/fonts` (лицензия OFL там же): стандартные шрифты PDF не содержат кириллицу. Если перенести мероприятие на более позднюю дату, ранее выданный токен истечёт раньше срока: участнику нужно открыть билет заново.

## reminder-service

Напоминает участникам о скором мероприятии. БД нет.

По расписанию `reminder.cron` (каждый час с 9:00 до 21:00, задано в `reminder-service.yml`) сервис:
1. Запрашивает у `event-service` мероприятия, которые начнутся через `reminder.days-before` дней (по умолчанию 1).
2. Запрашивает у `booking-service` подтверждённых участников. Пользователь с несколькими билетами учитывается один раз.
3. Публикует по сообщению на участника в exchange `reminder.events`. Текст и доставку делает `notification-service`, поэтому напоминание приходит и в браузер, и на почту.

**Почему нет дублей без своей БД.** `messageId` строится из мероприятия, пользователя и даты начала (`reminder:<eventId>:<userId>:<startDate>`), а `notification-service` не обрабатывает `messageId` повторно. Поэтому проверки можно запускать сколько угодно раз. Это же даёт надёжность: если сервис, зависимость или брокер были недоступны, напоминание уйдёт при следующей проверке, а участник, зарегистрировавшийся после первой проверки, получит своё при следующей. После переноса мероприятия напоминание придёт снова, потому что изменилась дата.

`POST /api/reminders/run` (только `ADMIN`) запускает проверку сразу и возвращает итог: дату, число мероприятий, отправленных сообщений и сбоев. `published` считает отправленные сообщения, включая повторы, которые потом отбросит `notification-service`. Если `event-service` недоступен, ответ `503`, а сбой `booking-service` по одному мероприятию не мешает остальным и попадает в `failed`.

## Что осталось сделать

1. **Закоммитить `events-config-repo`.** Config Server читает только закоммиченное. Без `api-gateway.yml` и `jwt` в `application.yml` gateway не стартует, а `user-service` не сможет выпускать токены.
2. **Проверить Circuit Breaker вживую** (раздел 8 ниже).
3. **Пройти полный сценарий через gateway:**
   - менеджер создаёт категорию, локацию, мероприятие и тип билета;
   - пользователь покупает билет и регистрируется;
   - менеджер смотрит участников;
   - пользователь отменяет заказ, билеты возвращаются в остаток.
4. **Билеты не привязаны к мероприятию.** У `Tickets` нет `eventId`, поэтому купленный билет подходит для любого мероприятия, чья минимальная цена не выше цены билета. Если нужна строгая привязка, добавить `eventId` в `Tickets` и проверять его при регистрации.
5. **Удаление пользователя с заказами.** Сейчас `DELETE /api/users/{id}` удаляет пользователя без проверок, и его заказы остаются с несуществующим `userId`. Варианты: запретить удаление при наличии заказов или перейти на «мягкое удаление» (`active=false`).
6. **Тесты.** В `pom.xml` пяти модулей нет `spring-boot-starter-test`, поэтому их `contextLoads` не компилируются. Нужно либо добавить зависимость, либо удалить эти тесты: им всё равно нужны запущенный Config Server и PostgreSQL. Отдельно работают только юнит-тесты `api-gateway/.../AccessRulesTest` и тесты `notification-service`, `ticket-pass-service`, `reminder-service`.
7. **Убрать из git ранее закоммиченные `target/` и `.idea/`** (раздел 10).
8. **Надёжность уведомлений** (см. таблицу сбоев в разделе notification-service). Можно добавить outbox в `booking-service` и повторную отправку писем со статусом `FAILED`.
9. **Уведомления об изменении мероприятия.** Сейчас есть только события о заказах и регистрациях. Перенос или отмена мероприятия участникам не сообщаются: для этого `event-service` должен публиковать события, а участников знает `booking-service`.
10. По желанию: Circuit Breaker с fallback-маршрутом на самом gateway.
11. **Отмена после прохода.** Регистрацию и заказ сейчас можно отменить и после отметки о проходе, билеты вернутся в продажу. Если это не нужно, запретить отмену при заполненном `checkedInAt`.

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
CREATE DATABASE notification_db;
```

### RabbitMQ и почта

`booking-service` и `notification-service` нужен RabbitMQ, а `notification-service` ещё и SMTP-сервер. Для локального запуска оба описаны в `docker-compose.yml`:

```bash
docker compose up -d
```

На Podman без compose-провайдера то же самое:

```bash
podman run -d --name rabbitmq -p 5672:5672 -p 15672:15672 docker.io/library/rabbitmq:4-management-alpine
podman run -d --name mailpit -p 1025:1025 -p 8025:8025 docker.io/axllent/mailpit:latest
```

Консоль RabbitMQ: http://localhost:15672 (`guest` / `guest`). Письма: http://localhost:8025. Адрес брокера можно переопределить переменными `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`.

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

# юнит-тесты сервисов
api-gateway/mvnw -f pom.xml -pl notification-service,ticket-pass-service,reminder-service test
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
java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar
java -jar ticket-pass-service/target/ticket-pass-service-0.0.1-SNAPSHOT.jar
java -jar reminder-service/target/reminder-service-0.0.1-SNAPSHOT.jar

java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar            # 4. Gateway
```

При старте сервисы забирают конфиг у Config Server (без `optional:`, поэтому без него они падают) и регистрируются в Eureka. Gateway узнаёт о сервисах через Eureka с задержкой до ~30 секунд. Всё это время запросы через 8080 могут возвращать `503`.

### 7. Проверка, что всё живо

| Что | Адрес |
|---|---|
| Конфиг для сервиса | http://localhost:8888/api-gateway/default |
| Дашборд Eureka (7 сервисов в списке) | http://localhost:8761 |
| Здоровье (Actuator) | http://localhost:8080/actuator/health, http://localhost:8081/actuator/health, … |
| Информация о сервисе (имя, Java, ОС) | http://localhost:8080/actuator/info, http://localhost:8081/actuator/info, … |
| Маршруты gateway | http://localhost:8080/actuator/gateway/routes |
| Общий Swagger (все сервисы) | http://localhost:8080/swagger-ui.html |
| Swagger отдельного сервиса | http://localhost:8081/swagger-ui.html, :8082, :8083, :8084, :8085, :8086 |
| Письма (Mailpit) | http://localhost:8025 |
| Очереди RabbitMQ | http://localhost:15672 |

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
