# Spec: New case view

**Date:** 2026-11-21 · **Branch:** `feature/FRONTEND-new-case-view` (from `feature/FRONTEND`) · **Status:** decided by the lead, ready to implement

This spec records the decisions of the planning session of 2026-11-21 for the first frontend view, New case (PL: Nowa sprawa). It follows the `to-spec` template and is saved as a file instead of a tracker issue, on the lead's request. The work is split into three tasks in [tasks/](tasks/).

Sources this spec is built on, and which win when they differ from it:

- `.agents/context/frontend/views.md`: behaviour and UI copy of the view (wins on product behaviour).
- Pencil frame "[MAIN] Nowa sprawa" in `.agents/context/frontend/2026-11-21_kas-olcys-views_v01.pen`: layout, sizes, copy (wins on visual detail; read it through the Pencil MCP, the file is encrypted).
- `.agents/context/frontend/kas-ui-ux/README.md`: gov.pl palette, typography, modal pattern.
- `.agents/rules/frontend.md` and the frontend ADRs: how the code is written (win on code rules).
- `.agents/context/domain/BUSINESS-CONTEXT.md`: the glossary in force. `glossary.md`, `product.md` and `domain-model.md` in `.agents/context/` are still empty templates.

## Problem Statement

The Analyst (PL: Analityk), a KAS official without IT knowledge, opens the application and has nowhere to start. The start page is still the hackathon template. There is no form for the identifiers of the investigated person, the application does not look like a KAS tool, and nothing leads from entered data to a report.

## Solution

The start page `/` becomes the New case view from the Pencil frame "[MAIN] Nowa sprawa":

- a header with the KAS sign, the text "Projekt OŁCyŚ" and the link "Lista spraw →";
- a form with six narrowing identifiers and the optional full name, each field with an example and a hint;
- one primary button "Rozpocznij analizę" and the text action "Wyczyść";
- a grey guidance column "Co się stanie, gdy rozpoczniesz analizę";
- a one-line footer.

When the Analyst submits too little data, a pop-up says what is missing. When the data is enough, a full-screen loader blocks the page while the Analysis runs, the browser warns before the tab is closed, and then Case details opens with this Analysis selected. Until the backend has the Case contract, the Analysis runs on a mock.

The whole application gets the KAS look first: the shared UI package carries the gov.pl palette, Open Sans and the KAS shapes of the button, the input and the pop-up.

## User Stories

