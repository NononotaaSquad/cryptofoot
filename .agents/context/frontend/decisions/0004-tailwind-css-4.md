# ADR-0004: Tailwind CSS 4

**Status:** Accepted

**Context:**
Need utility-first styling with shared theme tokens.

**Decision:**
Use Tailwind CSS 4, configured CSS-first in `packages/ui/src/styles/globals.css` (no `tailwind.config`). PostCSS config is `packages/ui/postcss.config.mjs`, re-exported to the app. Prettier sorts classes.

**Consequences:**
- **Positive:** Fast UI work, one place for tokens
- **Negative:** Long class strings
