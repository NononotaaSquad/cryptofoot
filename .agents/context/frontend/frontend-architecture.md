# Frontend Architecture

Context and rationale. The rules an agent follows are in `.agents/rules/frontend.md`.

Views and user paths: [views.md](views.md). Visual design from the KAS / Ministry of Finance materials: [kas-ui-ux/](kas-ui-ux/README.md).

## Technology Stack

Versions are major versions only. Exact ranges live in `package.json` files.

- **Framework:** Next.js 16 (App Router)
- **UI Library:** React 19
- **Language:** TypeScript 5 (strict, `noUncheckedIndexedAccess`)
- **Styling:** Tailwind CSS 4 (CSS-first, no `tailwind.config`)
- **Component Library:** shadcn/ui, style `base-nova`, built on `@base-ui/react` `^1.9.0` (not Radix)
- **Server state:** TanStack Query 5
- **API client:** Orval 8, generated from the backend OpenAPI document
- **Validation:** Zod 4
- **Notifications:** Sonner `^2.0.8`
- **Theming:** next-themes `^0.4.6`
- **Icons:** lucide-react `^1.66.0`
- **Animation:** Motion (`motion/react`) `^14.12.0` for interactive animations; `tw-animate-css` classes for simple transitions
- **Tests:** Vitest 5
- **Linting:** ESLint 10 with typescript-eslint strict type-checked rules
- **Formatting:** Prettier 3 with `prettier-plugin-tailwindcss`
- **Monorepo:** Turborepo 2
- **Package Manager:** npm `11.13.0`
- **Node Version:** >= `22.18`

## Commands

Run from `frontend/`:

- `npm run check` - typecheck, lint, format check and unit tests (the quality gate; CI runs it, then `build`)
- `npm run dev` - dev server on `http://localhost:3000`
- `npm run build` - production build
- `npm run format` - apply Prettier
- `npm run test` - unit tests
- `npm run api:gen` - regenerate the API client from `apps/web/lib/api/openapi.json`

## Project Structure

```
frontend/
├── apps/
│   └── web/
│       ├── app/                    # Routing files only
│       │   ├── layout.tsx          # <html lang="pl">, mounts providers
│       │   ├── providers.tsx       # TanStack Query, ThemeProvider, Sonner toaster
│       │   ├── page.tsx
│       │   ├── error.tsx
│       │   ├── global-error.tsx
│       │   ├── loading.tsx
│       │   └── not-found.tsx
│       ├── features/               # One folder per feature (backend-status/ is the reference)
│       ├── lib/
│       │   ├── api/
│       │   │   ├── openapi.json    # Committed backend spec snapshot (refresh after a contract change)
│       │   │   ├── generated/      # Committed Orval output, never edited (regenerate with api:gen)
│       │   │   ├── api-error.ts    # ApiError + mapping of the backend error body
│       │   │   └── client.ts       # The only hand-written fetch (Orval mutator)
│       │   └── env.ts              # Zod validation of NEXT_PUBLIC_API_URL
│       ├── orval.config.ts
│       ├── .env.example
│       ├── next.config.ts
│       └── package.json
├── packages/
│   ├── ui/
│   │   ├── src/
│   │   │   ├── components/         # Vendored shadcn components
│   │   │   ├── hooks/
│   │   │   ├── lib/                # utils.ts (cn)
│   │   │   └── styles/globals.css  # Tailwind entry and theme tokens
│   │   ├── components.json
│   │   └── postcss.config.mjs
│   ├── eslint-config/              # base.js, next.js, react-internal.js
│   └── typescript-config/          # base.json, nextjs.json, react-library.json
├── Dockerfile                      # Next.js standalone output on Node
├── turbo.json
└── package.json
```

## Architecture Decisions

Each decision is a separate ADR in `.agents/context/frontend/decisions/`, named `NNNN-<slug>.md`:

- [ADR-0001: Turborepo Monorepo](decisions/0001-turborepo-monorepo.md) - Accepted
- [ADR-0002: Next.js App Router](decisions/0002-nextjs-app-router.md) - Accepted
- [ADR-0003: Shared UI Package](decisions/0003-shared-ui-package.md) - Accepted
- [ADR-0004: Tailwind CSS 4](decisions/0004-tailwind-css-4.md) - Accepted
- [ADR-0005: TypeScript 5 with Type-Aware Lint](decisions/0005-typescript-type-aware-lint.md) - Accepted
- [ADR-0006: Theming](decisions/0006-theming.md) - Accepted
- [ADR-0007: Generated API Client](decisions/0007-generated-api-client.md) - Accepted
- [ADR-0008: Data-Fetching Rule](decisions/0008-data-fetching-rule.md) - Accepted
- [ADR-0009: Feature Folders](decisions/0009-feature-folders.md) - Accepted
- [ADR-0010: Test Scope](decisions/0010-test-scope.md) - Accepted
- [ADR-0011: Direct Backend URL](decisions/0011-direct-backend-url.md) - Open (owner: Maksym, team lead)
- [ADR-0012: Node and npm](decisions/0012-node-and-npm.md) - Accepted
- [ADR-0013: Motion Animations](decisions/0013-motion-animations.md) - Accepted
