# Progress snapshot

Fast orientation doc — read this first, then `CLAUDE.md` for depth. Written 2026-09-10, after
commit `d824a58 adding messaging system and blog`. Regenerate/update this whenever a session does
substantial work, rather than letting it silently go stale.

## Where things actually stand

Everything through Stage 7 (auth, surveys, chat w/ mock LLM, store/checkout/fulfillment, admin
console) is built and tested. Beyond that, this round of work (post-Stage-7, pre-Stage-8) added:

- **Order lifecycle emails** — confirmation on paid, shipped on fulfillment. Event-driven
  (`OrderPaidEvent`/`OrderFulfilledEvent` → `OrderEmailListener`, `AFTER_COMMIT`), mock-only, tested
  with a real assertion (not just log-watching).
- **`DisplayRuleEvaluator` fail-closed fix** — an unrecognized display-rule operator used to default
  to "visible"; now defaults to hidden. Fixed on both backend and its frontend mirror.
- **Pagination** — `PageResponse<T>` pattern, applied to user order history, blog list, and the
  contact inbox, and (since the codebase-wide scan) `GET /api/v1/admin/orders`.
- **Survey theme-uniqueness guard** — `V10`, a partial unique index plus a service-level check.
  Fixes the real bug that happened: two active surveys sharing a theme, one with no published
  version, silently winning the home-page slot. "New survey" is hidden from the admin UI on
  purpose — only "New draft" on an existing survey is exposed right now.
- **Survey preview** — the admin editor can render the live draft through the real `SurveyRenderer`
  (read-only), so a non-technical author sees exactly what a user would see before publishing.
- **Blog** (`V11`) — full CMS. Admin creates/edits/publishes/deletes; any signed-in user reads
  published posts. Slug is server-derived from the title, deduped, permanent. **Text only — no
  image field**, deliberately deferred (see CLAUDE.md's Blog section for the two implementation
  paths and their tradeoffs).
- **Contact** (`V12`) — a working form tied to the sender's account, read via an admin inbox with
  mark-as-read.
- **CI** — `.github/workflows/ci.yml`, runs backend + frontend on every push/PR against `master`.
  No secrets needed or configured.
- **README.md** added (front door, points to CLAUDE.md for depth).

All of the above is committed and pushed. Full test suite green as of this snapshot.

## Open decisions nobody has made yet

These come up repeatedly and are worth resolving deliberately rather than by default:

1. **Public (signed-out) access.** Right now the *entire* app — including Blog and Contact, which
   sound public — requires a Supabase account. "Any signed-in user" was the explicit scope call for
   Blog/Contact this round, not "any visitor." Building a real public-facing shell is a bigger,
   separate decision (same one already flagged for the storefront).
2. **Refund/cancel flow** beyond an expired checkout — explicitly missing, explicitly not decided.
3. **Blog images** — URL field (fast, matches `Product.imageUrl`) vs. real upload (needs Supabase
   Storage, which local dev currently runs *without* since nothing used it before now).
4. **Deploy timing** — Railway/Vercel intentionally not started yet; the user is deliberately
   waiting until more is built out first.
5. **Real Stripe test-mode run** — the adapter code is complete and has never been exercised
   against the actual Stripe service, only the mock. Free to do locally, no deploy needed.

## Known gaps / tech debt (not urgent, just real)

- `SurveyQueryService.listActiveForUser` runs 1 + 2N queries on the home page (per survey: a
  published-version lookup and a submitted-response lookup). Left as-is deliberately: N is bounded
  at ~4 by the fixed theme model, so the fix would be churn for no measurable gain. Revisit only if
  themes ever become dynamic — at that point N stops being bounded and this becomes real.
- `ChatRateLimiter` is in-memory, single-instance only — fine today, a real limit the moment this
  ever runs on more than one Railway replica.
- No security review pass has been done yet (there's a `security-review` skill for this).
- Local Supabase runs the full default service set (~13 containers); this project only actually
  uses Postgres + GoTrue + Kong. A `supabase start --exclude ...` was suggested but is a per-run
  CLI flag, not something persisted anywhere in this repo — nobody has to remember it, but nobody
  automated it either.

## Where to look for more

- `CLAUDE.md` — architecture, every design decision and why, full API surface table, the staged
  plan, and the constraints (don't wire a real LLM provider, don't invent scoring rules, never edit
  an applied migration).
- `README.md` — quick start, status table, testing/CI commands.
- Git log — the user commits independently between sessions (often via IntelliJ), so `git log` and
  `git status` are more current than assuming nothing changed since a prior conversation.
