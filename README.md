# Smart Helpdesk

Java 21, Spring Boot 4.1, Maven multi-module, PostgreSQL/pgvector, Kafka и Ollama.

| Модуль | Порт | Задача |
|---|---|---|
| api-gateway | 8080 | Пользователи, JWT, тикеты, outbox и internal API владельца |
| ai-worker | 8081 | Обработка обращения, поиск RAG и генерация ответа |
| notification-service | 8082 | История уведомлений и WebSocket |
| knowledge-base-service | 8083 | Статьи, категории, полнотекстовый и векторный поиск |
| common | — | Kafka-контракты |

Описание новых сервисов: [уведомления](notification-service/README.md), [база знаний](knowledge-base-service/README.md).

## Локальный запуск

1. Запусти Docker Desktop. В `.env` в корне проекта должен быть `POSTGRES_PASSWORD`, соответствующий паролям БД в приложениях (локальный default — `helpdesk`). Существующий `.env` не изменён.
2. Подними инфраструктуру: `docker compose up -d`. Создаются Kafka-топики, а на **новом** PostgreSQL-томе — БД `gateway`, `ai_worker`, `notification`, `knowledge_base`. Не удаляй существующий том ради создания недостающей БД.
3. Запусти Ollama. Загрузи `nomic-embed-text` и модель генерации, указанную в ai-worker (`qwen3:4b` по умолчанию).
4. Во всех соответствующих процессах задай одинаковые `JWT_SECRET` и `INTERNAL_API_KEY`. В KB и ai-worker используй одинаковые `AI_BASE_URL` и `AI_EMBEDDING_MODEL`. Значения по умолчанию подходят только для локальной разработки.
5. Собери проект: `mvn clean package -DskipTests`. Запусти каждый сервис отдельным процессом из IDE или через `java -jar <module>/target/<module>-1.0.0-SNAPSHOT.jar`.
6. Создай статьи через API KB с токеном ADMIN. Затем обычный пользователь может создать тикет через gateway. ai-worker найдёт контекст, а notification-service сохранит уведомление и отправит его подключённому клиенту.

`INTERNAL_API_KEY` передают только сервисы, не браузер. Новый internal endpoint gateway — `GET /internal/v1/tickets/{ticketId}/owner`. Он возвращает `ticketId` и `userId` и защищён отдельной цепочкой Spring Security.

## Проверки

Компиляция всех модулей: `mvn clean compile`.

Полный стандартный набор тестов и отчёты покрытия:

```powershell
mvn clean verify
```

Нужны Java 21, Maven и работающий Docker. PostgreSQL/pgvector создаются в изолированных контейнерах; Kafka-тест поднимает настоящий встроенный broker. Рабочие БД не используются. Если Docker недоступен, сборка падает, а не показывает ложный успех с пропущенными интеграционными тестами. Настоящие SMTP/Ollama для стандартного набора не нужны: SMTP работает на loopback без пересылки, внешние HTTP-ответы контролируются тестами.

Дополнительная проверка установленной Ollama (нужна модель qwen3:4b либо AI_MODEL):

```powershell
mvn clean verify -Plive-ai
```

Профиль live-ai включает существующий ClassificationServiceOllamaIT через Failsafe; его БД также временная. Обычные тесты при этом тоже запускаются. Отчёты — `<module>/target/surefire-reports`, `ai-worker/target/failsafe-reports`, покрытие — `<module>/target/site/jacoco/index.html`. Проверка verify требует минимум 80% покрытия строк каждого сервисного модуля; это не доказательство отсутствия ошибок. Ветки и непокрытый код видны в HTML-отчётах.

Сценарии, найденные ошибки и измерения: [отчёт о тестировании](docs/testing-report.md).

## Принятые упрощения

За основу взято `smart-helpdesk-spec.md` версии 1.0 и существующие контракты проекта. Сохранены Spring Boot 4.1 и пакеты работающих модулей; переход на Spring Boot 3 из примера ТЗ не выполнялся.

Email с подтверждением адреса и несколько экземпляров notification-service поддерживаются. Доставка WebSocket повторяется до подтверждения клиента. Настройка, контракт клиента и границы гарантий описаны в [security-and-delivery.md](docs/security-and-delivery.md).

Redis не нужен для выбранной доставки через общую БД. OpenAPI UI, общий HTTP-прокси, массовая рассылка операторам и автоматическое развёртывание не добавлены. Таймауты внешних запросов ограничены; полноценного circuit breaker пока нет. Это отдельные возможности, а не снятые проверки безопасности.

Сохранены существующие Java/API-значения статусов ANSWED и WAITIND_OPERATOR и приоритет NORMAL. Миграция V8 согласует с ними PostgreSQL и позволяет хранить длинные ответы AI. Исправлен возврат generatedAnswer через API. Изменения операторского статуса публикуются outbox раз в 60 секунд.

## Полный локальный сценарий

После `mvn package -DskipTests`, при запущенных Docker Desktop и Ollama с моделями qwen3:4b и nomic-embed-text:

```powershell
python scripts/e2e_local.py
```

Скрипт создаёт отдельные временные PostgreSQL/Kafka и случайные тестовые секреты, запускает четыре JAR, индексирует статью, проверяет RAG, создаёт пользователя/тикет, ожидает ответ AI и сохранённое уведомление. Затем удаляет тестовую статью и проверяет передачу нового обращения оператору при пустой базе знаний. В конце удаляет только свои контейнеры/тома; отчёт находится в target/e2e. Письма наружу не отправляются. Тест SMTP и WebSocket выполняется отдельно в Maven.
