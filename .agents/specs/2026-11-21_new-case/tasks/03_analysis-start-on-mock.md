# 03 — Starting the Analysis on a mock

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 02 — New case form with validation pop-ups · **Status:** ready-for-agent

**What to build:** The Analyst fills in at least one narrowing identifier and clicks "Rozpocznij analizę". A full-screen loader with "Trwa analiza. Nie zamykaj tej strony." blocks the page, the browser warns when they try to close or reload the tab, and when the Analysis ends Case details opens by itself with this Analysis in the address. The Analysis runs on a mock until the backend has the Case contract.

## Decisions that apply

- Scope Z2: loader, browser warning, navigation. The duplicate pop-up is out of scope.
- Loader look accepted by the lead: white full-screen overlay, spinner, one sentence. No button.
- The mock is one async function called through a TanStack Query mutation, so the generated hook replaces it in one place.
- `/cases/{caseId}` is a stub page: the page shell, the title "Szczegóły sprawy" and the header link "← Lista spraw".
- A failed start is shown only through `toUserMessage(error)`, with the title "Coś poszło nie tak" and the button "Spróbuj ponownie" (`frontend.md`).

## Files

Paths relative to `frontend/apps/web/`. The file list was chosen by the lead (architecture B).

| File | Role |
|---|---|
| `features/new-case/start-analysis.ts` | The mock: parsed New case data in, `{ caseId, analysisId }` out after a fixed delay; one comment says it is replaced by the generated client |
| `features/new-case/analysis-loader.tsx` | Client Component: blocking overlay; registers the `beforeunload` warning while mounted |
| `features/new-case/new-case-form.tsx` | Change: mutation, loader while pending, navigation on success, error pop-up and field errors on failure |
| `features/new-case/new-case-dialogs.tsx` | Change: error pop-up "Coś poszło nie tak" |
| `app/cases/[caseId]/page.tsx` | Stub page of Case details |

## Before coding

- Read the TanStack Query docs for `useMutation` in the installed version (5.107.1); the planning session did not check them.
- Read the Base UI 1.9.0 docs for a dialog that cannot be dismissed (Escape and outside press), or choose a non-dialog overlay that makes the page behind inert; record which one and why.
- Read the local Next.js docs for `useRouter` and for the async `params` of a dynamic page.
- Check whether a template literal with a query string passes the typed routes check; if not, build the address in a way that needs no cast.

## Acceptance criteria

- [ ] A valid submit shows the loader at once, over the whole viewport, with the sentence "Trwa analiza. Nie zamykaj tej strony."
- [ ] While the loader is shown, the form behind it cannot be reached by mouse or keyboard, and Escape does not close the loader.
- [ ] The loader announces its text to a screen reader.
- [ ] Closing or reloading the tab while the loader is shown triggers the browser's own warning; after the Analysis ends, it no longer does.
- [ ] When the mock resolves, the address becomes `/cases/{caseId}?analysis={analysisId}` and the stub page of Case details is shown, without an extra click.
- [ ] A second Analysis cannot be started while one is running.
- [ ] On a rejected mutation the loader disappears, the typed values stay and the pop-up "Coś poszło nie tak" shows the text of `toUserMessage(error)` with "Spróbuj ponownie".
- [ ] A 400 with field errors puts each message next to the field of the same name; errors without a field go above the form.
- [ ] `/cases/{caseId}` shows the page shell, the title "Szczegóły sprawy" and the link "← Lista spraw".
- [ ] No hand-written `fetch`; the mock makes no network call.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser: the whole path from `/` to the stub of Case details, the tab-close warning, a clean console.

## Notes

- The mock never fails, so the failure criteria cannot be exercised in the browser. Report them at the gate as read, not as run, or reject the mock once by hand during verification and say so.
- The types of the mock are provisional (V8 of `views.md` is open) and must not spread outside the feature folder.
- The `beforeunload` prompt appears only after a user gesture on the page; clicking the submit button is one.
- The client navigation after success does not fire `beforeunload`, so the listener must be gone before the navigation only for cleanliness, not for correctness.
