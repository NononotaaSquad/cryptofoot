# ADR-0006: Input for the Confidence Level Algorithm

**Status:** Proposed (shape to be agreed with the owner of "E3: Confidence Level algorithm")

**Context:**
The Confidence Level algorithm needs every distinct address once, with all Mentions it appeared in, and the same set of Mentions must always give the same Confidence Level (E3). Malformed candidates must fail visibly and never become Attributions (#21). E2 owns detection in content; E3 owns scoring.

**Decision:**
- `AddressCandidateService.prepare(List<MentionAddresses>)` is the boundary. `MentionAddresses(mentionId, detectedAddresses)` is the E2 output it consumes.
- It returns `AddressCandidates(valid, rejected)`:
  - `valid`: one `AddressCandidate(address: CanonicalAddress, mentionIds)` per canonical address, grouping all Mentions with that address.
  - `rejected`: one `RejectedCandidate(mentionId, value, network?, reason)` per failed candidate, with the original text, so the rejection is visible to the Analyst and in logs.
- Determinism: Mention IDs are de-duplicated and sorted; candidates are sorted by network, then address. Input order does not change the output.
- Candidates carry Mention references only, not Mention content. The algorithm looks up source details (e.g. for the independent-sources bonus) by Mention ID.
- A plain Spring `@Service` with in-memory value types; nothing is persisted or exposed over HTTP at this step.

**Consequences:**
- **Positive:** Clear boundary between detection (E2), validation (this step) and scoring; the output is the Justification's raw material (Mention references) and is reproducible.
- **Negative:** The scorer needs a Mention lookup to read sources; `MentionAddresses` may need to adapt once E2's Mention model exists.
