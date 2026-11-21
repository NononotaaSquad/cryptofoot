# 02 — New case form with validation pop-ups

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 01 — KAS look of the shared UI package · **Status:** ready-for-agent

**What to build:** The Analyst opens `/` and sees the New case view of the Pencil frame "[MAIN] Nowa sprawa": header, form, guidance column, footer. They can fill in the fields, clear them with "Wyczyść" and click "Rozpocznij analizę". With too little data a pop-up says what is missing. With enough data nothing happens yet: starting the Analysis is task 03. The header link "Lista spraw →" opens a stub page of the Case list.

## Decisions that apply

- Architecture B: a shared page shell, the feature split into parts, `"use client"` only in the form.
- `react-hook-form` owns the form state. No resolver and no Zod schema: the submit handler calls the pure minimum data rule.
- Validation results are shown in a pop-up, not inline. Pop-up copy as accepted in the spec.
- The Case counter is not rendered.
- The stub page of the Case list has the page shell, the title "Lista spraw" and the header link "← Nowa sprawa".
- Below the `lg` breakpoint the guidance column moves under the form.

## Files

Paths relative to `frontend/apps/web/`. The file list was chosen by the lead (architecture B).

| File | Role |
|---|---|
| `package.json` | Add `react-hook-form` (with the lockfile) |
| `app/layout.tsx` | Metadata: application name "Projekt OŁCyŚ" |
| `app/page.tsx` | Thin Server Component that renders the New case view; template content removed |
| `app/cases/page.tsx` | Stub page of the Case list |
| `public/kas-logo-mark.png` | Copy of `.agents/context/frontend/kas-logo-mark.png` |
| `components/page-shell.tsx` | Server Component: header, content column 1200 px, footer; props: header link and children |
| `features/new-case/new-case-view.tsx` | Server Component: title, introduction, two columns |
| `features/new-case/new-case-form.tsx` | Client Component: `react-hook-form`, submit, "Wyczyść", pop-up state |
| `features/new-case/identifier-fields.ts` | The seven field definitions: name, label, example, hint, narrowing or not |
| `features/new-case/identifier-field.tsx` | One field: label, input, hint, optional error |
| `features/new-case/analysis-guidance.tsx` | Static right column |
| `features/new-case/new-case-dialogs.tsx` | Validation pop-up for `empty` and `fullNameOnly` |
| `features/new-case/new-case-rules.ts` | Pure functions: split values, minimum data decision |
| `features/new-case/new-case-rules.test.ts` | Vitest cases listed in the spec |

`features/backend-status/` stays as the documented reference feature; the start page no longer renders it.

## Before coding

- Read the current `react-hook-form` docs for `useForm`, `register`, `handleSubmit`, `reset` and `formState` (the spec marks its API as unchecked).
- Read the Pencil frame "[MAIN] Nowa sprawa" through the Pencil MCP for anything the layout table of the spec leaves open.
- Read `frontend/node_modules/next/dist/docs/` for linking, images and metadata in the installed Next.js.

## Acceptance criteria

- [ ] `/` shows the view with the copy of the spec, letter for letter, and the layout values of the spec at a 1920 px wide window.
- [ ] Every field has a visible label, the "np. …" example as placeholder and the hint linked to the input for assistive technology.
- [ ] No field is marked with an asterisk; the full name label carries "(opcjonalnie)".
- [ ] The whole view works by keyboard: tab order follows the reading order, Enter in a field submits, focus styles are visible.
- [ ] Submitting an empty form, or one with only spaces and commas, opens the pop-up "Podaj co najmniej jeden identyfikator".
- [ ] Submitting only the full name opens the pop-up "Samo imię i nazwisko nie wystarczy".
- [ ] The pop-up closes with "Rozumiem" and with Escape, returns focus, and the typed values stay.
- [ ] Submitting with at least one narrowing identifier opens no pop-up.
- [ ] "Wyczyść" empties all seven fields.
- [ ] The button "Rozpocznij analizę" is never disabled.
- [ ] The header shows the KAS sign with an `alt`, "Projekt OŁCyŚ" and the link "Lista spraw →" without a counter; the link opens `/cases`.
- [ ] `/cases` shows the page shell, the title "Lista spraw" and the link "← Nowa sprawa" back to `/`.
- [ ] Below the `lg` breakpoint the guidance column is under the form and nothing overflows horizontally.
- [ ] `new-case-rules.test.ts` covers every case listed in the spec and passes.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against the Pencil frame, every state touched, with a clean console.

## Notes

- The form values are keyed `nickname`, `login`, `email`, `messengerId`, `profileAddress`, `cryptocurrencyAddress`, `fullName`.
- The rule returns the parsed identifiers together with `ok`, so task 03 does not parse again.
- "Wyczyść" is a button (an action), the header link is a link (navigation): the gov.pl guide never uses a button for navigation.
- Page description text in the metadata is not defined anywhere; propose one sentence to the lead instead of inventing it silently.
