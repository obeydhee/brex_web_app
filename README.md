# Brex Web App

A minimal fullstack scaffold:

- **Database:** SQLite (single file, no server to run)
- **API layer:** Java 21 + Spring Boot (REST API over `/api/items`)
- **Frontend:** React (Vite) that lists, adds, and deletes items via the API
- **Build:** a single Gradle multi-project build drives both the backend jar
  and the frontend production bundle (the frontend module wraps `npm`
  via the `node-gradle` plugin)

## Project layout

```
.
├── backend/     Spring Boot API (Gradle subproject)
│   └── src/main/java/com/brex/demo/
│       ├── model/        Item JPA entity
│       ├── repository/   ItemRepository (Spring Data JPA)
│       ├── controller/   ItemController (REST endpoints)
│       └── config/       CORS config, SQLite dir bootstrap, seed data
├── frontend/    React app (Gradle subproject, built with Vite)
│   └── src/
│       ├── App.jsx    UI: list / add / delete items
│       └── api.js     fetch() wrapper for the backend API
├── build.gradle       Root build, aggregates backend + frontend
└── settings.gradle    Declares the two subprojects
```

## Prerequisites

- JDK 21+
- Node.js 22+ / npm (only needed if you want to run `npm` directly; the
  Gradle build downloads nothing extra since it uses the Node/npm already
  on your machine — see `frontend/build.gradle`)

No local Gradle install is required — use the included wrapper (`./gradlew`).

## Build everything

```bash
./gradlew build
```

This compiles and tests the Spring Boot backend (`backend/build/libs/*.jar`)
and installs npm dependencies + produces a production frontend bundle
(`frontend/dist/`).

## Run it locally

**1. Start the backend** (serves the API on `http://localhost:8080`, and
creates `backend/data/app.db` on first run with a few seeded items):

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

Open `http://localhost:5173` — you'll see the seeded items, and can add or
delete items through the form, which round-trips to the SQLite database.

> **Note:** `SQLITE_DB_PATH` defaults to the relative path `./data/app.db`,
> which is resolved against whatever directory you *launch* the process
> from — `./gradlew :backend:bootRun` runs with `backend/` as the working
> directory, so the file lands at `backend/data/app.db`. If you instead run
> the packaged jar directly (`java -jar backend/build/libs/*.jar`) from the
> repo root, the file lands at `<repo root>/data/app.db` instead. Either
> `cd backend` first, or set `SQLITE_DB_PATH` explicitly, to control where
> it goes.

### Running the production build

After `./gradlew build`, you can also serve the static frontend bundle from
`frontend/dist/` with any static file server, pointing it at a backend
started with `./gradlew :backend:bootRun` (adjust `app.cors.allowed-origins`
if the frontend isn't on `localhost:5173`).

## API

| Method | Path              | Description          |
|--------|-------------------|-----------------------|
| GET    | `/api/items`      | List all items        |
| GET    | `/api/items/{id}` | Get one item          |
| POST   | `/api/items`      | Create an item (`{"name": "...", "description": "...", "quantity": 0}`; `quantity` defaults to `0` if omitted) |
| DELETE | `/api/items/{id}` | Delete an item         |

## Configuration

Backend settings (`backend/src/main/resources/application.yml`) can be
overridden with environment variables:

- `SQLITE_DB_PATH` — path to the SQLite file (default `./data/app.db`)
- `SERVER_PORT` — backend port (default `8080`)
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins for the API
  (default `http://localhost:5173`)

## Tests

```bash
./gradlew :backend:test
```

Runs `ItemControllerTest`, which exercises the REST API against a throwaway
SQLite file via `MockMvc`.
