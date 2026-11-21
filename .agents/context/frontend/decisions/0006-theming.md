# ADR-0006: Theming

**Status:** Accepted (narrowed 2026-11-21: light theme forced)

**Context:**
The application is a KAS tool and follows the gov.pl look (`.agents/context/frontend/kas-ui-ux/README.md`, Pencil file `2026-11-21_kas-olcys-views_v01.pen`). The design has only a light palette; there is no dark variant of the gov.pl colours. The template's theme tokens still contain a `.dark` block.

**Decision:**
- The light theme tokens in `packages/ui/src/styles/globals.css` carry the gov.pl / KAS palette: primary `#0052a5`, text `#1b1b1b`, muted text `#656565`, borders `#dadada`, field borders `#b7b7b7`, muted surfaces `#f1f1f1`, destructive `#d5233f`, radius base 4 px, Open Sans as the sans font. The additional Pencil colours (`navy`, `primary-light`, `primary-tint`, `border-strong`, `error-dark`, `error-tint`, `warning`, `warning-tint`, `warning-text`, `success`, `success-tint`, `success-text`) are Tailwind colour tokens under their Pencil names.
- `app/providers.tsx` mounts the `next-themes` `ThemeProvider` with `forcedTheme="light"` and the system preference disabled. The application is light even when the operating system is dark.
- The `.dark` token block is left as generated and is not maintained. No theme toggle.

**Consequences:**
- **Positive:** Every shadcn component looks like a KAS tool without per-view overrides; one palette to check.
- **Negative:** No dark mode. Adding one later needs a dark gov.pl palette from design and a review of the `.dark` block, which does not contain the new colour tokens.