1. As an Analyst, I want to see the New case form when I open the application, so that I can start work at once.
2. As an Analyst, I want the application to look like other KAS and gov.pl tools, so that I trust it and know how to use it.
3. As an Analyst, I want a header with the KAS sign and the name "Projekt OŁCyŚ", so that I know which tool I am in.
4. As an Analyst, I want a link "Lista spraw →" in the header, so that I can go to my Cases at any time.
5. As an Analyst, I want the link to the Case list to open a page instead of an error, so that the application does not look broken while that view is not built.
6. As an Analyst, I want a field for each kind of narrowing identifier (nickname, login, e-mail, messenger ID, profile address, public cryptocurrency address), so that I can enter everything I know.
7. As an Analyst, I want a separate group "Dane dodatkowe" with the full name marked "(opcjonalnie)", so that I see it is not a narrowing identifier.
8. As an Analyst, I want an "np. …" example inside every field, so that I know the expected form of the value.
9. As an Analyst, I want a one-sentence hint under every field, so that I know what the field means without technical terms.
10. As an Analyst, I want to enter several values of one kind in one field, separated by commas, so that I can pass two nicknames or two addresses.
11. As an Analyst, I want the rule "Podaj co najmniej jeden" stated once for the whole group, without asterisks, so that the form stays quiet.
12. As an Analyst, I want the button "Rozpocznij analizę" to be always active, so that clicking it tells me what is missing instead of doing nothing.
13. As an Analyst, I want to submit the form with the Enter key, so that I do not have to reach for the mouse.
14. As an Analyst, I want a pop-up that lists the narrowing identifiers when I submit an empty form, so that I know what to enter.
15. As an Analyst, I want a pop-up that says a full name alone is not enough when I entered only the full name, so that I understand why the Analysis did not start.
16. As an Analyst, I want a field with only spaces or commas to count as empty, so that I do not start an Analysis by mistake.
17. As an Analyst, I want the pop-up to close with the button "Rozumiem" and with the Escape key, and to keep what I typed, so that I can fix the form at once.
18. As an Analyst who uses a keyboard or a screen reader, I want the pop-up to take focus, keep it inside and give it back when it closes, so that I do not get lost.
19. As an Analyst, I want the text action "Wyczyść" to empty all fields, so that I can start over for another person.
20. As an Analyst, I want a guidance column that says in four steps what happens after I start the Analysis, so that I am not surprised by the wait.
21. As an Analyst, I want the reminder "Pamiętaj" about the minimum data, so that I see the rule before I click.
22. As an Analyst, I want one click on "Rozpocznij analizę" to create the Case and start its first Analysis, so that I do not create the Case as a separate step.
23. As an Analyst, I want a full-screen loader with "Trwa analiza. Nie zamykaj tej strony.", so that I know the Analysis is running and that leaving can break it.
24. As an Analyst, I want the page behind the loader to be unreachable by mouse and keyboard, so that I cannot start a second Analysis or navigate away by accident.
25. As an Analyst, I want the browser to warn me when I try to close or reload the tab during an Analysis, so that I do not close it by accident.
26. As an Analyst, I want Case details to open by itself when the Analysis ends, with this Analysis in the page address, so that I do not have to look for the report.
27. As an Analyst, I want a clear message with "Spróbuj ponownie" when starting the Analysis fails, with my data still in the form, so that I do not type everything again.
28. As an Analyst, I want messages from the server about a single field shown next to that field, so that I know which value to fix.
29. As an Analyst on a narrow screen, I want the guidance column to move under the form, so that the form stays readable.
30. As a frontend developer, I want the header, the content column and the footer as one shared page shell, so that the Case list and Case details reuse it.
31. As a frontend developer, I want the gov.pl palette and Open Sans as tokens of the shared UI package, so that every shadcn component looks like KAS without per-view overrides.
32. As a frontend developer, I want the minimum data rule and the splitting of values as pure functions with tests, so that the invariant is checked without a browser.
33. As a frontend developer, I want the mock of "start Analysis" behind one function with the shape of a mutation, so that swapping it for the generated client is a change in one place.

## Implementation Decisions

### Decisions of the lead (2026-11-21)

- **Scope Z2.** The branch delivers the form, "Wyczyść", two validation pop-ups and the Analysis path on a mock: blocking loader, browser warning on tab close, navigation to Case details. The duplicate pop-up "Taka sprawa już istnieje" is deferred until the backend contract exists.
- **Loader look.** The lead accepted the proposal: a white full-screen overlay, a spinner and the sentence "Trwa analiza. Nie zamykaj tej strony." It is judged in the browser, not drawn in Pencil first. This closes V4 of `views.md` for now.
- **Missing routes.** `/cases` and `/cases/{caseId}` get stub pages (the page shell and a title only). No `as Route` casts.
- **Case counter.** Hidden until the backend returns the number of Cases with New Results. No mock value.
- **Pop-up copy** (accepted as written):
  - Empty form: title "Podaj co najmniej jeden identyfikator", text "Aby rozpocząć analizę, wpisz nick, login, adres e-mail, identyfikator z komunikatora, adres profilu lub adres kryptowalutowy.", button "Rozumiem".
  - Full name only: title "Samo imię i nazwisko nie wystarczy", text "Dodaj co najmniej jeden identyfikator, na przykład nick lub adres e-mail.", button "Rozumiem".
