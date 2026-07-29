# Soundshelf

Search the iTunes catalog, build a personal album library, and get analytics and
natural-language search over it.

**Live app:** _(to be filled in)_ · **API:** _(to be filled in)_

**Demo login:** `demo@soundshelf.app` / `demo1234` — pre-loaded with 80 albums across
15 genres and 8 decades so the dashboard has something real to show.

---

## Entity choice: albums

The brief asks for one entity. I picked **albums**, and it wasn't really a preference —
it's the only entity iTunes returns enough data for.

| Entity | What the API gives back |
|---|---|
| `album` | collectionId, collectionName, artistName, primaryGenreName, releaseDate, trackCount, artworkUrl100, collectionPrice |
| `song` | Same fields, but describing the album the track sits on |
| `musicArtist` | `artistId`, `artistName`, `primaryGenreName`. That's it — no artwork, no dates, no counts |

An artist-focused app couldn't populate `release_date`, `track_count`, or `artwork_url`
at all, and with one usable dimension (genre) there's nothing to build four charts on.
Albums give four independent dimensions — genre, release date, track count, price —
which is what makes the analytics page more than a single bar chart.

**Search still works the way people think about music.** All three search modes resolve
to albums:

- `type=album` — searches albums directly
- `type=song` — searches songs and returns *the album each song is on*, because iTunes
  includes the parent album's `collectionId` on every track result
- `type=artist` — resolves the artist, then pivots their `artistId` through
  `/lookup?entity=album` to return their discography

So you can search "shape of you" and save *÷ (Deluxe)*. One entity end to end, three
ways in.

## Database: PostgreSQL

- The schema is fixed and known upfront. There's no document-shaped or schemaless data
  anywhere in this app.
- Every analytics number is a `GROUP BY` — genre counts, decade counts, releases per
  year, track-count histogram, top artists. That work belongs in the database, not in
  application code.
- Saving the same album twice needs to be *impossible*, not just unlikely. A unique
  constraint on `(user_id, apple_catalog_id)` gives that for free; deleting a user needs
  to take their library with it, which `on delete cascade` does.

Flyway owns the schema and Hibernate runs with `ddl-auto: validate`, so Hibernate can
never quietly alter a deployed database, and a mismatch between an entity and the
migration fails at startup rather than at runtime.

### Schema

```sql
create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name  varchar(100),
    created_at    timestamptz  not null default now()
);

create table library_items (
    id               bigserial primary key,
    user_id          bigint       not null references users (id) on delete cascade,
    apple_catalog_id bigint       not null,
    title            varchar(500) not null,
    artist_name      varchar(500) not null,
    genre            varchar(120),
    release_date     date,
    track_count      integer,
    artwork_url      varchar(1000),
    collection_price numeric(10, 2),
    user_rating      smallint,
    user_notes       text,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),

    constraint uq_library_user_album unique (user_id, apple_catalog_id),
    constraint ck_library_rating check (user_rating is null or user_rating between 1 and 5)
);

create index idx_library_user_created  on library_items (user_id, created_at desc);
create index idx_library_user_genre    on library_items (user_id, genre);
create index idx_library_user_release  on library_items (user_id, release_date);
```

Every index leads with `user_id` because every read is scoped to the owner — there is no
query in this app that looks at library rows across users.

---

## API

Everything except `/api/auth/**` and the health endpoint needs `Authorization: Bearer <jwt>`.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/auth/register` | → `{token, tokenType, expiresInSeconds, user}`; 409 if the email is taken |
| `POST` | `/api/auth/login` | Same shape; 401 on bad credentials |
| `GET` | `/api/auth/me` | Current user |
| `GET` | `/api/search?query=&type=album\|song\|artist&limit=` | Proxies iTunes, 10-minute cache, marks results already in your library |
| `GET` | `/api/library?page=&size=&sort=&genre=&search=` | Paginated, defaults to 12 per page, newest first |
| `GET` | `/api/library/genres` | Distinct genres, for the filter dropdown |
| `POST` | `/api/library` | Body is `{appleCatalogId, userRating?, userNotes?}` → 201; 409 if already saved |
| `PUT` | `/api/library/{id}` | Rating and notes only |
| `DELETE` | `/api/library/{id}` | 204 |
| `GET` | `/api/analytics/summary` | Every chart on the dashboard, in one response |
| `POST` | `/api/ai/query` | `{question}` → interpretation + filter + matching albums |
| `GET` | `/api/ai/status` | Whether answers come from the model or the fallback |

### Two decisions worth calling out

**`POST /api/library` only accepts a catalog id.** Title, artist, genre, release date,
artwork and price are fetched from iTunes server-side at save time. A client can't store
an album that was never in the catalog, or quietly change the genre of one that was.
The only things a client supplies are the two fields that are genuinely theirs — rating
and notes.

**Items you don't own return 404, not 403.** A 403 would confirm that the id exists,
which is more than a caller needs to know. Every library query is scoped by the user id
taken from the JWT subject; no endpoint accepts a user id as a parameter, so there is no
path to another user's rows.

### Errors

One shape everywhere, from a single `@RestControllerAdvice`:

```json
{
  "timestamp": "2026-07-28T13:36:11.264Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/library",
  "fieldErrors": { "userRating": "must be less than or equal to 5" }
}
```

400 validation · 401 bad credentials or missing token · 403 forbidden · 404 not found ·
409 duplicate · 429 rate limited · 502 iTunes unreachable · 500 catch-all. Bad-credential
responses are deliberately identical whether the email or the password was wrong, and the
catch-all logs the stack trace but never returns it.

---

## Analytics

`GET /api/analytics/summary` returns all six datasets in a single response. Each one is a
SQL aggregate, so the payload is proportional to the number of *groups*, not the size of
the library — a 5,000-album library returns the same few hundred bytes as an 80-album one.
One request also means the stat tiles can't disagree with the charts because the library
changed halfway through loading.

| Chart | Data |
|---|---|
| Bar | Albums per genre |
| Donut | Share by decade |
| Line | Releases by year |
| Histogram | Track-count distribution (1–5, 6–10, 11–15, 16–20, 21+) |
| Horizontal bar | Top 10 artists by album count |
| Line | Library growth over time (cumulative) |

Histogram buckets are ordered in Java rather than SQL — sorting by label puts "11-15"
ahead of "6-10", and sorting by count would reshuffle the axis every time you saved an
album.

---

## AI feature: natural-language library search

Ask `"which alternative albums from the 2000s do I have?"` or `"my 5 star jazz records"`
and get the matching albums back.

**The model never writes SQL and never sees the database.** It's given the question and a
JSON schema describing a fixed filter, and it fills in that filter — genres, year range,
rating range, track-count range, artist/title text. The backend then clamps every value
into a valid range and applies it as the same parameterised query the library list
endpoint already uses.

```
question ─▶ Claude (structured output, JSON schema) ─▶ InterpretedFilter
                                                            │ clamp + validate
                                                            ▼
                                              LibraryFilter ─▶ JPA Specification
                                                            (always scoped to your user id)
