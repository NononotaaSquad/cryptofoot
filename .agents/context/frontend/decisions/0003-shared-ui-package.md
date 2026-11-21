# ADR-0003: Shared UI Package

**Status:** Accepted

**Context:**
Components must look and behave the same across features built in parallel.

**Decision:**
`@workspace/ui` holds the vendored shadcn components. There is no build step: `package.json` `exports` point at TypeScript sources and `apps/web` compiles them via `transpilePackages`. Import as `@workspace/ui/components/button`, `@workspace/ui/lib/utils`. The style is `base-nova` on `@base-ui/react`, so the API differs from Radix-based shadcn (`render` instead of `asChild`, `data-open` instead of `data-[state=open]`). New components are added with the shadcn CLI (`npx shadcn@latest add <name> -c apps/web`). The package is shared by both frontend developers; changes are agreed first.

**Consequences:**
- **Positive:** Consistent design, no build step, source is editable
- **Negative:** Agents trained on Radix-based shadcn write wrong props; `.agents/rules/frontend.md` lists the differences
