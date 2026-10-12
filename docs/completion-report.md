# Отчёт о доработке и проверке сервисов

Основа — smart-helpdesk-spec.md и существующие контракты Smart Helpdesk.

Это отчёт предыдущего этапа. Актуальные проверки и последующие исправления: [testing-report.md](testing-report.md).

## Результат

Notification-service: атомарная обработка событий, история, личный WebSocket, подтверждение получения и повторная доставка после отключения; несколько экземпляров через общую PostgreSQL. Email с подтверждением адреса, шифрованием очереди, повторами и отпиской. JWT истекает и в активной WebSocket-сессии, URL-токены запрещены, подключения и очереди ограничены.

Knowledge-base-service: CRUD, категории, full-text, чанкинг, embeddings Ollama и pgvector/HNSW, internal API поиска, совместимый с ai-worker. Сохранена атомарность изменения статьи и индекса. Добавлен production-профиль.

Gateway: исправлены несовпадения SQL-ограничений и существующих Java enum, хранение длинного ответа AI и выдача generatedAnswer. Обработка ticket.processed теперь атомарна с processed_event; ошибки направляются в DLQ. Ai-worker получил ограничение времени ожидания Ollama. Порты локальной инфраструктуры и сервисов по умолчанию доступны только на loopback.

## Проверено

- Maven test: 66 тестов, 0 ошибок, 0 пропусков. Выбранный набор указан в README; весь старый набор проекта не запускался.
- Maven package: все модули собраны, четыре исполняемых JAR.
- Настоящие PostgreSQL/pgvector в изолированных контейнерах: миграции, повторы событий, откат транзакций, индексация и поиск.
- Два экземпляра notification-service: получение уведомления другим экземпляром и повтор при переподключении до подтверждения.
- Локальный SMTP: подтверждение, одноразовый код, шифрование, доставка уведомления, сохранение очереди после ошибки. Письма во внешнюю почту не отправлялись; TLS реального провайдера не проверялся.
- Полный локальный сценарий: все четыре сервиса, Kafka, PostgreSQL и настоящая Ollama (qwen3:4b + nomic-embed-text), создание статьи и тикета, RAG, генерация/сохранение ответа, получение уведомления и подтверждение через HTTP. WebSocket отдельно проверен интеграционными тестами.

Результат сквозного запуска: статус ANSWED, длина ответа 141 символов, оценка RAG 0.995763. Временные БД/контейнеры удаляются скриптом.

## Настройки и оставшиеся границы

Email требует SMTP и отдельного ключа шифрования; production — HTTPS/WSS, собственных секретов и закрытой внутренней сети. Профиль prod требует явной конфигурации, но не разворачивает инфраструктуру. Подробности и HTTP/STOMP-контракт: [security-and-delivery.md](security-and-delivery.md) и [notification README](../notification-service/README.md).

Повтор сетевого сообщения возможен до подтверждения. Смена embedding-модели требует переиндексации, смена размерности — миграции. Существующие значения статусов ANSWED/WAITIND_OPERATOR сохранены. Outbox gateway публикует раз в 60 секунд.

Redis, OpenAPI UI, общий gateway-прокси, массовая рассылка операторам и полноценный circuit breaker остаются отдельными возможностями. Для текущей доставки Redis не требуется. Не заявляется отсутствие всех возможных уязвимостей или готовое публичное production-развёртывание.

## Файлы этой доработки

- `README.md`
- `ai-worker/src/main/java/com/smarthelpdesk/aiworker/config/ProductionSecrets.java`
- `ai-worker/src/main/java/com/smarthelpdesk/aiworker/config/RestClientConfig.java`
- `ai-worker/src/main/resources/application-prod.yaml`
- `ai-worker/src/main/resources/application.yaml`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/config/KafkaConfig.java`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/config/ProductionSecrets.java`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/kafka/TicketProcessedConsumer.java`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/mapper/TicketMapper.java`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/repository/ProcessedEventRepository.java`
- `api-gateway/src/main/java/com/smarthelpdesk/apigateway/service/ProcessedTicketService.java`
- `api-gateway/src/main/resources/application-prod.yaml`
- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/resources/db/migration/V8__align_ai_result_contract.sql`
- `api-gateway/src/test/java/com/smarthelpdesk/apigateway/servise/GatewayDatabaseIntegrationTest.java`
- `docker-compose.e2e.yml`
- `docker-compose.yml`
- `docs/completion-report.md`
- `docs/security-and-delivery.md`
- `knowledge-base-service/src/main/java/com/smarthelpdesk/knowledgebase/config/ProductionSecrets.java`
- `knowledge-base-service/src/main/resources/application-prod.yaml`
- `knowledge-base-service/src/main/resources/application.yaml`
- `notification-service/README.md`
- `notification-service/pom.xml`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/config/ProductionSecrets.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/config/StompAuthenticationInterceptor.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/config/WebSocketConfig.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/controller/NotificationController.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/mail/EmailCipher.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/mail/EmailController.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/mail/EmailService.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/repository/NotificationRepository.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/security/AccessTokenDetails.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/security/AccessTokenFilter.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/security/AccessTokenVerifier.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/security/SecurityConfig.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/security/WebSocketSessionGuard.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/service/NotificationDeliveryJob.java`
- `notification-service/src/main/java/com/smarthelpdesk/notificationservice/service/NotificationService.java`
- `notification-service/src/main/resources/application-prod.yaml`
- `notification-service/src/main/resources/application.yaml`
- `notification-service/src/main/resources/db/migration/V3__confirmed_delivery.sql`
- `notification-service/src/main/resources/db/migration/V4__email_outbox.sql`
- `notification-service/src/test/java/com/smarthelpdesk/notificationservice/EmailDatabaseIntegrationTest.java`
- `notification-service/src/test/java/com/smarthelpdesk/notificationservice/EmailSecurityUnitTest.java`
- `notification-service/src/test/java/com/smarthelpdesk/notificationservice/LocalSmtpServer.java`
- `notification-service/src/test/java/com/smarthelpdesk/notificationservice/NotificationDatabaseIntegrationTest.java`
- `notification-service/src/test/java/com/smarthelpdesk/notificationservice/WebSocketSecurityUnitTest.java`
- `scripts/e2e_local.py`
