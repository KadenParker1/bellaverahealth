# Bellavera

Personalized women's health app. A user signs up, completes an onboarding survey, then works
through four themed surveys (Exercise, Nutrition, Hormones, Pelvic Floor). Their answers become
the context for an LLM chat assistant that gives personalized, non-diagnostic guidance. A store
sells physical products alongside the guidance, an admin console runs survey authoring, product
and order management, and a blog, and order/shipment fulfillment is tracked end to end.

**Stack:** Spring Boot 4.1 / Java 21 monolith + React 19 SPA + Supabase (Postgres & Auth).

For the full architecture, API surface, and design decisions, see [`CLAUDE.md`](./CLAUDE.md) - this
file is a front door, not the reference.

## Quick start

```bash
# One-time: copy the local config templates
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
cp frontend/.env.local.example frontend/.env.local

supabase start                                              # local Postgres :54322 + GoTrue :54321
./mvnw spring-boot:run -Dspring-boot.run.profiles=local      # backend :8080
cd frontend && npm install && npm run dev                    # frontend :5173
```

Open `http://localhost:5173`. Sign up through the SPA - auth is fully local against the Supabase
CLI stack, no external account needed. There is no UI yet for granting admin, so promote yourself
in SQL against the local DB:

```sql
update app.app_user set role = 'ADMIN' where email = '…';
```

API docs (dev only): `http://localhost:8080/docs`.

## Testing

```bash
./mvnw test                              # Testcontainers Postgres - no local DB needed, needs Docker running
cd frontend && npm run build && npm run lint
```

CI runs both on every push and pull request against `master` (`.github/workflows/ci.yml`) - no
secrets required, since tests run entirely against mock LLM/payment/email adapters.

## What's here

| Area | Status |
|---|---|
| Auth, onboarding, surveys, chat (mock LLM) | Built |
| Store: catalog, Stripe Checkout, fulfillment | Built (Stripe wired but untested against a real account) |
| Admin console: surveys, products, orders, users, blog, contact inbox | Built |
| Blog, About, Contact | Built |
| Real survey content | Not started - waiting on reviewed clinical copy |
| Insight scoring engine | Deferred - waiting on clinical/business scoring rules |
| Real LLM provider | Deliberately deferred - mock only until explicitly requested |
| Deploy (Railway + Vercel) | Not started |

See CLAUDE.md's staged plan for the full breakdown and the reasoning behind what's deferred.

## Project layout

```
src/main/java/com/pm/bellavera/   Spring Boot backend, package-by-feature
frontend/src/                     React 19 SPA
src/main/resources/db/migration/  Flyway migrations (never edit an applied one - add a new file)
.github/workflows/                CI
```
