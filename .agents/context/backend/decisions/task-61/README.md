# Decision register: task #61 (Web2 sources GitHub and Bitcointalk; Reddit rejected)

IDs are `D61-NNN`. "Proposed" means the implementer's proposal; nothing here is an approval by the team or the mentors.

| ID | Topic | Status |
|---|---|---|
| D61-001 | Access matrix: every live source stays off until #38 approves it | proposed, **blocking live** |
| D61-002 | `NOT_CHECKED` source status (`SourceNotCheckedException`) | proposed, needs the orchestration owner |
| D61-003 | Relevance by provenance (author / Case profile URL) in `SearchService` | proposed, needs the E2 owner |
| D61-004 | `author` on RawMention, Mention, FoundMention and `MentionResponse` | proposed |
| D61-005 | Scoring (D9, `confidence-1`) untouched; known gaps below | open, needs a D9 decision |

## D61-001 Access matrix

The mentors approved GitHub and Bitcointalk (stated by the team, not recorded in #38). **Reddit was rejected by the team and its adapter was removed from this task** (app registration failed with "Max clients have been reached"). Each source is a bean only with `sources.<name>.enabled=true` (default `false`). Enabling is a deliberate switch. A comparison project having a connector is not an approval.

| Source | Data that leaves the application | Method | Credentials | Status |
|---|---|---|---|---|
| github | the Case login/nickname or the login in a GitHub profile URL | REST API `GET /users/{login}`, `GET /repos/{login}/{login}/readme` (raw media type, same host) | optional `SOURCES_GITHUB_TOKEN` (raises the 60 requests/hour anonymous limit) | approved by mentors (team statement) |
| bitcointalk | nothing about the person: only the URL the Analyst gave is fetched | `GET index.php?action=profile;u=ID` or `?topic=ID.msgN`, HTML parsed with jsoup, 2 s between requests | none | approved by mentors (team statement) |

## D61-002 `NOT_CHECKED`

`SourceStatus` gets a third value. A source throws `SourceNotCheckedException` when the Case has no identifier it supports ; `SearchService` reports it as `NOT_CHECKED` with no check time. Any other exception (HTTP 401/403/429/5xx, timeout, unreadable answer) is `UNAVAILABLE` as before. `AnalysisOrchestrator`: a `NOT_CHECKED` source is no failure; an Analysis with no `CHECKED` source fails (`NO_SOURCE_CHECKED`, or `ALL_SOURCES_UNAVAILABLE` when one was unavailable). A disabled source is not part of the source set and is not reported.

## D61-003 Relevance by provenance

The content filter needs the identifier in the text. A profile README rarely names its author. `SearchService` therefore also accepts a `RawMention` when its `author` equals the Case login/nickname, or its URL equals/is under the Case profile URL, **and only when the content holds an address** (the snippet is centred on it). Nothing is appended to the fetched text. A Bitcointalk thread post is relevant only through its own author or its own post URL; other authors' posts and quoted text are dropped.

## D61-005 Known gaps (decision for D9, a scoring change needs a new algorithm version)

- Bitcointalk post URLs do not contain the nickname, and `confidence-1` looks only at URL and snippet, so such a Mention gets `NO_PERSON_LINK` (0%) although the author matches. Linking by `author` needs a D9 rule.
- API profile and README of one GitHub account share the host `github.com`, so they are `SAME_SERVICE`, never independent (+15).
- Several far-apart addresses of one README share one 200-character context; only the first address is centred.
- A Bitcointalk URL with extra parameters (`;sa=summary`) is not "under" the profile URL for the relevance filter.
