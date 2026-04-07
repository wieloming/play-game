# Phishing Filter

Simple Scala 2.13 service for filtering phishing SMS messages.

## What it does

The service processes incoming SMS messages and returns a decision:

- `ACCEPTED` – message can be delivered
- `REJECTED` – message should be blocked

Supported features:

- subscription management via SMS commands (`START`, `STOP`)
- URL extraction and normalization
- phishing URL evaluation through external API
- local cache with TTL
- H2 database

## Tech stack

- Scala 2.13
- Cats Effect 3
- http4s
- Circe
- Doobie
- H2
- Docker

## Architecture decisions

- **Single synchronous service** - kept intentionally simple for fast delivery and low operational cost.
- **H2 database** - enough for the assignment, with a straightforward migration path to PostgreSQL in production.
- **Cached URL verdicts** - external phishing lookup is paid per request, so verdicts are cached with TTL to reduce cost.
- **Fail-open on lookup errors** - if the external service is unavailable, the SMS is accepted with a dedicated reason instead of blocking traffic because of a dependency outage.
- **Per-recipient protection** - regular SMS messages are filtered only if the recipient opted in using `START`.

## Assumptions

- `START` and `STOP` commands are recognized only when sent to the configured subscription number.
- Regular SMS messages are checked only for users with protection enabled.
- If an SMS contains multiple URLs, processing stops early as soon as a phishing URL is found.
- URL verdicts are cached for the configured TTL.
- The sample external API shape is enough; full Google Web Risk integration is not required for the assignment.

## Run locally

### Requirements

- JDK 17+
- sbt

### Start the app

```bash
sbt run
```

By default the app starts on:

```text
http://localhost:8080
```

## Run tests

```bash
sbt test
```

## Build fat jar

The Docker image uses a fat jar created with `sbt assembly`.

```bash
sbt assembly
```

Expected jar path:

```text
target/scala-2.13/phishing-filter.jar
```

## Run with Docker

### Build image

```bash
docker build -t phishing-filter .
```

### Run container

```bash
docker run --rm -p 8080:8080 phishing-filter
```

The app will be available at:

```text
http://localhost:8080
```

## Configuration

Example application configuration:

```hocon
app {
  http {
    host = "0.0.0.0"
    port = 8080
  }

  subscription-number = "7997"

  phishing {
    api-base-url = "https://example.com"
    api-token = "dummy-token"
    cache-ttl-hours = 24
  }

  db {
    url = "jdbc:h2:mem:phishing;DB_CLOSE_DELAY=-1"
    driver = "org.h2.Driver"
    user = "sa"
    password = ""
  }
}
```

You can override settings with JVM properties.

Example:

```bash
sbt -Dapp.http.port=9000 run
```

Docker example with overrides:

```bash
docker run --rm -p 8080:8080 phishing-filter   -Dapp.http.host=0.0.0.0   -Dapp.http.port=8080   -Dapp.subscription-number=7997   -Dapp.phishing.api-base-url=http://host.docker.internal:9999   -Dapp.phishing.api-token=dummy   -Dapp.phishing.cache-ttl-hours=24   '-Dapp.db.url=jdbc:h2:mem:phishing;DB_CLOSE_DELAY=-1'   -Dapp.db.driver=org.h2.Driver   -Dapp.db.user=sa   -Dapp.db.password=
```

## Example requests

### Enable protection

```bash
curl -X POST http://localhost:8080/sms   -H "Content-Type: application/json"   -d '{
    "sender": "48700100200",
    "recipient": "7997",
    "message": "START"
  }'
```

Example response:

```json
{
  "decision": "ACCEPTED",
  "reason": "SUBSCRIPTION_STARTED"
}
```

### Disable protection

```bash
curl -X POST http://localhost:8080/sms   -H "Content-Type: application/json"   -d '{
    "sender": "48700100200",
    "recipient": "7997",
    "message": "STOP"
  }'
```

### Check regular SMS

```bash
curl -X POST http://localhost:8080/sms   -H "Content-Type: application/json"   -d '{
    "sender": "234100200300",
    "recipient": "48700100200",
    "message": "Please verify your account at https://m-bonk.pl.ng/personal-data"
  }'
```

## Production follow-ups

- PostgreSQL instead of H2
- retries / circuit breaker around external phishing API
- metrics and structured logging
- richer URL normalization and allow/deny-list support
- container registry publication and deployment manifests