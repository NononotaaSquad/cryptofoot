# ADR-0001: Turborepo Monorepo

**Status:** Accepted

**Context:**
The web app, shared UI components and shared configs need one install and one task runner.

**Decision:**
Use Turborepo with npm workspaces: `apps/web`, `packages/ui`, `packages/eslint-config`, `packages/typescript-config`. `turbo.json` defines `build`, `lint`, `format`, `typecheck`, `test` and `dev`.

**Consequences:**
- **Positive:** One command runs a task in every package, with caching
- **Negative:** One more tool to understand
