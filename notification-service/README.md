# Notification Service

**Уведомления о результатах обработки обращений и изменении их статусов.**

Сервис связывает события системы с конкретным пользователем: получает событие из Kafka, узнаёт владельца тикета у API Gateway, сохраняет уведомление и доставляет его клиенту. История остаётся доступной через HTTP, даже если пользователь был офлайн.

| Параметр | Значение по умолчанию |
|---|---|
| HTTP | `http://localhost:8082` |
| WebSocket | `ws://localhost:8082/ws/notifications` |
| База данных | PostgreSQL · `notification` |
| Зависимости | PostgreSQL, Kafka, API Gateway; SMTP для Email |

## Что делает сервис

- Получает `ticket.processed` и `ticket.status-changed`.
- Запрашивает `userId` по `ticketId` через внутренний API Gateway.
- Сохраняет уведомление и идентификатор обработанного события одной транзакцией, предотвращая повторное создание уведомления.
- Возвращает историю пользователя, поддерживает отметки о доставке и прочтении.
- Отправляет уведомления по WebSocket и повторяет доставку до подтверждения или прочтения.
- При включённом Email-канале отправляет письма пользователям, подтвердившим подписку.

## HTTP API

Для пользовательских запросов нужен `Authorization: Bearer <accessToken>`. Пользователь определяется по JWT; история и действия над уведомлениями доступны только их владельцу.

| Метод | Адрес | Назначение |
|---|---|---|
| GET | `/api/v1/notifications?unreadOnly=true&page=0&size=20` | Страница непрочитанных уведомлений |
| PATCH | `/api/v1/notifications/{id}/read` | Отметить прочитанным, `204` |
| POST | `/api/v1/notifications/{id}/delivered` | Подтвердить доставку, `204` |
| GET | `/api/v1/notification-email` | Получить состояние Email-подписки |
| POST | `/api/v1/notification-email/subscribe` | Запросить письмо с кодом подтверждения, `202` |
| POST | `/api/v1/notification-email/confirm` | Подтвердить адрес, `204` |
| DELETE | `/api/v1/notification-email` | Отписаться, `204` |
| POST | `/api/v1/notification-email/retry-failed` | Повторить неудачные отправки, `202` |

Для полной истории убери `unreadOnly=true` или передай `false`. Подтверждение доставки можно повторять. Для подтверждения Email передаётся JSON `{"token":"код из письма"}`. Email-endpoints доступны при `EMAIL_ENABLED=true`.

## Подключение WebSocket

1. Открой соединение с `ws://localhost:8082/ws/notifications` без query-параметров.
2. В течение 10 секунд отправь STOMP `CONNECT` с нативным заголовком `Authorization: Bearer <accessToken>`.
3. Подпишись на `/user/queue/notifications` — личную очередь пользователя.
4. После получения уведомления подтверди доставку через HTTP `POST /api/v1/notifications/{id}/delivered`. Прочтение отмечается отдельным `PATCH`.

До подтверждения или прочтения сервер повторяет доставку примерно каждые 10 секунд, поэтому клиент объединяет сообщения по `id`. Подтверждение общее для пользователя, включая его другие вкладки. После истечения JWT соединение закрывается: клиент обновляет токен через Gateway и подключается заново. Клиентские STOMP `SEND` и подписки на чужие адреса запрещены.

Несколько экземпляров сервиса могут использовать общую БД и Kafka consumer group. Каждый экземпляр доставляет уведомления своим подключённым сессиям.

## Конфигурация и Email

| Переменная | Значение по умолчанию / назначение |
|---|---|
| `NOTIFICATION_SERVICE_PORT` | `8082` |
| `NOTIFICATION_DB_URL` | `jdbc:postgresql://localhost:5432/notification` |
| `NOTIFICATION_DB_USERNAME` / `NOTIFICATION_DB_PASSWORD` | Учётные данные БД |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `API_GATEWAY_URL` | `http://localhost:8080` |
| `INTERNAL_API_KEY` | Ключ внутренних запросов к Gateway |
| `JWT_SECRET` | Общий с Gateway ключ JWT |
| `NOTIFICATION_ALLOWED_ORIGINS` | `http://localhost:3000` — разрешённый origin клиента |
| `EMAIL_ENABLED` | `false` — Email включается отдельно |

Для Email нужны `EMAIL_FROM`, `EMAIL_ENCRYPTION_KEY`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` и `SMTP_PASSWORD`. Адрес берётся из JWT аккаунта и подтверждается одноразовым кодом. Адреса и содержимое очереди писем шифруются; тексты обращений и AI-ответов в письма не включаются. Подробности — в [руководстве по доставке](../docs/security-and-delivery.md).

## Запуск

После запуска инфраструктуры и Gateway выполни из корня репозитория:

```shell
mvn -pl notification-service -am package -DskipTests
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar
```

[Обзор и общий запуск](../README.md)