- **Form library.** `react-hook-form` is added to simplify the form. This replaces the earlier proposal of uncontrolled inputs read through `FormData`.
- **Identifier names and shape (2026-11-21), following the backend contract `CreateCaseRequest` (PR #51).** The field and identifier kind `profileAddress` is renamed `profileUrl` (label stays "Adres profilu"). `cryptocurrencyAddress` takes exactly one value: it is only trimmed, never split on commas, and an empty value counts as absent (like `fullName`); it stays a narrowing identifier. Open: the group description "W każdym polu możesz podać kilka, oddzielając je przecinkami." is now inexact for the cryptocurrency address field; the UI copy is not changed yet.
- **Architecture B** (feature split into parts, a shared page shell), with one change: **the shared UI package is set up for KAS first**, before the view is built. Tokens and the KAS shapes of the primitives live in `@workspace/ui`, not in an app-level stylesheet.

### Decisions made by Claude inside the lead's choices (confirm or change at the first checkpoint)

- **No `@hookform/resolvers`, no Zod schema for the form.** The form has no per-field client rule, only one cross-field rule whose result goes to a pop-up. The submit handler calls the pure rule function. `react-hook-form` handles registration of fields, reset, the submitting state and server field errors.
- **The page shell goes to the app's `components` folder**, the default of proposal B. The alternative `features/app-shell` was offered and not chosen explicitly.
- **Light theme only.** The design has no dark palette. The theme provider is forced to light; the dark token block of the UI package stays untouched. This narrows ADR-0006, which must be updated in the same task.
- **The `field` component of shadcn is added** with the CLI, to link label, hint and error of a field.
- **The template content of the start page is removed.** The reference feature `backend-status` stays in the repository as the documented example and is no longer rendered.
- **Values are not deduplicated or normalised** beyond trimming and dropping empty parts. Normalisation is open (V6).

### Modules

- **KAS theme of the shared UI package.** Theme tokens of the light theme are mapped to the gov.pl palette (table below). The radius base becomes 4 px. The sans font token points at Open Sans with the old system stack as fallback. The additional gov.pl colours and the tints of the Pencil file become colour tokens under their Pencil names.
- **KAS primitives of the shared UI package.** The input gets the KAS field shape. The button gets a large size for the page-level action. The alert dialog gets the gov.pl modal shape. The `field` component is added.
- **Root layout.** Loads Open Sans (subsets `latin` and `latin-ext`) through the font module of Next.js and exposes it as a CSS variable. Sets the application name in the metadata. Forces the light theme.
- **Page shell.** A Server Component with the header, the centred content column of 1200 px and the footer. Its interface: the header link (label and target) and the page content. The Case counter is not rendered.
- **New case view.** A Server Component: page title, introduction, the form and the guidance column in two columns that stack below the `lg` breakpoint.
- **New case form.** The only Client Component of the view. Owns the form state through `react-hook-form`, runs the minimum data rule on submit, opens the validation pop-up, and (Task 3) starts the Analysis.
- **Identifier field definitions.** One list of the seven fields: form name, label, example, hint, and whether the kind is narrowing. The form and the rule both read it, so a field is defined once.
- **Identifier field.** Label, input, hint and optional error of one field, linked for assistive technology.
- **Minimum data rule.** Pure functions: split a field value into values; decide whether the form may start an Analysis. Interface of the decision: form values in, one of `ok` (with the parsed identifiers), `empty`, `fullNameOnly` out.
- **Validation pop-up.** One component that renders the pop-up for `empty` or `fullNameOnly`.
- **Analysis guidance.** The static right column.
- **Start Analysis mock.** One async function: parsed New case data in, `{ caseId, analysisId }` out after a fixed delay. It is called through a TanStack Query mutation, so the view already has the pending, error and success states of the future generated hook.
- **Analysis loader.** The blocking overlay; while it is mounted it also holds the `beforeunload` warning.
- **Stub pages** of the Case list and Case details.

### Names in code

From `BUSINESS-CONTEXT.md` and `views.md`: `nickname`, `login`, `email`, `messengerId`, `profileUrl`, `cryptocurrencyAddress` (the six narrowing identifier kinds) and `fullName` (additional data). The feature is `new-case`. "Case", "Analysis" and "narrowing identifier" are used as in the glossary; the word "identifier" is never used for the Case Number or database ids (those are `caseId`, `analysisId`).

### Token mapping (Pencil variable → theme token)

| Pencil | Value | Theme token |
|---|---|---|
| `white` | `#ffffff` | background, card, popover, primary-foreground |
| `text` | `#1b1b1b` | foreground, card-foreground, popover-foreground |
| `primary` | `#0052a5` | primary, ring |
| `surface` | `#f1f1f1` | muted, secondary |
| `text-muted` | `#656565` | muted-foreground |
| `border` | `#dadada` | border |
| `border-strong` | `#b7b7b7` | input (border of form fields) |
| `error` | `#d5233f` | destructive |
| `primary-tint` | `#f0f6fc` | accent |
| `radius` | 4 px | radius base |
| `font` | Open Sans | sans font |

New colour tokens under their Pencil names: `navy` `#00468d`, `primary-light` `#006cd7`, `border-strong` `#b7b7b7`, `error-dark` `#a7162d`, `warning` `#eba828`, `success` `#598527`, `primary-tint` `#f0f6fc`, `success-tint` `#eef4e6`, `success-text` `#3a5a16`, `warning-tint` `#fdf3dc`, `warning-text` `#6b4a00`, `error-tint` `#fbe9ec`.

### Layout values from the Pencil frame

- **Page:** drawn at 1920 px; content column 1200 px, centred; the header and the footer use the same column.
- **Header:** height 72 px, white, bottom border 1 px `border`. Left: KAS sign 64 × 48 px (fit), gap 16 px, "Projekt OŁCyŚ" 20 px bold `navy`. Right: link "Lista spraw →" 16 px semibold `primary`.
- **Content:** padding top 40 px, bottom 56 px; gap 32 px between the title block and the columns.
- **Title block:** width 728 px; "Nowa sprawa" 40 px bold, line height 1.25; gap 10 px; introduction 16 px, line height 1.5.
- **Columns:** gap 56 px. Form column 728 px; guidance column takes the rest (416 px).
- **Form:** gap 40 px between the group "Identyfikatory", the divider (1 px, `border`) and the group "Dane dodatkowe". Inside a group: gap 24 px; group heading 24 px bold, line height 1.25; gap 8 px; group description 16 px.
- **Field grid:** two columns, gap 20 px; three rows for the narrowing identifiers; the full name takes the left column of its own row.
- **Field:** gap 8 px. Label 16 px semibold ("(opcjonalnie)" 16 px regular `text-muted`, gap 8 px). Input: height 44 px, border 1 px `border-strong`, radius 4 px, horizontal padding 12 px, text 16 px, example in `text-muted`. Hint 14 px `text-muted`, line height 1.5.
- **Actions:** gap 24 px. Primary button: height 48 px, horizontal padding 28 px, 16 px bold white on `primary`, radius 4 px. "Wyczyść": 16 px semibold `primary`, text only.
- **Guidance column:** background `surface`, radius 4 px, padding 28 px, gap 20 px. Heading 20 px bold, line height 1.25. Steps: gap 16 px; number 16 px bold in a 20 px column, gap 12 px, text 16 px. Divider 1 px `border-strong`. Reminder: "Pamiętaj" 16 px bold, gap 4 px, text 16 px.
- **Footer:** top border 1 px `border`, vertical padding 20 px, text 14 px `text-muted`.

### UI copy

All copy is taken from the Pencil frame as written.

- **Title:** "Nowa sprawa". **Introduction:** "Wprowadź poniżej znane dane do analizy obywatela. Na tej podstawie przeszukamy publiczne źródła i przygotujemy raport."
- **Group "Identyfikatory":** "Podaj co najmniej jeden. W każdym polu możesz podać kilka, oddzielając je przecinkami."
- **Group "Dane dodatkowe":** "Te dane pomagają zawęzić wyniki, ale nie musisz ich podawać."

| Field | Label | Example | Hint |
|---|---|---|---|
| `nickname` | Nick | np. krypto_janek | Nazwa, pod którą osoba występuje w internecie. |
| `login` | Login | np. jkowalski84 | Nazwa konta, której osoba używa do logowania. |
| `email` | Adres e-mail | np. jan.kowalski@example.com | Adres poczty elektronicznej. |
| `messengerId` | Identyfikator z komunikatora | np. @janek_k | Nazwa lub numer konta w komunikatorze, na przykład w Telegramie. |
| `profileUrl` | Adres profilu | np. https://forum.example.com/profil/janek | Link do profilu w mediach społecznościowych, na forum lub w serwisie ogłoszeniowym. |
| `cryptocurrencyAddress` | Adres kryptowalutowy | np. bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkf | Adres portfela, na przykład w sieci Bitcoin lub Ethereum. |
| `fullName` | Imię i nazwisko (opcjonalnie) | np. Jan Kowalski | Samo imię i nazwisko nie wystarczy, ponieważ prowadzi do zbyt ogólnych wyników. |

- **Actions:** "Rozpocznij analizę", "Wyczyść".
- **Guidance heading:** "Co się stanie, gdy rozpoczniesz analizę".
  1. "Utworzymy nową sprawę. Znajdziesz ją później na liście spraw."
  2. "Przeszukamy publicznie dostępne źródła w internecie pod kątem podanych identyfikatorów."
  3. "Analiza chwilę potrwa. Nie zamykaj w tym czasie strony."
  4. "Gdy analiza się zakończy, raport otworzy się sam."
- **Reminder:** "Pamiętaj" / "Aby rozpocząć analizę, podaj co najmniej jeden identyfikator. Samo imię i nazwisko nie wystarczy."
- **Loader:** "Trwa analiza. Nie zamykaj tej strony."
- **Footer:** "Wszelkie prawa zastrzeżone. Krajowa Administracja Skarbowa, projekt OŁCyŚ."
- **Header links:** New case "Lista spraw →"; Case list "← Nowa sprawa"; Case details "← Lista spraw".
- **Failed start of an Analysis:** title "Coś poszło nie tak", the text from `toUserMessage(error)`, button "Spróbuj ponownie" (wording fixed by `frontend.md`).

### Behaviour

- **Splitting values:** a field value is split on commas; each part is trimmed; empty parts are dropped. The full name is one value, trimmed; an empty one counts as absent.
- **Minimum data rule:** `ok` when at least one narrowing identifier kind has at least one value. `fullNameOnly` when none has and the full name is present. `empty` otherwise.
- **Submit:** always possible. `empty` and `fullNameOnly` open the pop-up and do nothing else; the field values stay. `ok` starts the Analysis.
- **"Wyczyść":** resets all fields to empty and removes field errors. It is a button, not a link.
- **Pop-ups:** modal, role `alertdialog`, focus trapped inside and returned on close, closed by the button and by Escape.
- **Loader:** shown from the start of the mutation until it settles. The page behind it is inert. Escape does not close it. It has no button.
- **Tab close:** a `beforeunload` listener is registered only while the Analysis runs and removed afterwards (pattern from MDN: `preventDefault()` plus the legacy `returnValue`).
- **After success:** client navigation to `/cases/{caseId}?analysis={analysisId}`.
- **After failure:** the loader disappears; the error pop-up is shown. A 400 with field errors puts each message next to its field by form name; errors without a field go above the form.
- **Data fetching rule (ADR-0008):** no Server Actions. `page.tsx` files stay thin Server Components; `"use client"` sits only in the form (and the loader and pop-ups it renders).

### Dependencies

- **Added:** `react-hook-form` in the web app. `checked: npm registry, 2026-11-21` — latest 7.92.0, peer `react ^16.8 || ^17 || ^18 || ^19`, so it fits React 19.3.1. Its API (`useForm`, `register`, `handleSubmit`, `reset`, `setError`, `formState`) is `unchecked (memory)`: read the current docs before coding (Context7 was not available in the planning session).
- **Not added:** `@hookform/resolvers` (latest 7.0.8, supports Zod 4; `checked: npm registry, 2026-11-21`), msw, Testing Library, jsdom.
- **Already installed and used** (`checked: frontend/package-lock.json`): Next 16.4.6, React 19.3.1, Tailwind 4.3.3, Base UI 1.9.0 under shadcn `base-nova`, TanStack Query 5.107.1, lucide-react 1.66.0.
- **shadcn `field`:** exists in the registry (`checked: npx shadcn view @shadcn/field`, CLI 4.23.0, 2026-11-21). The `base-nova` variant that the CLI installs is `unchecked`.

### Facts checked in the planning session

- With `typedRoutes: true` (already set), a link to a route that does not exist is a type error. `checked: nextjs.org/docs/app/api-reference/config/typescript#statically-typed-links (2026-08-25)`. Whether a template literal with a query string passes the check is `unchecked`.
- Base UI alert dialog is always modal, cannot be dismissed by an outside press and has the role `alertdialog`. `checked: Base UI v1.9.0 source (useRenderDialogRoot)`.
- In the shared alert dialog wrapper the action button is a plain button and does not close the dialog by itself; the pop-ups are controlled (`open` plus `onOpenChange`).
- Open Sans is a variable font in the Next.js font data; `latin-ext` is among its subsets. `checked: next.js v16.4.6 font-data.json`.
- `beforeunload`: the prompt needs a user gesture on the page and its text cannot be customised. `checked: MDN, Window: beforeunload event (2026-10-26)`.
- The local Next.js docs are in `frontend/node_modules/next/dist/docs/` after `npm ci`; `frontend/AGENTS.md` requires reading the relevant guide there before coding.

## Testing Decisions

- **A good test here** checks external behaviour of pure logic: values in, decision out. No test looks at component internals, `react-hook-form` state or markup.
- **Tested module:** the minimum data rule (splitting of values and the decision). This is the one seam; it carries the invariant "at least one narrowing identifier; a full name alone is not enough" (E1, story 1).
- **Cases:** empty form → `empty`; only spaces and commas → `empty`; only the full name → `fullNameOnly`; full name of spaces only with nothing else → `empty`; each narrowing kind alone → `ok`; narrowing identifier plus full name → `ok`; several values in one field are split and trimmed; empty parts between commas are dropped.
- **Not tested by Vitest** (ADR-0010: no component and no E2E tests): the form, the pop-ups, the loader, the page shell, the mock. They are verified in the browser with a clean console, as `frontend.md` requires.
- **Prior art:** `lib/api/error-message.test.ts` and `lib/api/api-error.test.ts` (co-located `*.test.ts`, node environment, `describe` / `it.each`).
- **Quality gate:** `npm run check` from `frontend/` after every change; it was green on the untouched branch (7 tasks, 22 tests) on 2026-11-21.

## Out of Scope

- The duplicate pop-up "Taka sprawa już istnieje" and the duplicate rule (waits for the contract: status code and the id of the existing Case).
- The real backend call, the generated client for Case and Analysis, `openapi.json` changes.
- The Case counter in the header.
- The Case list and Case details views beyond their stub pages.
- Client-side format checks of single values (e-mail, URL, cryptocurrency address). `views.md` defines none.
- Normalisation and deduplication of values (V6).
- Dark theme, a theme toggle.
- Component tests and E2E tests.
- Measuring the contrast of the tint tokens (V12).

## Further Notes

- **Changing the shared UI package needs the agreement of the other frontend developer** (ADR-0003, `frontend.md`). The lead decided to set the package up for KAS; telling the other developer before Task 1 is merged stays with the lead.
- **Mock types are temporary.** `frontend.md` forbids hand-written response types; `views.md` allows mocks "built on the agreed types" until the contract exists. The Case contract is not agreed (V8), so the shape of the mock is provisional and is replaced by the generated hook in one place.
- **Unproven on the mock:** the failure path of starting an Analysis (error pop-up, field errors from a 400). The mock does not fail, so this path can be read but not exercised in the browser.
- **Contrast of the input border:** `#b7b7b7` on white is about 2:1, below the 3:1 that the gov.pl guide asks for UI components. The Pencil frame uses it; the spec follows the frame and flags it next to V12.
- **KAS sign:** the permission to use it is still an open question in `kas-ui-ux/README.md`; the lead decided to show it in the demo.
- **Difference from E1:** the epic lists the cryptocurrency address as out of scope for input; `views.md` (newer) makes it a narrowing identifier. This spec follows `views.md`.
- **Environment:** `frontend/node_modules` is installed (`npm ci`, 2026-11-21). npm reported one install script not yet approved (`esbuild` postinstall); the checks pass without it.
- **Research:** two Sonnet researchers mapped the codebase and the documentation for the installed versions on 2026-11-21. Their findings are folded into this spec; Context7 was not available, so official docs were used.
