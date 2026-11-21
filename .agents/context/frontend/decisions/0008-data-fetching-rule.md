# ADR-0008: Data-Fetching Rule

**Status:** Accepted

**Context:**
Agents need one rule to decide between Server and Client Components.

**Decision:**
Ask: "Does the data on this screen change as a result of a user action (save, filter, refresh)?" Yes: Client Component using the generated TanStack Query hook, rendering loading, error, empty and data states. No: Server Component that awaits the generated fetch function. No backend data: Server Component. `page.tsx` stays a thin Server Component; `"use client"` goes in the feature view. No Server Actions (the backend is Spring; an action adds a hop). Secrets never reach the frontend.

**Consequences:**
- **Positive:** Predictable boundaries, one cache (TanStack Query) for mutable data
- **Negative:** Server-rendered data is not refreshed by mutations; use the client path when in doubt
