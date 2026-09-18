# Brex Pay

A peer-to-peer payment app scaffold:

- **Database:** SQLite (single file, no server to run)
- **API layer:** Java 21 + Spring Boot — user profiles, accounts, and
  peer-to-peer payments over `/api/**`
- **Frontend:** React (Vite) — sign up / log in, view balance, deposit /
  withdraw, send payments, browse transaction history
- **Build:** a single Gradle multi-project build drives both the backend jar
  and the frontend production bundle (the frontend module wraps `npm`
  via the `node-gradle` plugin)

## Project layout

```
.
├── backend/     Spring Boot API (Gradle subproject)
│   └── src/main/java/com/brex/demo/
│       ├── model/        JPA entities: UserProfile, Account, AccountLedgerEntry, Transaction
│       ├── repository/   Spring Data repositories
│       ├── service/      Business logic: UserProfileService, AccountService, PaymentService
│       ├── security/     JWT issuing/parsing, Spring Security config
│       ├── controller/   REST endpoints
│       ├── dto/          Response records (keep entities like passwordHash out of JSON)
│       └── config/       CORS config, SQLite dir bootstrap
├── frontend/    React app (Gradle subproject, built with Vite)
│   └── src/
│       ├── api/           fetch() wrappers per resource (auth, accounts, payments)
│       ├── context/       AuthContext — holds the JWT + current profile
│       ├── views/         Login, Signup, Dashboard, SendPayment, History
│       └── App.jsx        View-switching shell (no router)
├── scripts/
│   └── bootstrap-demo-data.sh   Seeds 3 demo users with starting balances for manual testing
├── build.gradle       Root build, aggregates backend + frontend
└── settings.gradle    Declares the two subprojects
```

## Data model

- **UserProfile** — `id`, `firstName`, `lastName`, `email` (unique), `phoneNumber`,
  `paymentName` (unique handle, e.g. `jane_doe`), `passwordHash`, `createdAt`.
- **Account** — 1:1 with `UserProfile`, holds `balance` (`BigDecimal`, stored as
  exact integer cents under the hood — see `MoneyCentsConverter` — to avoid any
  floating-point drift), optimistic-locked via `@Version` so concurrent
  deposits/withdrawals/payments can't silently lose an update.
- **AccountLedgerEntry** — an append-only audit log of every deposit and
  withdrawal (`entryType`, `amount`, `balanceAfter`, `createdAt`). Never
  updated or deleted, and kept separate from `Transaction`.
- **Transaction** — a peer-to-peer payment attempt: `senderId`, `receiverId`,
  `amount`, `status` (`PENDING` / `SUCCESS` / `FAILURE` / `DISPUTED` —
  `PENDING`/`DISPUTED` are modeled but unused by this MVP, which resolves
  synchronously to `SUCCESS` or `FAILURE`), `note`, `createdAt`, `updatedAt`.
  Insufficient funds is a **recorded FAILURE transaction (HTTP 201)**, not a
  4xx error — the request was well-formed and a real transaction resource was
  created, it just didn't succeed.

## Prerequisites

- JDK 21+
- Node.js 22+ / npm (only needed if you want to run `npm` directly; the
  Gradle build downloads nothing extra since it uses the Node/npm already
  on your machine — see `frontend/build.gradle`)
- `jq` (only for `scripts/bootstrap-demo-data.sh`)

No local Gradle install is required — use the included wrapper (`./gradlew`).

## Build everything

```bash
./gradlew build
```

This compiles and tests the Spring Boot backend (`backend/build/libs/*.jar`)
and installs npm dependencies + produces a production frontend bundle
(`frontend/dist/`).

## Run it locally

**1. Start the backend** (serves the API on `http://localhost:8080`; the
SQLite file is created on first run with no seeded data — sign up through the
API or frontend, or run `scripts/bootstrap-demo-data.sh` for ready-made demo
accounts):

```bash
./gradlew :backend:bootRun
```

