# ADR-0002: Next.js App Router

**Status:** Accepted

**Context:**
Next.js 16 uses the App Router by default.

**Decision:**
Use the App Router. Whether a component is a Server or Client Component is decided by ADR-0008. No Server Actions and no Cache Components.

**Consequences:**
- **Positive:** Streaming, nested layouts, `error.tsx` / `loading.tsx` / `not-found.tsx` conventions
- **Negative:** Server/Client boundary must be placed deliberately
