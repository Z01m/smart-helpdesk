# Notification Service

Порт 8082. Kafka-события ticket.processed и ticket.status-changed преобразуются в уведомления владельца тикета. GatewayClient принимает ticketId, получает userId из защищённого internal API. NotificationService в одной транзакции сохраняет уведомление и processed_event; повтор события не создаёт дубликат. После трёх повторов ошибочное событие сохраняется в ticket.dlq; неудачная отправка в DLQ не подтверждает событие.

## HTTP API

Все запросы, кроме health и начального WebSocket handshake, требуют access JWT в Authorization: Bearer.

| Метод | Путь | Результат |
|---|---|---|
| GET | /api/v1/notifications?unreadOnly=true&page=0&size=20 | Страница с content |
| PATCH | /api/v1/notifications/{id}/read | Прочитано, 204 |
| POST | /api/v1/notifications/{id}/delivered | Получено, 204; повтор безопасен |
| GET | /api/v1/notification-email | enabled, verified, failedMessages |
| POST | /api/v1/notification-email/subscribe | Запрос подтверждения адреса из JWT, 202 |
| POST | /api/v1/notification-email/confirm | JSON {"token":"код из письма"}, 204 |
| DELETE | /api/v1/notification-email | Отписка, 204 |
| POST | /api/v1/notification-email/retry-failed | Повтор неудачных уведомлений, 202 |

Пользователь определяется только по JWT. Чужое уведомление даёт 404. Email API существует только при EMAIL_ENABLED=true.

## WebSocket

Подключение к ws://localhost:8082/ws/notifications (в production wss://). URL не должен содержать query-параметров. За 10 секунд отправь STOMP CONNECT с нативным заголовком Authorization: Bearer <accessToken>. Затем подпишись на /user/queue/notifications. SEND и подписки на другие адреса запрещены.

После отображения уведомления отправь HTTP POST /api/v1/notifications/{id}/delivered. Это прикладное подтверждение; STOMP ACK не используется. Прочтение — отдельный PATCH. Клиент объединяет сообщения по id: до подтверждения сервер повторяет доставку примерно каждые 10 секунд. После истечения JWT сокет закрывается, клиент обновляет токен через gateway и переподключается.

NotificationDeliveryJob принимает локальный список сессий, читает из общей БД ещё не подтверждённые/непрочитанные уведомления их пользователей и отправляет DTO в конкретную сессию. Несколько экземпляров используют одну notification DB и одну Kafka consumer group. Сессии между узлами не копируются. Подтверждение считается общим для пользователя, не отдельным для каждой вкладки. Старые сообщения всегда доступны через HTTP.

## Email и эксплуатация

См. [настройку и гарантии](../docs/security-and-delivery.md). EmailService принимает пользователя и адрес его аккаунта, выдаёт одноразовое подтверждение по почте, затем обрабатывает независимую очередь писем. Адрес и полезная нагрузка зашифрованы AES-GCM; ключ хранится вне БД. В письмах нет текста тикета/ответа AI. Для доставки нужен свой SMTP.

Аннотация @Transactional объединяет изменения БД: ошибка откатывает все изменения. @Scheduled периодически запускает доставку. @ConditionalOnProperty создаёт EmailService только при включённом канале. @Profile("prod") включает обязательную проверку production-секретов.

