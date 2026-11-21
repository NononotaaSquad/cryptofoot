# ADR-0010: Test Scope

**Status:** Accepted

**Context:**
24 hours; tests must pay for themselves.

**Decision:**
Vitest tests only for pure logic that carries invariants or that the product depends on (calculations, validation, mapping API responses), co-located as `*.test.ts` under `apps/web/features/` or `apps/web/lib/` (the only folders Vitest runs). No component tests, no E2E. E2E (Playwright) was considered and deliberately not included; add it back when there are specs worth running.

**Consequences:**
- **Positive:** Fast suite inside `npm run check`
- **Negative:** UI regressions are caught only by browser verification
