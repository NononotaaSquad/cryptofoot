# 01 — KAS look of the shared UI package

**Spec:** [../spec.md](../spec.md) · **Blocked by:** None — can start immediately · **Status:** ready-for-agent

**What to build:** Every screen of the application looks like a KAS / gov.pl tool before any view is built. Text is set in Open Sans, the primary colour is the gov.pl dark blue, corners have a 4 px radius, and the button, the input and the pop-up of the shared UI package have the KAS shapes. The result is visible on the existing template start page, which still renders a card, buttons and a skeleton.

## Decisions that apply

- Tokens live in the shared UI package (lead's decision), following the token mapping table of the spec.
- Light theme only: the theme provider is forced to light; the dark token block is left as it is.
- Only the light `:root` tokens, the radius base, the sans font token and the new colour tokens change in the theme. Nothing else in the stylesheet is restructured.
- Primitives are changed in place in the shared package; no copies in the app.

## Files

Paths relative to `frontend/`. The file list was chosen by the lead (architecture B, UI package first).

| File | Change |
|---|---|
| `packages/ui/src/styles/globals.css` | Light tokens → gov.pl palette; radius base 4 px; `--font-sans` → Open Sans variable with the system stack as fallback; new colour tokens (`navy`, `primary-light`, `border-strong`, `error-dark`, `warning`, `success`, tints) |
| `packages/ui/src/components/input.tsx` | KAS field: height 44 px, horizontal padding 12 px, text 16 px at every width, border from the `input` token |
| `packages/ui/src/components/button.tsx` | New large size for the page-level action: height 48 px, horizontal padding 28 px, 16 px bold. Existing sizes stay |
| `packages/ui/src/components/alert-dialog.tsx` | gov.pl modal shape: left-aligned bold title 20 px, text 16 px in the foreground colour, padding 24 px, action right-aligned, no tinted footer bar, width about 480 px |
| `packages/ui/src/components/field.tsx` | New, added only with `npx shadcn@latest add field -c apps/web` |
| `apps/web/app/layout.tsx` | Load Open Sans (`latin`, `latin-ext`) through the Next.js font module and put its CSS variable on the document |
| `apps/web/app/providers.tsx` | Force the light theme |
| `.agents/context/frontend/decisions/0006-theming.md` | Record: light theme forced, KAS palette, no dark palette in the design |

## Before coding

- Read `.agents/rules/frontend.md` and `frontend/node_modules/next/dist/docs/01-app/01-getting-started/13-fonts.md` (the local docs of the installed Next.js).
- Read the reference modal `.agents/context/frontend/kas-ui-ux/assets/tool-biala-lista-blad.png` and the button and typography screenshots next to it.
- Check that the shadcn CLI installs the `base-nova` variant of `field` (the registry check in the planning session showed only that `field` exists).

## Acceptance criteria

- [ ] The start page renders in Open Sans, including Polish diacritics, with no request to Google from the browser.
- [ ] The default button is white text on `#0052a5`; focus rings are visible and blue; corners of buttons, inputs and cards are 4 px.
- [ ] Body text is `#1b1b1b`, muted text `#656565`, borders `#dadada`, muted surfaces `#f1f1f1`.
- [ ] The new colour tokens are usable as Tailwind colour utilities under their Pencil names.
- [ ] The input is 44 px high with 16 px text and a `#b7b7b7` border; the large button size is 48 px high with a 16 px bold label.
- [ ] An alert dialog opened in the browser matches the gov.pl modal: bold title, one sentence, one right-aligned primary button, focus trapped and returned.
- [ ] `field` exists in the shared package and was added by the CLI, not by hand.
- [ ] The application is light even when the operating system is set to dark.
- [ ] ADR-0006 describes the forced light theme.
- [ ] `npm run check` passes from `frontend/`; `npm run build` passes.
- [ ] Verified in the browser at `http://localhost:3000` with a clean console.

## Notes

- The alert dialog has no screen on the template page. To verify it, open one temporarily on the start page and remove it before the task ends, or verify it as the first step of task 02.
- English `sr-only` or `aria-label` strings in a vendored component are translated to Polish when the component is used (`frontend.md`).
- The input border `#b7b7b7` is about 2:1 on white; this follows the Pencil frame and is flagged in the spec.
- The other frontend developer must know about this change of the shared package before it is merged (ADR-0003). The lead handles that.
