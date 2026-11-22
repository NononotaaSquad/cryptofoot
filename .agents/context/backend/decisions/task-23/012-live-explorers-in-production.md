# D23-012: Live explorers on in production

- **Status:** accepted (owner); mentor question 4 ("approved external services") still open
- **Date:** 2026-11-22
- **Type:** owner decision; changes the production default of D23-003
- **Source:** gap D23-009 A9; #70 (production runs without synthetic data, Web2 sources on)
- **Proposed by:** AI agent (Claude), after the owner's request
- **Approved by:** Maksym U., 2026-11-22 ("włącz aktywność adresu na produkcji")

## Problem and context
Since #70 the production deploy runs with `SOURCES_SYNTHETIC_ENABLED=false` and the real Web2 sources on. With the live explorers off (D23-003), no Attribution had Address Activity, and every production Analysis with Attributions was `PARTIAL` with `BLOCKCHAIN_DATA_UNAVAILABLE` (D23-005).

## Options considered
1. `app.address-activity.live-enabled: ${ADDRESS_ACTIVITY_LIVE_ENABLED:true}` in `application-prod.yaml`.
2. `ADDRESS_ACTIVITY_LIVE_ENABLED=true` in the `--set-env-vars` of the deploy job in `ci.yml`.
3. Keep production off and accept `PARTIAL`.

## Decision
Option 1. The prod profile reads Address Activity from mempool.space (BTC) and Blockscout (ETH) by default. `ADDRESS_ACTIVITY_LIVE_ENABLED=false` on the Cloud Run service switches it off without a code change. Local runs, tests and the default profile stay off. The setting lives next to the other production behaviour of the prod profile (`sources.github.require-token`), so it is tested with the real YAML files and does not depend on the deploy script.

## Consequences and risks
- Production sends the bare address of every Attribution with a Confidence Level above 0 to mempool.space or Blockscout (D23-002, D23-003). No Case data, identifiers or scores are sent. This is done before the mentors answer question 4. If they do not approve these services, set the variable to `false` or point `app.address-activity.mempool-url` / `blockscout-url` at self-hosted instances.
- A very busy address can exceed the 5 s timeout of Blockscout's token transfers (D23-009 A8) and stays `UNAVAILABLE`, which still makes that Analysis `PARTIAL`.
- Public instances may rate-limit; a refused request gives `UNAVAILABLE`, never zero activity.

## Verification
`AddressActivityProfilesSpec`: the prod profile enables the live explorers with the default URLs; `ADDRESS_ACTIVITY_LIVE_ENABLED=false` switches them off; the local and default profiles stay off. After the deploy, the backend log shows "Address Activity sources: [mempool.space …, Blockscout …]".

## Related
- Files: `resources/application-prod.yaml`. Decisions: D23-003, D23-005, D23-009 (A8, A9), D61-006.

## Reconsider when
Mentors answer question 4, or KAS hosts its own explorers.
