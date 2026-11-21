# ADR-0009: Feature Folders

**Status:** Accepted

**Context:**
Two people and two agents edit the frontend in parallel; shared folders cause conflicts.

**Decision:**
Each feature lives in `apps/web/features/<feature-name>/` (views, hooks, pure logic, tests). `app/` holds routing files only. `lib/` holds cross-feature code. `features/backend-status/` is the reference feature: the smallest path through the generated client (see ADR-0007).

**Consequences:**
- **Positive:** Parallel work rarely touches the same files
- **Negative:** Shared pieces must be moved to `lib/` or `packages/ui` deliberately
