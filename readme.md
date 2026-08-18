
## What this application is:
A Spring Boot-based payment ingestion and ticketing pipeline that accepts payment events over HTTP, persists them, publishes them to Kafka, and drives downstream ticket/receipt delivery (via email). 

 - – A production payment reconciliation platform to process 3000+
   real-time financial transactions during a large-scale college fest,
   Invente 2025.
 - – Implemented with a Kafka stack including Transactional Outbox
   producers, downstream consumers, and Kafka Streams pipelines for
   real-time transaction processing, reconciliation and event pass
   generation.
 - – Used Kafka Streams topology using Windowed Joins backed by SQL
   state tracking to correlate high-velocity payment events with
   settlement receipts across producers whilst handling out of window
   events.
 - – Built monitoring and observation stacks using Grafana and
   Prometheus, along with NGINX and Docker containerized deployment on a
   VPS.
 - – Validated system ceiling through K6 load testing, achieving a
   stable throughput of 50,000+ Transactions per Minute.

### Stack
- Language(s): Java (primary), Dockerfile (deployment artifacts)
- Framework / runtime: Spring Boot 3.5.x, Java 17
- Notable libraries/dependencies:
  - Spring Boot starters: web, kafka, data-mongodb, data-redis, actuator
  - Apache Kafka (spring-kafka + kafka-streams)
  - MongoDB (spring-data-mongodb) for primary persistence
  - Redis (spring-data-redis / Jedis) for caching/OTP/session artifacts
  - Google Gmail API client for email/ticket delivery
  - JJWT (io.jsonwebtoken) for token/OTP related features
  - MyBatis starter (mybatis-spring-boot-starter) — present for SQL integrations

## How it's organized
Top-level (annotated):
```
.gitignore
pom.xml                          Maven build + dependency list
mvnw, mvnw.cmd                    Maven wrapper
.mvn/                             maven wrapper files
Kafka_Dev/                        local Kafka/dev infra helpers
docker_fingers_crossed/           docker/dev packaging/config examples
redis-setup_dev/                  local Redis/dev helpers
k6/                               load test scripts
docx/                             design / documentation artifacts (binary/docx)
src/
  main/
    java/
      com/saipbuilds/kpaymentpipeline/
        KpaymentpipelineApplication.java    Spring Boot entrypoint
        Controller/        HTTP controllers (PaymentController, OTPController, ReceiptController, HealthController, HackPaymentController)
        Service/           business logic and orchestration
        Database/          DB wrappers (SQL via MyBatis, MongoDB for raw event storage)
        Kafka/             Kafka producers/consumers/streams (EventProducer and stream processors)
        Redis/             Redis helpers (OTP, cache)
        Email/             ticket/receipt email sender (Gmail integration)
        TicketSender/      event consumer that converts payment events into tickets/receipts
        DTO/               event and HTTP payload data transfer objects (e.g., PaymentReceivedEvent)
        ExceptionHandler/  centralized error handling for REST API
        Auth/              authentication / token/OTP endpoints
    resources/            Spring properties and resource files
```

How it fits together (runtime shape):
- The application is a Spring Boot HTTP server exposing REST endpoints (e.g., /api/payment). Controllers validate requests and delegate to services.
- On payment ingestion, the system writes to persistent storage (raw events to MongoDB) and to relational stores, enqueues or publishes a domain event to Kafka via an EventProducer, and persists additional state via PaymentDatabaseServices / PaymentDB.
- Downstream components (TicketSender package and Kafka consumers) subscribe to Kafka topics to generate receipts/tickets and send them via the Gmail API; Redis is used for short-lived state such as OTPs or caching.

## Detailed functional overview

1) HTTP ingestion (PaymentController)
- Endpoint: POST /api/payment
- Payload: PaymentReceivedEvent (DTO)
- Behavior (as implemented):
  - Validate required fields (emailID, paymentID)
  - Persist event details via PaymentDatabaseServices and PaymentDB (Mongo)
  - Produce a domain event into Kafka using EventProducer
  - Return 201 on success or appropriate error codes on failure

2) Event publishing and streaming
- EventProducer encapsulates Kafka publishing (topic names and serialization are handled in Kafka package).

3) Persistence and data layer
- MongoDB is raw data persistence for payment documents (spring-data-mongodb present).
- MyBatis starter and a Postgres driver are present as dependencies — repository includes both NoSQL and SQL integrations.
- PaymentDB class provides explicit Mongo operations; PaymentDatabaseServices encapsulates application-level DB workflows.

4) Ticketing and email delivery
- TicketSender and Email packages contain logic to build tickets/receipts and send emails.

5) OTP and Auth
- OTPController and Auth package provide endpoints for one-time passwords / authentication flows, backed by Redis for TTL and fast lookup.

6) Observability and health
- HealthController exposes liveness/readiness endpoints.
- Spring Boot Actuator is included for operational metrics and endpoints.

7) Caching / short-lived state
- Redis via spring-data-redis + Jedis: used for OTP/state caching and fast lookups.

## Unique / distinguishing features
- Event-first payment pipeline: every payment is persisted locally and published as a Kafka domain event in the same request flow, enabling asynchronous downstream processing and at-least-once event distribution.
- Integrated ticket/receipt generation using Gmail API (direct integration with Google APIs for email delivery rather than relying purely on SMTP).
- Hybrid persistence approach: MongoDB for document storage and SQL integration (MyBatis + Postgres driver) — useful for our polyglot persistence strategy.
- Lightweight OTP and ticketing subsystem co-located with ingestion: reduces cross-service coupling for smaller deployments.

## Operational considerations
- Idempotency: ingestion endpoints write to DB and publish to Kafka. To avoid duplicates downstream, ensure event deduplication by paymentID at consumers or use Kafka message keys and idempotent producers where appropriate.
- Error handling: controllers use a central ExceptionHandler package; confirm retry/backoff policies at Kafka consumer side.
- Scaling: Kafka-backed processing allows scaling ticket sender consumers independently from ingestion service.

## Files and places to inspect first (where to look in code)
- src/main/java/com/saipbuilds/kpaymentpipeline/Controller/PaymentController.java — ingress flow and how events are validated and sent
- src/main/java/com/saipbuilds/kpaymentpipeline/Kafka/EventProducer* — publisher implementation (topic names, serialization)
- src/main/java/com/saipbuilds/kpaymentpipeline/TicketSender/ — consumer logic that generates tickets and invokes email
- src/main/java/com/saipbuilds/kpaymentpipeline/Database/PaymentDB* and Service/PaymentDatabaseServices* — persistence contract and DB interactions
- src/main/resources/* — application properties and environment mappings
