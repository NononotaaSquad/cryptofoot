# ADR-0011: Direct Backend URL

**Status:** Open (owner: Maksym, team lead)

**Context:**
The browser must reach the backend; deployment target is Google Cloud.

**Decision:**
For now the browser calls the backend directly at `NEXT_PUBLIC_API_URL` (validated in `lib/env.ts`, which also holds the dev-only default; the backend allows CORS on `/api/**` only for origins in `CORS_ALLOWED_ORIGINS`, local default in `backend/src/main/resources/application.yaml`; the deploy job in `.github/workflows/ci.yml` sets it to the frontend URL). `NEXT_PUBLIC_*` values are inlined at `next build`, so the Docker image must be built with `--build-arg NEXT_PUBLIC_API_URL=...`; changing the container environment later has no effect. Alternative: a same-origin proxy through Next.js, which makes the URL a runtime setting.

**Consequences:**
- **Positive:** No proxy code, no extra hop
- **Negative:** One image per backend URL
