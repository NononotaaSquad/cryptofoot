# Live checks, task #61

Date: 2026-11-21. Base commit: 044dfbd (the #61 changes were uncommitted in the working tree). Method: backend `bootRun` with MongoDB, `SOURCES_SYNTHETIC_ENABLED=false`, `SOURCES_GITHUB_ENABLED=true`, `SOURCES_BITCOINTALK_ENABLED=true`, default timeouts and limits; no tokens. One Case and one manual Analysis per resource over REST. The mentors approved the services (stated by the team).

## GitHub: done, with a limitation

- Resource: login `BohdanMoldavets` (a team member's own public profile), anonymous REST API.
- Result: `github` CHECKED, 0 Mentions. Profile fetched (name, company, location, bio); the profile README `BohdanMoldavets/BohdanMoldavets` does not exist (HTTP 404, logged, profile kept). The public text holds no cryptocurrency address, so no Mention was stored.
- Limitation: this check proves access, parsing and the valid-zero path live. It does not prove address detection on live GitHub material (covered only by the controlled-HTTP specs).

- Second resource: login `maksymuimanov`, same method. `github` CHECKED, 2 Mentions: the profile (`https://github.com/maksymuimanov`) and the profile README (`https://github.com/maksymuimanov/maksymuimanov`, 200-character context), both with author `maksymuimanov` and no detected address, so no Attribution and no scoring. The Mentions were kept because the login occurs in the fetched text (the existing relevance rule). This is the first live check with both a profile and a README present; address detection on live GitHub material is still unproven.

## Bitcointalk: done

- Resource: `https://bitcointalk.org/index.php?action=profile;u=113670`.
- Result: `bitcointalk` CHECKED, 1 Mention, author `Mitchell`, URL equal to the profile URL, detected `bc1qgptmcqk3sp6f9fdtkvf79rr8hcp4tdn7k0ae02` (BTC, valid) from the "Bitcoin address" field; the signature was read as well. Attribution count 1, algorithm `confidence-1`.
- Limitations: the profile page only; no thread or post was checked live; the HTML parser matched this one page.

## Reddit: rejected

The team dropped Reddit from the scope of #61 (no credentials available). There is no Reddit adapter and no Reddit live check. The Analyses above were run when the adapter still existed and reported NOT_CHECKED for it.
