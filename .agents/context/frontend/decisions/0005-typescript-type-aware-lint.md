# ADR-0005: TypeScript 5 with Type-Aware Lint

**Status:** Accepted

**Context:**
Most code is written by AI agents. Typical mistakes are floating promises, `any` and unchecked index access.

**Decision:**
TypeScript 5.9 in strict mode with `noUncheckedIndexedAccess` (shared config in `packages/typescript-config`). ESLint 10 with typescript-eslint strict type-checked rules; every violation is an error (`eslint-plugin-only-warn` removed). Stay on TypeScript 5: typescript-eslint 8.72 declares peer `typescript <6.1.0` and does not support TypeScript 7.

**Consequences:**
- **Positive:** Type-aware rules catch AI mistakes before review
- **Negative:** Type-aware lint is slower; revisit TypeScript 7 when typescript-eslint supports it