**2. Start the frontend dev server** (serves the UI on
`http://localhost:5173` and proxies `/api/*` to the backend):

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`, sign up, and you're in.

> **Note:** `SQLITE_DB_PATH` defaults to the relative path `./data/app.db`,
> which is resolved against whatever directory you *launch* the process
> from — `./gradlew :backend:bootRun` runs with `backend/` as the working
> directory, so the file lands at `backend/data/app.db`. If you instead run
> the packaged jar directly (`java -jar backend/build/libs/*.jar`) from the
> repo root, the file lands at `<repo root>/data/app.db` instead. Either
> `cd backend` first, or set `SQLITE_DB_PATH` explicitly, to control where
> it goes.

### Seeding demo data for manual testing

```bash
./scripts/bootstrap-demo-data.sh
```

Signs up 3 demo users (`demo_alice`, `demo_bob`, `demo_carol`, password
`demo-password-123`) against a locally-running backend and deposits a
starting balance into each, then prints ready-to-run `curl` examples for
sending payments and checking history between them. Requires the backend to
already be running and `jq` to be installed. Safe to re-run.

### Running the production build

After `./gradlew build`, you can also serve the static frontend bundle from
`frontend/dist/` with any static file server, pointing it at a backend
started with `./gradlew :backend:bootRun` (adjust `app.cors.allowed-origins`
if the frontend isn't on `localhost:5173`).

## API

All endpoints are under `/api`. Amounts are numbers with at most 2 decimal
places (e.g. `25.00`).

**Auth note:** signup/login issue a real JWT (`Authorization: Bearer <token>`),
and the frontend uses it. **Enforcement is currently disabled** so the API is
easy to exercise directly (e.g. with `curl`) while iterating — every
authenticated-in-spirit endpoint below also accepts a plain `?userId=<id>`
query param as a fallback identity source when no token is supplied. See the
comment on `SecurityConfig` for how to re-enable enforcement.

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/users` | Create a profile (`{firstName, lastName, email, phoneNumber, paymentName, password}`) + a zero-balance account, returns `{token, profile}` |
| GET    | `/api/users/me` | Current user's profile (token or `?userId=`) |
| GET    | `/api/users/lookup?paymentName=` | Look up a peer's public info by payment name |
| POST   | `/api/auth/login` | `{identifier, password}` (email or payment name) → `{token, profile}` |
| GET    | `/api/accounts/me` | Current balance (token or `?userId=`) |
| POST   | `/api/accounts/me/deposit` | `{amount}` — add money, appends a ledger entry |
| POST   | `/api/accounts/me/withdraw` | `{amount}` — withdraw money (400 if insufficient funds), appends a ledger entry |
| GET    | `/api/accounts/me/ledger?page=` | Paginated (15/page) deposit/withdrawal audit history |
| POST   | `/api/payments` | `{receiverPaymentName, amount, note?}` — send a payment; returns the `Transaction` with `status: SUCCESS` or `FAILURE` |
| GET    | `/api/transactions/me?window=1D\|1W\|1M&page=` | Paginated (15/page) transaction history, sender or receiver, rolling window from now (defaults to `1W`) |
| GET    | `/api/transactions/{id}` | A single transaction (403 if you're not a party to it) |

Example:

```bash
# Sign up (no token needed for this one — it's how you get one)
curl -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@example.com","phoneNumber":"555-0100","paymentName":"jane_doe","password":"secret123"}'

# Everything else: either pass the token from above, or just ?userId=<id>
curl -X POST "http://localhost:8080/api/accounts/me/deposit?userId=1" \
  -H 'Content-Type: application/json' -d '{"amount":100.00}'

curl -X POST "http://localhost:8080/api/payments?userId=1" \
  -H 'Content-Type: application/json' -d '{"receiverPaymentName":"other_user","amount":25.00,"note":"lunch"}'

curl "http://localhost:8080/api/transactions/me?userId=1&window=1W"
```

## Configuration

Backend settings (`backend/src/main/resources/application.yml`) can be
overridden with environment variables:

- `SQLITE_DB_PATH` — path to the SQLite file (default `./data/app.db`)
- `SERVER_PORT` — backend port (default `8080`)
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins for the API
  (default `http://localhost:5173`)
- `JWT_SECRET` — HMAC signing secret for JWTs (dev-only insecure default —
  override before any real deployment)
- `JWT_EXPIRATION_MINUTES` — token lifetime in minutes (default `1440`, i.e. 24h)

## Tests

```bash
./gradlew :backend:test
```

Covers signup/login, deposit/withdraw (including insufficient-funds and
ledger pagination), payments (success, insufficient funds → recorded
FAILURE, self-payment rejection, unknown receiver), transaction history
(window filtering, pagination, per-transaction access control), JWT
generate/parse/expiry, and the optimistic-locking mechanism on `Account`.