```

That structure is the security story. The model's output is data, not code — the worst a
prompt injection can achieve is a filter that returns the wrong subset of *your own*
albums. There's no query it can express that the UI couldn't already make.

Other details:

- **The interpretation is shown to the user.** Every response includes a plain-English
  sentence describing the filter that was applied, so a misread is visible instead of
  silently returning the wrong albums.
- **It works without an API key.** If `ANTHROPIC_API_KEY` isn't set, or the call fails, a
  deterministic keyword parser handles decades, genres, ratings and album length. The
  response says which path produced it (`"source": "model"` or `"keyword-fallback"`), and
  `GET /api/ai/status` lets the UI say so upfront. The demo never breaks on a missing key.
- **Rate limited** to 10 questions per minute per user.
- Model: `claude-opus-5`, low effort (this is a translation task, not a reasoning one).

---

## Running it locally

Needs JDK 21, Node 20+, and Docker.

```bash
# 1. Database
docker compose up -d          # or: docker-compose up -d

# 2. Backend  → http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. Frontend → http://localhost:3000
cd frontend && npm install && npm run dev

# 4. Optional: fill the demo account with 80 albums
./scripts/seed-demo.sh
```

The AI feature runs on the keyword fallback until you export `ANTHROPIC_API_KEY`.

### Configuration

| Variable | Default | Notes |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/soundshelf` | JDBC form, not a `postgres://` URI |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `soundshelf` | |
| `JWT_SECRET` | dev-only default | **Must** be set in any deployed environment; ≥32 bytes |
| `JWT_TTL` | `PT24H` | ISO-8601 duration |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated |
| `ANTHROPIC_API_KEY` | _(empty)_ | Omit to run the AI feature on the fallback parser |
| `PORT` | `8080` | |

### Tests

```bash
cd backend && ./mvnw test
```

33 tests, ~40 seconds, no database or Docker required. They cover the rules that would
actually hurt if they broke: ownership scoping (another user's item is a 404, and the
service is never reached), duplicate saves, catalog metadata coming from iTunes rather
than the request body, the security filter chain rejecting missing/forged/wrongly-signed
tokens, date and artwork parsing edge cases, and the clamping that keeps AI-generated
filters inside a valid range.

---

## Trade-offs

Things I chose deliberately, and what I'd do differently with more time.

**No refresh tokens.** A 24-hour access token, and you log in again. Refresh-token
rotation means a token store, revocation, and reuse detection — real work that this
brief doesn't ask for. The cost is that a stolen token is valid until it expires.

**The token is stored in `localStorage`.** An httpOnly cookie would be safer against XSS,
but needs a same-site story and CSRF protection for a cross-origin SPA. I took the simpler
option and I'm naming it rather than pretending it's free.

**Analytics are computed live on every request.** At library sizes a person would
actually have, six indexed aggregates are trivially fast. At a million rows per user I'd
precompute into a summary table or a materialised view refreshed on write.

**The AI rate limiter is in-memory.** Fine for one instance; behind a load balancer it
would need to move to Redis.

**iTunes is proxied server-side rather than called from the browser.** It costs a hop,
but it's what makes response caching, the `alreadySaved` flag, and consistent error
handling possible — and the browser can't call it directly anyway without hitting CORS.

**Spring Boot 3.5, not 4.0.** Boot 4 was released weeks ago and shifts starter names and
Jackson major version. 3.5 is what the large majority of production Spring services run
and has the widest library compatibility. For something that had to ship in three days,
that mattered more than being on the newest line.

**The Anthropic call is raw HTTP, not the Java SDK.** It's one request/response with a
JSON-schema-constrained result, and the app already has a configured `RestClient` with
timeouts and error handling for the iTunes proxy. A second HTTP stack for a single
endpoint wasn't worth the dependency.

**No database-backed integration test.** I tried Testcontainers for a
migration-versus-entity drift check and it hung in my environment; rather than ship a
test that intermittently blocks a build, I dropped it and kept the suite Docker-free.
Drift is still caught — `ddl-auto: validate` fails application startup if an entity and
the migration disagree — but it's caught at boot rather than in CI. Adding this back
with a working Testcontainers setup is the first thing I'd do next.

**Free-tier hosting sleeps.** The backend cold-starts after inactivity, so the first
request can take up to a minute. The UI shows a waking state rather than looking broken.
