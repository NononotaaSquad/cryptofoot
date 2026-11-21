# ADR-0012: Node and npm

**Status:** Accepted

**Context:**
Orval and Vitest need Node >= 22.18. Bun was evaluated.

**Decision:**
Node >= 22.18 and npm. Bun rejected for this event: Node is still needed, no documented Docker pattern, open Windows issues.

**Consequences:**
- **Positive:** One well-known toolchain on every machine
- **Negative:** Slower installs than Bun
