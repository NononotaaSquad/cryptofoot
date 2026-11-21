# Backend E2E tests

Black-box tests of the backend REST API (`/api/1`) against a running backend, as a **Postman** collection and an identical **Bruno** collection. Both hold the same 56 requests in the same order, with the same assertions.

```
.e2e/backend/
  postman/cryptofoot-backend.postman_collection.json   Postman collection (v2.1)
  postman/local.postman_environment.json               baseUrl = http://localhost:8080
  bruno/                                                Bruno collection (open this folder in Bruno)
  bruno/environments/local.bru                          baseUrl = http://localhost:8080
```

## Run

Start the backend (MongoDB + backend, profile `local`), then run one or both suites:

```bash
./scripts/run-backend.sh                  # or: docker compose -f backend/docker-compose.yaml up -d --build
./scripts/e2e-backend.sh                  # both suites; `postman` or `bruno` runs one
BASE_URL=http://localhost:8081 ./scripts/e2e-backend.sh bruno
```

The script uses `npx` with pinned versions (`newman@6.2.3`, `@usebruno/cli@4.3.0`), so nothing is installed in the repo. Without the script:

```bash
npx newman@6.2.3 run .e2e/backend/postman/cryptofoot-backend.postman_collection.json -e .e2e/backend/postman/local.postman_environment.json
cd .e2e/backend/bruno && npx @usebruno/cli@4.3.0 run --env local
```

In the apps: **Postman** → Import both JSON files, select the `cryptofoot local` environment, run the collection. **Bruno** → Open Collection → `.e2e/backend/bruno`, select the `local` environment, run the collection.

Run the whole collection in order: later folders use the ids that earlier ones save (`caseId`, `analysisId`, ...). A single request run alone fails when its variables are not set yet.

## What is covered

| Folder | Checks |
|---|---|
| 01 Health | `/actuator/health`, liveness and readiness are `UP` |
| 02 Case validation | No identifier, full name alone, blank values, malformed / bad-checksum / unsupported wallet, invalid e-mail, malformed JSON → 400 with the Polish message on the right field |
| 03 Case | Create from nickname, wallet only (trimmed, canonical lowercase), full set (trimmed, blank dropped, bech32 lowercased); no deduplication; get, list (newest first), unknown → 404 |
| 04 Analysis | Start (202, `MANUAL`), wait until finished, `PARTIAL` with source coverage (`synthetic` checked with 3 Mentions, `synthetic-broken` unavailable), report: order by Confidence Level, BTC 75 and bech32 60 with rules and Polish Justification, each ETH address either excluded as `SMART_CONTRACT` or scored, synthetic Address Activity of the demo addresses (totals, cash-out signals in time order) and `UNAVAILABLE` never shown as zero, limitations; Mentions with provenance and validated addresses; unknown Analysis / Case → 404 |
| 05 Rerun and comparison | A rerun is a new Analysis, the earlier report is unchanged, history newest first, Case shows the latest Analysis, comparison (`PARTIAL_ANALYSIS` limitation, no change on BTC), reversed order → 400, unknown → 404 |
| 06 Analysis edge cases | The Analyst's wallet is input, not evidence (every Attribution 0%, `ENTERED_BY_ANALYST` + `NO_PERSON_LINK`, Address Activity `NOT_CHECKED`); analyses of different Cases → 400; an identifier nobody mentions finishes with no results (not a failure): empty report and Mentions |
| 07 Schedule | Off by default, enable → `DAILY` 06:00 Europe/Warsaw, repeat keeps the next execution, `WEEKLY` → Monday 06:00, disable keeps the cadence, unknown cadence → 400 and changes nothing, unknown Case → 404. The suite leaves the schedule disabled |
| 08 API errors and CORS | Unknown route 404, wrong method 405 with `Allow`, API version 2 → 400, `text/plain` body → 415, `Access-Control-Allow-Origin: *` |

## Notes

- **Synthetic data.** The Analyses run over the `demo-v1` fixture (`backend/src/main/resources/fixtures/demo-v1.json`); the `synthetic-broken` source always fails, so every Analysis ends `PARTIAL`.
- **On-chain check.** The ETH bytecode check calls public services (`app.exclusion.onchain-check-enabled`), so each ETH address ends in exactly one of three outcomes, and the suite accepts each one with exact details: an Excluded Address `SMART_CONTRACT` (code found), an Attribution flagged `UNAVAILABLE` with the extra rule `CLASSIFICATION_UNAVAILABLE` and its Justification sentence (check failed or disabled), or a `NOT_EXCLUDED` Attribution (no code). On mainnet both demo ETH addresses, including the person's `0x5aAeb6…BeAed`, have contract code, so a backend with internet access excludes both. The Spock specs stub that address as a private account. BTC results are always exact.
- **Waiting for an Analysis.** The `Wait for ...` requests poll `GET /api/1/analyses/{id}` in their pre-request script until the status leaves `RUNNING` (at most 60 s).
- **Not covered here.** Races (a second start while an Analysis runs → 409, a report while running → 409) and the demo cadence (`DEMO_EVERY_MINUTE`, on only in the `local` profile) depend on timing or configuration; the Spock integration specs in `backend/src/integrationTest` cover them.
- **Data.** Each run creates Cases and Analyses in the target database and nothing is deleted (there is no delete endpoint). Run it against a local or test backend.
- **Keep both suites in sync.** A new or changed request goes into both collections, in the same folder and position.
