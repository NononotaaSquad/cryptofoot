# 0003. Synthetic demo data in production, without the always-unavailable source

**Status:** Accepted (owner: Maksym U., 2026-11-22) · Changes the production setting of #70

## Context

Since #70 the production deploy ran with `SOURCES_SYNTHETIC_ENABLED=false`. The fictional demo person `zenek_fx` exists only in the synthetic fixture, so on production a Case with that nickname checked no source at all and ended `FAILED` (`NO_SOURCE_CHECKED`): GitHub skips the nickname because `zenek_fx` is not a valid GitHub username, and Bitcointalk needs a profile or thread URL. The same switch also enables `synthetic-broken` (`SyntheticFailingMentionSource`), a source that always fails so the local demo can show the "source unavailable" scene. In production it would make every Analysis, including real ones, `PARTIAL` with `SOURCE_UNAVAILABLE`.

## Decision

- The production deploy (`.github/workflows/ci.yml`) sets `SOURCES_SYNTHETIC_ENABLED=true` and `SOURCES_SYNTHETIC_BROKEN_ENABLED=false`.
- `SyntheticFailingMentionSource` needs both `sources.synthetic.enabled` and the new `sources.synthetic-broken.enabled` (`application.yaml`: `${SOURCES_SYNTHETIC_BROKEN_ENABLED:true}`). Locally, in tests and in the E2E suites both stay on, so nothing changes there.

## Consequences

- The demo person works on production next to GitHub and Bitcointalk, with the synthetic Address Activity of the demo addresses (D23-007).
- Every production report lists the source `synthetic` with the label "Dane testowe". Its Mentions appear only for Cases whose identifiers match the demo person, because of the relevance filter of the search.
- The demo addresses are real mainnet addresses (`1A1zP1…` is the Bitcoin genesis address). If a real Case finds one of them, its Address Activity comes from the fixture (synthetic wins, D23-003) and is labelled "dane testowe", not read from the explorers.
- The "source unavailable" scene is shown only locally.

## Verification

`SyntheticSourcesSwitchSpec` loads the real `application.yaml`: the defaults give `synthetic` and `synthetic-broken`; the production deploy values give `synthetic` only; synthetic data off gives neither.
