# Bellavera

Personalized women's health app. A user signs up, completes an onboarding survey, then works
through four themed surveys (Exercise, Nutrition, Hormones, Pelvic Floor). Their answers become
the context for an LLM chat assistant that gives personalized, non-diagnostic guidance. A store
sells physical products alongside the guidance, with order and shipment fulfillment run from an
admin console.

**Stack:** Spring Boot 4.1 / Java 21 monolith + React 19 SPA + Supabase (Postgres & Auth).

---

## The staged plan

| Stage | Scope | Status |
|-------|-------|--------|
| 0 | Project scaffolding, Maven/Spring Boot setup, profiles (`local`/`prod`/`test`), CORS, Actuator, OpenAPI | Done |
| 1 | Flyway-managed `app` schema — identity, versioned surveys, responses, insights, chat, audit (`V1`–`V6`) | Done |
| 2 | Auth — Supabase JWT as OAuth2 resource server, JIT user provisioning, `@CurrentUser`, role mapping | Done |
| 3 | Survey + response APIs — per-type answer validation, display-rule-aware required checks, submit events | Done |
| 4 | LLM harness **shell** — `LlmChatService` port, RAG context assembly, `/chat` persistence, mock adapter only | Done |
| 5 | **Admin console — surveys** — controllers behind `/api/v1/admin/**`, a `ROLE_ADMIN` area in the SPA, survey authoring (create / edit draft / publish / retire), audit-log writes | Done |
| 6 | **Store — catalog & checkout** — `product` schema (`V7`), public catalog API, Stripe Checkout, webhook signature verification, `customer_order` records, product CRUD in the admin shell | Done |
| 7 | **Fulfillment & inventory** — fulfillment columns on `customer_order`, the packing queue and order history, a "mark fulfilled" action, and stock (`V8`) drawn down when an order ships | Done |
| 8 | Real survey content — author it through the Stage 5 editor, or ship it as a migration | **Next** |
| 9 | Insight scoring engine — schema and read API exist; the scoring itself waits on the clinical/business rules being defined | Deferred |
| 10 | Real LLM provider adapter — only when explicitly asked (see [Constraints](#constraints)) | Deliberately deferred |
| 11 | Deploy — backend on Railway, frontend on Vercel, hosted Supabase | Not started |

Stages 0–4 landed in `5c8cf5d`; the frontend landed in `c28fca6`; Stages 5–7 landed together.

**What Stages 5–7 do not include yet:** no public (signed-out) storefront — the shop lives inside
the authenticated shell — no refund or cancel flow beyond an expired checkout, and no Stripe account
is connected: `bellavera.store.payment-provider` is `mock` everywhere but `prod`. (Order-confirmation
and shipped emails landed after Stage 7 — see "What landed after Stage 7" below.)

**Why the scoring engine moved back.** Stage 5 was the insight engine. It is now Stage 9: the
rules that turn answers into insights are a business/clinical decision, not a coding one, and
nothing should be written against guessed thresholds. The seams stay cut and unused until those
rules exist — see [Deferred: the insight scoring engine](#deferred-the-insight-scoring-engine).

---

## Architecture

### Backend — `src/main/java/com/pm/bellavera/`

Package-by-feature, not by layer. Each feature package holds its entities, repositories, and
services at the top level, with DTOs and controllers in a nested `api/` package.

```
user/       AppUser, UserProfile, JIT provisioning, @CurrentUser resolver, /api/v1/me
survey/     Survey → SurveyVersion → SurveySection → Question → QuestionOption (read side)
response/   SurveyResponse, Answer, AnswerValidator, DisplayRuleEvaluator (write side)
insight/    InsightRun, Insight, InsightRule — schema + read API exist, engine does not (Stage 9)
chat/       ChatThread, ChatMessage, ChatContextSnapshot, ChatService, UserContextBuilder
llm/        LlmChatService port + records + MockLlmChatService. No provider SDK types here.
store/      Product, CustomerOrder, OrderItem, PaymentEvent; CatalogService, CheckoutService,
            PaymentApplicationService, OrderService, FulfillmentService; payment/ holds the gateway port;
            OrderEmailListener sends the confirmation/shipped emails, AFTER_COMMIT off OrderPaidEvent /
            OrderFulfilledEvent
email/      EmailGateway port + records + MockEmailGateway. No provider SDK types here.
blog/       BlogPost, BlogService (published posts only), SlugGenerator — no versioning like a survey
contact/    ContactMessage, ContactService — a message from a signed-in user to the admin inbox
admin/      AdminSurveyService (authoring), AdminProductService, AdminBlogService, AdminContactService,
            AdminEmailService (the opt-in broadcast); api/ holds every /admin controller
audit/      AuditLog + AuditService — every admin mutation writes a row here
config/     SecurityConfig, SupabaseJwtAuthenticationConverter, CorsProperties, WebMvcConfig
common/     AuditableEntity, GlobalExceptionHandler (RFC 7807 ProblemDetail), domain exceptions,
            PageResponse<T> — the paginated-list shape used wherever a list can grow unbounded
```

Fulfillment lives in `store/` rather than its own package: with one shipment per order it is order
state, not a separate aggregate.

### Key design decisions

**Supabase owns identity; we mirror it.** `app_user.id` *is* `auth.users.id` — never generated
locally. `UserProvisioningService.resolve(jwt)` creates the `app_user` + `user_profile` rows the
first time a verified caller is seen. Any controller taking `@CurrentUser AppUser` gets a
provisioned user for free.

**Authorities come from our `app_user.role`, not the JWT.** Supabase's `role` claim is always
`"authenticated"`, so `SupabaseJwtAuthenticationConverter` looks up the local role and maps it to
`ROLE_USER` / `ROLE_ADMIN`.

**Supabase signs with ES256, not RS256.** Spring's `JwtDecoder` defaults to RS256 and will fail
verification silently-looking (401) if this isn't set. Every profile pins:
```properties
spring.security.oauth2.resourceserver.jwt.jws-algorithms=ES256
```

**Surveys are versioned and immutable once published.** A partial unique index enforces at most
one `PUBLISHED` version per survey. Responses reference a `survey_version_id`, never a
`survey_id`, so historical answers stay interpretable after content changes. **Never edit an
applied migration** — add a new versioned file instead.

**Answers are polymorphic across typed columns.** `answer` has `value_text` / `value_number` /
`value_boolean` / `value_date` / `value_json` plus an `answer_option` join table for choice
questions. `AnswerValidator` enforces the shape per `QuestionType` against the question's
`config` JSONB (`min`, `max`, `maxLength`, `scaleLabels`).

**Display rules gate required-ness.** `DisplayRuleEvaluator` reads a question's `display_rule`
JSONB so a hidden question isn't reported as a missing required answer on submit. Only
`{"all": [{"questionCode","op","value"}]}` (AND of `eq`/`ne`/`in`) is supported — extend it if
branching gets richer.

**Draft vs. submit is one endpoint.** `POST /surveys/{id}/responses` upserts; required-field
checks run only when `status: "SUBMITTED"`. A partial unique index allows exactly one
`IN_PROGRESS` draft per user per version.

**RAG without vectors.** `UserContextBuilder` deterministically renders profile + latest
submitted answers per survey + current insights into plain text, hashes it (SHA-256), and reuses
the stored `chat_context_snapshot` until the content actually changes. Truncated to
`bellavera.llm.context-token-budget × 4` chars.

**The LLM boundary is a single port.** `LlmChatService.complete(LlmRequest) → LlmReply`. No
provider SDK type crosses it. Swapping providers = add an adapter class + a branch in
`LlmConfig`, and change nothing else.

**Errors are RFC 7807 everywhere.** `GlobalExceptionHandler` maps `NotFoundException` → 404,
`ValidationException` → 400 with an `errors` array, `AccessDeniedException` → 403,
`IllegalStateException` → 409, `ObjectOptimisticLockingFailureException` → 409, and
`PaymentException` → 400.

**The payment boundary is a port, like the LLM one.** `PaymentGateway` has two methods —
`createCheckoutSession` and `parseWebhook` — and no Stripe type crosses it. `MockPaymentGateway`
takes no money and verifies nothing, which is what makes the whole store flow testable offline;
`PaymentConfig` refuses it under the `prod` profile.

**The email boundary is a port too.** `EmailGateway.send(EmailMessage)` — one method, no provider
SDK type crosses it. `MockEmailGateway` logs instead of sending; `EmailConfig` throws for any
provider name other than `mock`, same shape as `LlmConfig`. Nothing has flipped this to a real
provider yet — order-confirmation/shipped emails and the admin broadcast all go through it, and all
of them are exercisable offline today.

**Only a verified webhook marks an order paid.** Checkout creates a `PENDING` order and hands back
a URL. `PaymentApplicationService` is the only thing that writes `PAID`, and it is idempotent twice
over: `payment_event`'s primary key rejects a redelivered event, and each transition re-checks the
order's current status. A browser landing on the success URL proves nothing.

**Money is resolved server-side.** A checkout request is product codes and quantities. Prices come
from `app.product`, the currency from `StoreProperties`, and both redirect URLs from configuration —
a client-supplied redirect target is an open redirect. Order lines snapshot the code, name, and unit
price at purchase time.

**Stock is drawn down at fulfillment, but sold against availability.** `product.stock_quantity`
is nullable — null means "not stock-tracked", which is also what every product predating `V8`
became; a `NOT NULL DEFAULT 0` would have marked the whole catalog out of stock. The units
physically leave when the parcel does, so `InventoryService.consumeForFulfilledOrder` decrements
inside the fulfillment transaction. But a paid, unshipped order has already spoken for its units,
so selling against raw stock would let one unit sell many times over: **availability = stock −
what PAID-but-unfulfilled orders owe**, and that is the number the storefront shows and the
checkout guard enforces. PENDING orders are deliberately excluded — an abandoned checkout must not
hold stock hostage. Two people can still buy the last unit simultaneously, since nothing is
committed until a payment lands; that is the usual trade for a shop that does not reserve
inventory in a cart. Oversold stock clamps at zero and logs loudly rather than going negative.

**Fulfillment is a guarded transition on the order.** One shipment per order, so `fulfilled_at`,
`carrier`, `tracking_number`, and `fulfilled_by` are columns on `customer_order`; `OrderStatus` is
the derived fulfillment status and the only thing to read. Fulfilling a non-`PAID` order is a 409,
which is what makes a double click harmless, and `lock_version` closes the concurrent-admin window.
Two DB check constraints back this up: a `FULFILLED` order must have a `fulfilled_at`, and a
`PAID`/`FULFILLED` one must have a `paid_at`.

**Removal is deactivation, for surveys and products alike.** Responses reference a
`survey_version_id` and order lines reference a `product`; deleting either would orphan real
history. Only an unpublished draft can actually be deleted, because nothing can have answered it.

**Published survey versions are immutable.** The admin editor enforces it: `PUT` on a non-draft is a
409. Editing a live survey is clone-to-draft → edit → publish, and publishing archives the version
it replaced (the partial unique index allows exactly one published version, so the archive is
flushed first).

### Frontend — `frontend/src/`

React 19 + Vite 8 + TypeScript + Tailwind v4 + TanStack Query + React Router 7.

```
auth/       AuthProvider (supabase-js session), RequireAuth, RequireOnboarding, RedirectIfAuthed
surveys/    renderer/ — SurveyRenderer + a questionRegistry mapping QuestionType → component
themes/     HomePage (4 theme cards), ThemeDetailPage, LearnMorePage (stub), themeConfig
chat/       ChatPage, composer, message bubbles
store/      CartContext (localStorage, codes + quantities only), StorePage, CartPage, OrderPage
admin/      RequireAdmin, AdminLayout, survey list + version editor (preview mode included), product
            CRUD, fulfillment queue, blog authoring, the contact-message inbox
account/    MyAccountPage — profile, order history (paginated), the email-chain opt-in checkbox
blog/       BlogPage (list, paginated), BlogPostPage (detail by slug) — reads any signed-in user gets
contact/    ContactPage — the message form; any signed-in user can send one
content/    AboutPage — real copy, no backend
lib/        apiClient (attaches Supabase bearer token, throws ApiError), queryClient, supabaseClient
types/      api.ts — hand-maintained mirror of the backend DTOs
```

`AppShell` carries two rows: the brand bar, then a dark nav bar of
HOME | ABOUT | CONTACT | BLOG | STORE | MY ACCOUNT separated by white rules, with a cart count on
STORE and an Admin link that appears only for `role === 'ADMIN'`.

**The survey editor shows an author no JSON and no identifiers.** It is aimed at someone
non-technical, so:

- `config` is split into the only keys the renderer reads — `min`/`max` (SCALE, NUMBER), `unit`
  (NUMBER), `maxLength` (TEXT, LONG_TEXT), `scaleLabels` end labels — each a labelled field.
- A `displayRule` is built with dropdowns ("only show this when *Do you smoke?* **is** *Yes*"),
  offering exactly the `{"all": [...]}` / eq-ne-in shape `DisplayRuleEvaluator` understands. A rule
  in any other shape is shown as unsupported and written back verbatim rather than dropped.
- Option `metadata.signals` is a comma-separated field, tucked behind **Advanced** with the
  per-option scores, since neither matters until the scoring engine exists.
- Anything the editor does not model is preserved in `configExtra` / `metadataExtra` and written
  back untouched, so editing never silently discards a key.

**Codes are derived, never typed.** A new section, question, or option takes its code from
`slugify()` of its own wording, de-duplicated by `assignCodes`; an item that already exists
server-side has `codeLocked` and keeps the code it was published with however it is reworded —
which is the whole point, since answers join on `question.code` across versions. The code is shown
read-only under **Advanced**. Display-rule conditions therefore reference a question's *editor id*,
not its code, and resolve to a code at save time — storing the code would break the rule the moment
someone reworded the question it points at.

`sortOrder` is positional, so the on-screen order is the order. The question-type dropdown is built
from `QUESTION_COMPONENTS`, so the renderer decides what the editor may offer.

`ProtectedLayout` composes `RequireAuth → RequireOnboarding → AppShell → <Outlet/>`, so every
route inside it is authenticated *and* past onboarding. `/onboarding` sits outside it (auth only).

The survey renderer is registry-driven: adding a `QuestionType` means adding a component under
`surveys/renderer/questions/` and one entry in `questionRegistry.ts`.

**Preview reuses the real renderer, not a mock of it.** The editor's "Preview" toggle runs the
current draft through `toSectionDtos` — the exact function `Save draft` sends to the server — then
adapts that into `SurveyRenderer`'s own prop shape via `surveyPreviewAdapter.ts` and renders it
read-only (`onSubmit` is a no-op). A non-technical author sees precisely what a user would see,
required-question and display-rule behavior included, without publishing anything.

**At most one active survey per theme**, enforced at both layers: a partial unique index
(`ux_survey_active_theme`, `V10`) and a pre-check in `AdminSurveyService` on create and on
reactivating a retired survey. Two active surveys sharing a theme is exactly the bug that let a
draft-only survey silently eclipse a real one on the home page — the API returned both, and the
frontend's per-theme lookup picked whichever sorted first. Creating a brand-new survey is
deliberately not exposed in the admin UI right now (`AdminSurveysPage` only offers "New draft" on
an existing survey) — there is nowhere on the home page for a sixth theme to go yet, and no way to
get a second one, other than a manual API/DB action.

Dev requests go to `/api/v1` and Vite proxies `/api` → `localhost:8080`. Splitting the deploy
across Vercel/Railway means switching `BASE_URL` in `lib/apiClient.ts` to
`import.meta.env.VITE_API_BASE_URL` (there's a `TODO(prod)` marking the spot).

Design tokens live in `index.css` under Tailwind v4's `@theme`: magenta brand ramp, `ink` /
`ink-muted` text, `surface` / `surface-subtle` / `surface-border`, Inter Variable.

---

## Running it

```bash
# One-time: copy the local config templates
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
cp frontend/.env.local.example frontend/.env.local

supabase start                                              # local Postgres :54322 + GoTrue :54321
./mvnw spring-boot:run -Dspring-boot.run.profiles=local      # backend :8080
cd frontend && npm install && npm run dev                    # frontend :5173

./mvnw test          # Testcontainers Postgres, no local DB needed
cd frontend && npm run build && npm run lint
```

Auth is fully local — sign up through the SPA (or `POST /auth/v1/signup` against
`127.0.0.1:54321`) to get a real, JWKS-verifiable JWT for curl/Postman. There is no UI for
granting admin yet, so promote yourself in SQL against the local DB:
`update app.app_user set role = 'ADMIN' where email = '…';`

API docs at `http://localhost:8080/docs`.

The store runs with no Stripe account: `bellavera.store.payment-provider=mock` hands back a
success-URL redirect and accepts an unsigned webhook in the normalized shape, so checkout →
paid → fulfilled is exercisable offline. To drive real Stripe locally, set
`bellavera.store.stripe.secret-key` / `webhook-secret`, flip the provider to `stripe`, and forward
events:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

### Profiles

- `local` — Supabase CLI stack, `application-local.properties` (gitignored)
- `test` — Testcontainers Postgres via `@DynamicPropertySource`; a dummy JWKS URI exists only so
  the resource-server DSL can start, since `MockMvc`'s `jwt()` post-processor injects the security
  context directly
- `prod` — every value from an env var; see `.env.example`

### CI

`.github/workflows/ci.yml` runs on every push and PR against `master`: `./mvnw test` (backend,
needs Docker on the runner for Testcontainers — GitHub's `ubuntu-latest` ships one) and
`npm ci && npm run build && npm run lint` (frontend). No secrets are configured or needed — the
test profile always uses the mock LLM/payment/email adapters regardless of what `prod` is
configured to use, so wiring a real provider later has zero effect on what CI runs.

---

## API surface

| Method | Path | Notes |
|---|---|---|
| GET / PATCH | `/api/v1/me` | Profile; PATCH also stamps `consent_terms_at` / `consent_ai_at` |
| GET | `/api/v1/surveys/active` | All active surveys + a per-user `completed` flag |
| GET | `/api/v1/surveys/{id}` | Published version detail: sections, questions, options |
| POST | `/api/v1/surveys/{id}/responses` | Upsert draft or submit |
| GET | `/api/v1/surveys/{id}/responses/me` | Submitted response, else the draft, else 404 |
| GET | `/api/v1/insights/me` | Empty until the scoring engine lands (Stage 9) |
| POST | `/api/v1/chat` | One turn; creates the thread when `threadId` is null |
| GET | `/api/v1/chat/threads` | Thread list |
| GET | `/api/v1/chat/threads/{id}` | Full message history |
| GET | `/api/v1/store/products` | Public catalog — active products only |
| GET | `/api/v1/store/products/{code}` | Public; 404 for an inactive product |
| POST | `/api/v1/store/checkout` | Codes + quantities in; `{orderId, checkoutUrl}` out |
| GET | `/api/v1/store/orders/me` | The caller's orders, newest first, paginated (`?page=&size=`) |
| GET | `/api/v1/store/orders/{id}` | The caller's own order; 404 for anyone else's |
| POST | `/api/v1/webhooks/stripe` | Unauthenticated; the raw body must pass signature verification |
| GET | `/api/v1/blog` | Published posts only, paginated, newest first |
| GET | `/api/v1/blog/{slug}` | 404 unless published |
| POST | `/api/v1/contact` | Sends a message to the admin inbox as the caller |
| GET / POST | `/api/v1/admin/surveys` | Every survey incl. retired; create one plus its v1 draft — rejected if the theme is already active elsewhere |
| GET / PATCH | `/api/v1/admin/surveys/{id}` | `{"active": false}` retires it; `{"active": true}` is rejected if another active survey already holds the theme |
| POST | `/api/v1/admin/surveys/{id}/versions` | Opens a draft cloned from the newest version |
| GET / PUT / DELETE | `/api/v1/admin/surveys/{id}/versions/{vid}` | Read; whole-document replace (draft only); delete (draft only) |
| POST | `/api/v1/admin/surveys/{id}/versions/{vid}/publish` | Archives the version it replaces |
| GET / POST | `/api/v1/admin/products` | Catalog incl. inactive; create |
| PATCH / DELETE | `/api/v1/admin/products/{id}` | Update; DELETE deactivates rather than destroys |
| GET | `/api/v1/admin/orders` | Order history, newest first, paginated; `?status=PAID` is the packing queue (oldest first), `?status=FULFILLED` what has shipped |
| GET | `/api/v1/admin/orders/{id}` | One order in full, whatever its status |
| POST | `/api/v1/admin/orders/{id}/fulfill` | Mark shipped; 409 unless the order is `PAID` |
| GET / PATCH | `/api/v1/admin/users` / `/api/v1/admin/users/{id}` | List / ban / reinstate |
| POST | `/api/v1/admin/emails/broadcast` | Sends `{subject, body}` to every account with the email-chain opt-in set |
| GET / POST | `/api/v1/admin/blog` | Every post incl. drafts, paginated; create a draft (slug derived from the title) |
| PATCH / DELETE | `/api/v1/admin/blog/{id}` | Update or toggle published (in place — no versioning); DELETE really deletes, nothing else references a post |
| GET | `/api/v1/admin/contact-messages` | The inbox, paginated, newest first |
| PATCH | `/api/v1/admin/contact-messages/{id}/read` | Marks one read |

Public: `/actuator/health`, `/docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `GET` on the two
catalog paths, and `POST /api/v1/webhooks/stripe`. `/api/v1/admin/**` requires `ROLE_ADMIN`.
Everything else — including the blog and contact endpoints — requires a valid JWT: "any signed-in
user" for those, not a public signed-out visitor. There is still no public storefront or public
blog; see the deploy-stage note on that decision.

---

## Constraints

**Keep the LLM harness provider-free.** Do not wire Anthropic — or any concrete provider — into
`llm/` unless explicitly asked. The interface plus `MockLlmChatService` is the intended state;
`LlmConfig` throws for any provider name other than `mock`, which is deliberate.

**Never edit an applied Flyway migration.** Add a new versioned file.

**The V6 seed content is placeholder, not clinical.** Survey copy in `V6__seed_surveys.sql` was
written for scaffolding. Real content supersedes it via a new migration, never by editing V6.

**Do not invent scoring rules or survey content.** The insight engine stays unbuilt until the
bands, thresholds, and copy are handed over as a decision, and the survey authoring tools stay
empty of real questions until reviewed clinical content exists. Both would otherwise put
health-adjacent output nobody signed off on in front of a user.

**The assistant must not diagnose.** `src/main/resources/prompts/system-prompt-v1.txt` forbids
diagnosis, dosage, and claims of medical certainty, requires treating answers as self-reported,
and requires escalating red-flag symptoms to in-person care. Preserve those guardrails in any
prompt revision.

**Money is server-side only.** Prices come from our `product` rows (or Stripe price IDs), never
from the client; a checkout request names products and quantities, nothing more. Card data never
touches our servers or database — Stripe holds it. Webhooks are only trusted after signature
verification, and every Stripe secret comes from an env var. `MockPaymentGateway` verifies nothing
and takes nothing — `PaymentConfig` refuses it under the `prod` profile, and that check should stay.

**Admin writes are audited.** Anything mutating behind `/api/v1/admin/**` writes an audit row —
that's what the `V5` audit table is for.

**Keep `frontend/src/types/api.ts` in sync** with backend DTOs — it's hand-maintained, and
nothing will catch drift for you. Note that `spring.jackson.default-property-inclusion=non_null`
means a null field is *absent* from the JSON rather than null, so a `| null` type is really
"may be missing": normalize with `?? null` before comparing. `x === null` is false for an absent
field, which is how an untracked product first rendered as "undefined in stock".

**`ddl-auto=validate`.** Entities must match the migrations or the app won't start. That's the
intended safety net, not an obstacle.

---

## Stage 8 notes — real survey content (the next piece of work)

The tooling is built and holds nothing. Content arrives one of two ways:

- **Through the editor.** Admin console → Surveys → New draft on the existing seeded survey for
  that theme, fill it in, publish. (Creating a wholesale new survey isn't exposed in the UI right
  now — see the theme-uniqueness note above; content replaces the seeded survey's version, it
  doesn't get its own new survey row.)
- **As a migration.** A `V13__` file (next available) writing `survey` / `survey_version` /
  `survey_section` / `question` / `question_option` rows, for content that should exist in every
  environment from a clean database. The V6 placeholder copy is superseded by whichever route you
  pick — never by editing V6.

Two things to settle before writing any of it:

- **What replaces the V6 placeholders.** The seeded onboarding and four themed surveys are
  scaffolding copy. Real content either supersedes them by publishing a new version, or the V6
  surveys get retired and replaced outright.
- **The option `metadata` signals.** Scoring rules will consume them
  (`{"signals":["IRON_SUPPLEMENTED"]}` and friends). Whatever vocabulary the content uses is the
  vocabulary the Stage 9 engine has to speak, so it is worth agreeing on before the copy is final.

## What landed in Stages 5–7

**Admin console.** `/api/v1/admin/**` was already gated on `ROLE_ADMIN`; it now has controllers.
Survey authoring is whole-document replace on a draft, validated in one pass so the editor gets
every problem at once: duplicate section or question codes, a choice question with no options, a
`SCALE` with no `min`/`max` for `AnswerValidator` to enforce, a display rule naming a question that
is not in the version (it could never be satisfied, so the question would never appear) or using an
operator `DisplayRuleEvaluator` does not understand.

**Store.** `V7__schema_store.sql` adds `product`, `customer_order`, `order_item`, and
`payment_event`; `V8__product_stock.sql` adds the nullable `product.stock_quantity`. Stripe
Checkout is hosted, so no card data reaches this application. Checkout
merges repeated product codes into one line, because the DB enforces one line per product per order
and sending a product twice means "two of them".

**Fulfillment.** The console has three views over `GET /api/v1/admin/orders`: **To ship**
(`?status=PAID`, oldest first — the order they should be packed in), **Shipped**
(`?status=FULFILLED`), and **All orders** (no filter). Fulfilling stamps the columns, draws stock
down, writes an audit row recording the drawdown, and moves the order from the first view to the
second.

**Adding partial fulfillment later** means a `shipment` table backfilled one row per fulfilled order
from those columns. What would make that painful is code testing the raw columns, so read
fulfillment state through `OrderStatus` — never `trackingNumber != null` at a call site.

## What landed after Stage 7 (pre-Stage-8 hardening + Blog/Contact)

Done while waiting on real survey content and the scoring rules — none of it needed either.

**Order confirmation and shipped emails.** `OrderPaidEvent` / `OrderFulfilledEvent` publish from
`PaymentEventApplier` / `FulfillmentService` at the exact moment each transition happens.
`OrderEmailListener` consumes them `AFTER_COMMIT` (in its own `REQUIRES_NEW` transaction — Spring
refuses any other propagation on a post-commit listener) so an email for a payment or fulfillment
that then rolled back is never sent, and it swallows `EmailGateway` failures so a broken provider
can't turn a successful webhook delivery or fulfillment request into an error response.

**`DisplayRuleEvaluator` fails closed.** An unrecognized display-rule operator used to default to
"condition satisfied" (question shows/required unconditionally); it now defaults to unmet (question
stays hidden), logged as a warning. Matters most for the migration-authored survey-content path,
which skips the admin editor's own validation. Fixed on both the backend and its frontend mirror
(`displayRuleEngine.ts`).

**Pagination.** Every list that grows without bound returns `PageResponse<T>`
(`common/PageResponse.java`) rather than a bare list — `page`/`size` query params, clamped
server-side: `/store/orders/me`, `/blog`, `/admin/blog`, `/admin/contact-messages`, and
`/admin/orders`.

**Fetching a paged list without an N+1 - and without the collection-fetch trap.** The admin list
DTOs read across a lazy relation (`AdminOrderDto` → `order.getUser()`, `AdminBlogPostDto` →
`post.getAuthor()`, `AdminContactMessageDto` → `message.getUser()`), which left alone costs one
query per row. Each is fixed with `@EntityGraph` on the paged repository method — but only for
*to-one* relations. `CustomerOrder.items` is a collection, and fetch-joining a collection alongside
`firstResult`/`maxResults` makes Hibernate load every matching row and paginate in memory
(`HHH90003004`), which is far worse than the N+1 it would fix. That one uses `@BatchSize` on the
collection instead, so a page's lines load in one extra query and pagination stays in SQL. If you
add a list here, follow the same split.

**Survey preview and the theme-uniqueness guard** — see the Frontend section above for both; they
are documented there rather than repeated here.

**Blog.** `V11__schema_blog.sql`. No versioning like a survey needs — nothing else references a
post, so editing a published one changes it in place, and deleting one is a real delete rather than
a deactivation. The slug is derived from the title server-side (`SlugGenerator`, dedup by suffixing
`-2`, `-3`, ...) and never changes afterward. Any signed-in user can read published posts; only
`ROLE_ADMIN` can write. Text only — no image field. Deliberately deferred: adding one means deciding
between an `imageUrl` string (matches how `Product.imageUrl` already works, zero new infra) or a
real upload (needs Supabase Storage, which local dev currently excludes since nothing uses it).

**Contact.** `V12__schema_contact.sql`. A message is tied to the sender's account (no free-text
name/email to fake), read by any `ROLE_ADMIN` via the inbox, marked read individually. No spam
protection beyond requiring authentication, since the whole app already does.

## Deferred: the insight scoring engine

Blocked on the business/clinical rules, not on code. The seams are already cut, so when the
rules arrive:

- `SurveyResponseSubmittedEvent` is published on submit and carries `userId`, `responseId`,
  `surveyId`, `surveyCode`. `OnboardingCompletionListener` is the existing example of consuming it.
- `insight_run` records `triggered_by` (`SURVEY_SUBMIT` / `MANUAL` / `RECOMPUTE`),
  `engine_version`, and an `input_fingerprint` — intended for skipping recomputation when the
  inputs haven't changed, mirroring how `UserContextBuilder` hashes its context.
- `insight` rows carry `domain`, `code`, `label`, `score`, `band` (`LOW`/`MODERATE`/`HIGH`/
  `UNKNOWN`), `confidence`, `rationale`, and an `evidence` JSONB for the answers that drove it.
- `insight_rule` holds tunable thresholds and copy in a `definition` JSONB; the rule logic itself
  is meant to be a Java bean, not data.
- Seeded option metadata already emits signals for rules to consume — e.g.
  `{"signals":["IRON_SUPPLEMENTED"]}`, `["FATIGUE"]`, `["PELVIC_LEAKAGE"]`, `["MOOD_SWINGS"]`.
- `InsightQueryService` and `GET /insights/me` are written and will start returning data the
  moment runs exist. `UserContextBuilder` already folds insights into the chat context.
