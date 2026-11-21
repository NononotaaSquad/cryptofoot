# ADR-0013: Motion animations

**Status:** Accepted

**Context:**
A demo judged in a few minutes benefits from a polished feel. `motion` (`motion/react`) is installed in `apps/web`; `tw-animate-css` is available through `packages/ui`. No component uses Motion yet.

**Decision:**
Subtle animations make the product feel interactive and modern while the jury evaluates it. They are optional: use them only where they improve the demo. Because Motion is already in the codebase, adding them costs little time.

**Consequences:**
- **Positive:** Animation is available without setup when the design needs it
- **Negative:** Extra client-side JavaScript in components that import Motion
