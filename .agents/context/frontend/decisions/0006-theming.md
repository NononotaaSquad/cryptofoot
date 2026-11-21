# ADR-0006: Theming

**Status:** Accepted

**Context:**
Theme tokens include a dark variant.

**Decision:**
`app/providers.tsx` mounts the `next-themes` `ThemeProvider`. No theme toggle is provided; add one only if the product needs it.

**Consequences:**
- **Positive:** Dark mode is available without extra setup
- **Negative:** None significant
