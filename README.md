# RideGo — On-Demand Ride Booking Microservices

RideGo is a portfolio-ready ride-booking system built with **Java 17, Spring Boot 3, REST microservices, PostgreSQL, Redis GEO, and Next.js**.

It contains six independent backend services:

- Customer Service — customer profiles
- Rider Service — rider, vehicle and availability
- Booking Service — ride lifecycle and dispatch orchestration
- Location Service — Redis GEO location search
- Payment Service — simulated payment and idempotent settlement
- Notification Service — in-app notifications

The Next.js frontend provides a simple end-to-end demo for registering users, creating a ride and walking a booking through its lifecycle.

## Architecture

```text
                         Next.js :3000
                              |
                         REST APIs
                              |
                    +-------------------+
                    | Booking :8083     |
                    | Orchestration     |
                    +-------------------+
                     /   /   |   \   \
                    /   /    |    \   \
             Customer Rider Location Payment Notification
              :8081   :8082  :8084   :8085    :8086
                |       |       |       |         |
             Postgres Postgres Redis  Postgres  Postgres
```

Each service has its own Maven build and data ownership. Booking stores only external IDs (`customerId`, `riderId`) and communicates with other services over HTTP.

## Ride lifecycle

`CREATED → SEARCHING → RIDER_ASSIGNED → RIDER_ACCEPTED → RIDER_ARRIVED → OTP_VERIFIED → STARTED → COMPLETED`

Cancellation is supported before a ride starts. Rider reservation uses an atomic `ONLINE → BUSY` transition. Payment settlement uses an idempotency key based on the booking ID.

## Run with Docker

Prerequisite: Docker Desktop.

```bash
docker compose up --build
```

Then open:

**http://localhost:3000**

The compose stack starts PostgreSQL, Redis, all six Spring Boot services, and the Next.js frontend.

## Local development

Prerequisites:

- JDK 17
- Maven 3.9+
- Node.js 22+
- PostgreSQL
- Redis

Backend services can be started independently:

```bash
cd services/customer-service && mvn spring-boot:run
cd services/rider-service && mvn spring-boot:run
cd services/booking-service && mvn spring-boot:run
cd services/location-service && mvn spring-boot:run
cd services/payment-service && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
```

Frontend:

```bash
npm install
npm run typecheck
npm run build
npm start
```

Copy `.env.example` to `.env.local` when running the frontend outside Docker.

## Ports

| Component | Port |
|---|---:|
| Next.js frontend | 3000 |
| Customer Service | 8081 |
| Rider Service | 8082 |
| Booking Service | 8083 |
| Location Service | 8084 |
| Payment Service | 8085 |
| Notification Service | 8086 |
| PostgreSQL | 5432 |
| Redis | 6379 |

## Testing

A starter Postman collection is available at `postman/RideGo.postman_collection.json`.

GitHub Actions builds and tests all six Maven services and builds the Next.js frontend on every push/pull request.

## Repository structure

```text
.
├── services/
│   ├── customer-service/
│   ├── rider-service/
│   ├── booking-service/
│   ├── location-service/
│   ├── payment-service/
│   └── notification-service/
├── src/                         # Next.js frontend
├── infra/postgres/init.sql
├── postman/RideGo.postman_collection.json
├── docker-compose.yml
├── Dockerfile
└── docs/architecture.md
```

## Important portfolio note

This is a **portfolio/demo deployment**, not a production commercial ride-hailing platform. Payment is simulated and no real money is processed. Before production use, add authentication/authorization, secret management, database migrations, an API gateway/rate limiting, a real payment PSP and webhooks, asynchronous messaging, observability, distributed tracing, automated integration tests, and stronger reconciliation/retry workflows.

The project is inspired by the general ride-booking domain and is not an official Rapido application.
