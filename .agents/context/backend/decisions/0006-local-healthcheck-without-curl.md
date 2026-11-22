# 0006. Local Docker healthcheck without curl

**Status:** Accepted (owner: Maksym U., 2026-11-22)

## Context

`scripts/run-all.sh` starts MongoDB and the backend with `docker compose up --build -d --wait` and starts the frontend only after that command succeeds (`set -e`). The backend healthcheck in `backend/docker-compose.yaml` called `curl`, but the runtime image `eclipse-temurin:25-jre` has neither curl nor wget. The check never passed, the backend container ended `unhealthy`, `--wait` failed and the script stopped before `npm run dev`, so the frontend never started. `run-backend.sh` does not wait for health, which hid the problem. Nothing else uses this healthcheck: CI and the E2E suites do not run `up --wait`, and Cloud Run ignores Docker healthchecks.

## Decision

- The healthcheck uses `bash` from the image: it opens `/dev/tcp/127.0.0.1/8080`, sends `GET /actuator/health` (HTTP/1.0) and is healthy only when the status line is `200`. This is the same rule as `curl -f`: `503` (health `DOWN`) and a refused connection fail.
- The production image stays unchanged. curl or wget is not installed just for a local check.
- When `up --wait` fails, `run-all.sh` says why the frontend was not started (most often port 8080 taken by a backend started from the IDE), points to the backend logs and exits with 1.

## Consequences

- `./scripts/run-all.sh` starts MongoDB, the backend and the frontend with one command again.
- The check needs `bash`, `head` and `grep` in the runtime image (present in the Ubuntu-based Temurin JRE). Moving to an image without bash (Alpine, distroless) needs a new healthcheck.
- A port taken on the host still stops the script, now with a message instead of a silent missing frontend.

## Verification

- `docker compose -f backend/docker-compose.yaml config` shows the test command with a literal `\r\n`, which `printf` turns into CRLF.
- In the backend image with nothing listening on 8080 the check exits 1 (`Connection refused`).
- Isolated stack (`docker compose -p olcys-hc-test` with ports overridden to avoid the running services): `up --build -d --wait` exited 0 after 21 s with `health=healthy`. A copy of `run-all.sh` pointed at that stack printed `Starting frontend...`, Next.js reported `Ready`, and `GET /cases` on port 3000 returned 200.
- `./scripts/run-all.sh` with port 8080 taken by a backend from the IDE exits 1 with the new message, and the frontend is not started.
