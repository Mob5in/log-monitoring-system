# Log Monitoring System

A distributed log monitoring tool built with **Spring Boot**, **Apache Kafka**, and **PostgreSQL**. It watches log files produced by different systems, streams parsed log entries through Kafka, evaluates them against a set of configurable rules, and exposes generated alerts through a REST API.

## Architecture

```
                 ┌──────────────┐
                 │    Client    │
                 └──────┬───────┘
                        │ GET /alerts
                        ▼
                 ┌──────────────┐
                 │   Backend    │
                 │     API      │
                 └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │  PostgreSQL  │◄────────────┐
                 └──────────────┘             │
                        ▲                     │
                        │ save alerts         │
                 ┌──────┴───────┐             │
                 │     Rule     │             │
                 │  Evaluator   │             │
                 └──────┬───────┘             │
                        │ consume             │
                        ▼                     │
                 ┌──────────────┐             │
                 │    Kafka     │             │
                 └──────▲───────┘             │
                        │ produce             │
                 ┌──────┴───────┐             │
                 │     File     │             │
                 │   Ingester   │─────────────┘
                 └──────▲───────┘   (extracted component name)
                        │ watches
                 ┌──────┴───────┐
                 │  Logs Folder │
                 └──────────────┘
```

The system is made up of three independent subsystems, connected through Kafka:

1. **File Ingester** — watches a configured folder for log files, parses each line, and publishes parsed entries to a Kafka topic. Deletes a file only after its contents are confirmed sent to Kafka.
2. **Rule Evaluator** — consumes log entries from Kafka, evaluates them against rules loaded from a YAML config file, and persists any generated alerts to PostgreSQL.
3. **Backend API** — exposes a `GET /alerts` endpoint returning all alerts as JSON, ordered by creation time.

## Tech Stack

- **Java 21**, **Spring Boot 4.1.0**, Maven
- **Apache Kafka** (KRaft mode, no Zookeeper) via `spring-kafka`
- **PostgreSQL 16** via Spring Data JPA
- **Docker Compose** for local infrastructure (Kafka, PostgreSQL, Kafka UI)
- **JUnit 5**, **AssertJ**, **Mockito** for testing

## Project Structure

```
ir.aut.logmonitor
├── common.model         → shared models (LogEntry)
├── ingester              → File Ingester subsystem
├── evaluator              → Rule Evaluator subsystem
│   └── evaluator.rules     → rule config model, loader, and rule implementations
├── alert                    → Alert entity + repository
├── api                       → Backend API (controllers)
│   └── api.dto                → API response DTOs
└── config                      → shared configuration (e.g. Kafka topic setup)
```

## Getting Started

### Prerequisites

- JDK 21+
- Docker & Docker Compose

### 1. Start the infrastructure

```bash
docker compose up -d
```

This starts:
- **Kafka** on `localhost:9092`
- **PostgreSQL** on `localhost:5432` (db: `logmonitor`, user/password: `logmonitor`)
- **Kafka UI** at [http://localhost:8081](http://localhost:8081) for inspecting topics/messages

### 2. Build

```bash
./mvnw clean package -DskipTests
```

### 3. Run

For local development, running with no profile brings up all three subsystems together in a single process:

```bash
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar
```

By default, the app watches `./logs` for log files and expects a rules config at `./rules.yml` (both configurable — see below).

### 4. Try it out

Drop a log file into the watched folder, e.g. `logs/auth-service_2025-07-19.log`:

```
2021-07-12 01:22:42,114 [main] ERROR ir.aut.Service – Database connection failed
```

Within a few seconds the file is parsed, published to Kafka, evaluated, and (if a rule matches) an alert is saved. Check it via:

```
GET http://localhost:8080/alerts
```

## Configuration

Key properties in `application.properties`:

| Property | Description |
|---|---|
| `app.logs.folder` | Folder watched by the File Ingester |
| `app.logs.line-pattern` | Regex used to parse a log line (default matches the format below) |
| `app.kafka.topic.logs` | Kafka topic name used for log entries |
| `app.kafka.topic.logs.partitions` | Number of partitions for that topic (affects Rule Evaluator parallelism) |
| `app.rules.config-path` | Path to the rules YAML config file |

Default expected log line format (configurable via `app.logs.line-pattern`):
```
2021-07-12 01:22:42,114 [ThreadName] INFO package.name.ClassName – msg
```

### Rules config (`rules.yml`)

```yaml
rules:
  - name: error-log-rule
    type: LOG_TYPE
    logLevel: ERROR

  - name: warning-rate-rule
    type: TYPE_RATE
    logLevel: WARNING
    windowSeconds: 300
    threshold: 10

  - name: overall-rate-rule
    type: OVERALL_RATE
    windowSeconds: 300
    threshold: 50
```

Supported rule types:
- **`LOG_TYPE`** — alerts on every log line matching `logLevel`.
- **`TYPE_RATE`** — alerts if a component produces more than `threshold` logs of `logLevel` within `windowSeconds`.
- **`OVERALL_RATE`** — alerts if a component's overall log rate (any level) exceeds `threshold` within `windowSeconds`.

## API

### `GET /alerts`

Returns all alerts, most recent first.

```json
[
  {
    "id": 12,
    "ruleName": "error-log-rule",
    "componentName": "auth-service",
    "description": "Database connection failed",
    "createdAt": "2025-07-19T10:42:03.114"
  }
]
```

## Running Multiple Instances (Horizontal Scaling)

Each subsystem can run as an independent, minimally-resourced instance using Spring profiles:

```bash
# Two File Ingester instances, each watching a different system's log folder
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingester --app.logs.folder=./logs/system-a
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingester --app.logs.folder=./logs/system-b

# Two Rule Evaluator instances — Kafka automatically balances work between them
# (requires the logs topic to have more than 1 partition)
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar --spring.profiles.active=evaluator
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar --spring.profiles.active=evaluator

# One Backend API instance
java -jar target/log-monitor-0.0.1-SNAPSHOT.jar --spring.profiles.active=api
```

Profile-scoped instances skip unnecessary resources — e.g. an `ingester`-only instance never opens a database connection or starts an embedded web server.

> **Note:** unlike the Rule Evaluator (which scales automatically via Kafka consumer groups), the File Ingester has no built-in coordination between instances. Each instance should watch its own folder to avoid multiple instances racing to process the same files.

## Testing

```bash
./mvnw test
```

Testing approach:
- Pure logic (parsing, rule evaluation, config loading) — plain JUnit 5 + AssertJ unit tests, no Spring context.
- Orchestrator classes (`RuleEngine`, `LogConsumerService`) — Mockito-based unit tests.
- Web layer (`AlertController`) — `@WebMvcTest` + `MockMvc` + `@MockitoBean`.

## Known Limitations

- At-least-once delivery: if the app crashes between a successful Kafka send and deleting the source file, some log lines may be reprocessed (not lost, but possibly duplicated).
- Rate-based rules keep their sliding window state in memory only — it resets on restart.
- No Dead Letter Queue yet for repeatedly-failing messages.
- `FileWatcherService` isn't covered by automated tests yet (would benefit from a Testcontainers-based integration test).
- `ddl-auto=update` is used for schema management, which is fine for development but not recommended as-is for production use.
