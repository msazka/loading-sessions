# Charging Sessions

Small internal tool for EV charging sessions: start a session at a charge point, stop it with the
charged energy, and see the cost.

- **backend/**: Kotlin, Spring Boot 3, Spring Data JPA, H2 in-memory database
- **frontend/**: React + TypeScript (Vite)

## Run

Backend (http://localhost:8080):

```bash
cd backend
./gradlew bootRun
```

Frontend (http://localhost:5173, proxies `/api` to the backend):

```bash
cd frontend
npm install
npm run dev
```

## Test

```bash
cd backend && ./gradlew test
cd frontend && npm run build
```

## API

| Method | Path | Description |
|---|---|---|
| GET | `/api/tariffs` | List tariffs (seeded on startup) |
| POST | `/api/sessions` | Start a session: `{ customerId, chargePointId, tariffCode }` |
| POST | `/api/sessions/{id}/stop` | Stop a session: `{ energyKwh }` |
| GET | `/api/sessions?customerId=` | List sessions, newest first, optional customer filter |

## Pricing

`cost = sessionFee + energyKwh × pricePerKwh`, rounded half-up to cents. All prices are gross EUR.
