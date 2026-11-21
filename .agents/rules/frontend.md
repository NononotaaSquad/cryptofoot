# Frontend Rules

Read at the start of every frontend task. Context and rationale: `.agents/context/frontend/frontend-architecture.md`. Product rules: `.agents/context/product.md`.

## Before you code

- Read `.agents/context/product.md` first - features follow the challenge, not guesses.
- Stop and ask when a domain term or rule is missing - never invent one.
- Use the term from `.agents/context/glossary.md` in types, identifiers and UI copy - one name per concept.

## Quality gate

- Run `npm run check` (from `frontend/`) after every change and before every commit - CI runs the same command, then `npm run build`.
- If `format:check` fails, run `npm run format`.
- Fix every lint error; never disable a rule inline without a comment saying why - lint errors fail CI.
- Don't use `any` or `!` non-null assertions. Avoid `as` casts - type the value instead; a justified `as` is allowed when it simplifies the implementation (e.g. a generic boundary like `customFetch` in `apps/web/lib/api/client.ts`), with a comment saying why.
- Await or `void` every promise - floating promises are a lint error.

## Tests

- Write a Vitest test for pure logic that carries an invariant or that the product depends on (calculations, validation, mapping API responses).
- Co-locate tests as `*.test.ts` next to the code, under `apps/web/features/` or `apps/web/lib/` - Vitest runs only those two folders and only `.test.ts`.
- Don't write component tests or E2E tests.

## Talking to the backend

- Use only the generated client in `apps/web/lib/api/generated/` - no hand-written `fetch`, no hand-written response types. If `generated/` does not exist yet, create it with the steps in `frontend/README.md` ("Updating the API client"); do not write a client by hand.
- Before touching code that calls the backend, follow the `api-contract` skill: run `./scripts/api-sync.sh` (repo root, needs Docker) so `openapi.json` and `generated/` match the backend; commit any drift separately first.
- Never edit `openapi.json` or `generated/` by hand. On a merge conflict in either, resolve the backend sources, then regenerate with `./scripts/api-sync.sh` - never pick one side.
- When the backend API changes: run `./scripts/api-sync.sh`, fix type errors, commit backend change + spec + generated code + the screen that uses them together. CI fails when they drift.
- Handle errors with `isApiError` / `ApiError` from `@/lib/api/client` (`error.httpStatus`, `error.statusName`, `error.message`, `error.fieldErrors`) - the backend error body is `{ code, status, message, errors }`; a rejection that is not an `ApiError` is a network failure.
- Show validation failures (400 with `error.fieldErrors`) next to their fields: match `fieldError.field` to the form field name and show `fieldError.message` as-is (Polish from the backend); errors with `field: null` go above the form.
- Show a failed backend call to the user only through `toUserMessage(error)` from `@/lib/api/client` - never `error.message` or your own text; one wording for the whole app. Error UI: title "Coś poszło nie tak", a full sentence ending with a period, the status as "(błąd N)" at the end, button "Spróbuj ponownie".
- ESLint blocks `fetch` everywhere in `apps/web` except `lib/api/` - do not work around it.
- Never put secrets in the frontend - `NEXT_PUBLIC_*` is public; anything needing a secret goes in the backend.

```tsx
// ✅ Do
// Read the real import path from generated/ after `npm run api:gen` (one folder per API tag).
import { useGetItems } from "@/lib/api/generated/items/items"
// ❌ Don't
const res = await fetch(`${url}/api/1/items`); const items = (await res.json()) as Item[]
```

## Data fetching

Ask: "Does the data on this screen change as a result of a user action (save, filter, refresh)?"

- Yes -> Client Component with the generated TanStack Query hook; render all four states: loading, error, empty, data.
- No -> Server Component that awaits the generated fetch function.
- No backend data -> Server Component.
- Keep `page.tsx` a thin Server Component; put `"use client"` in the feature view file, as low as possible.
- Don't use Server Actions or Cache Components - the backend is Spring; an extra hop adds nothing.

```tsx
// app/items/page.tsx (Server Component)
import { ItemsView } from "@/features/items/items-view"
export default function Page() { return <ItemsView /> }

// features/items/items-view.tsx
"use client"
const { data, isPending, isError } = useGetItems()
if (isPending) return <Skeleton />
if (isError) return <ErrorState />
// Generated hooks return the envelope { data, status, headers }: the body is data.data.
if (data.data.length === 0) return <EmptyState />
return <ItemsList items={data.data} />
```

## Where code goes

- `apps/web/features/<feature-name>/` - view components, feature hooks, pure logic and its tests.
- `apps/web/app/` - routing files only (`page.tsx`, `layout.tsx`); they import from `features/`.
- `apps/web/lib/` - cross-feature code only (`lib/api`, `lib/env.ts`).
- Shape every feature like the example in "Data fetching" above: thin `page.tsx`, one view file with the four states, pure logic in its own file with a test.
- Don't change `packages/ui` without agreeing with the other frontend person - it is shared.
- Add shadcn components only with `npx shadcn@latest add <name> -c apps/web` (from `frontend/`) - never by hand.

## UI library (shadcn `base-nova` on `@base-ui/react`, NOT Radix)

- Import components as `@workspace/ui/components/<name>`, `cn` from `@workspace/ui/lib/utils`, icons from `lucide-react`.
- Use `render` instead of `asChild` - `asChild` does not exist.
- Set `nativeButton={false}` when `render` swaps a button for a non-button element.
- Style state with `data-open:` / `data-closed:` / `data-disabled:` - not `data-[state=open]:`.
- Primitive part names differ from Radix: Dialog `Backdrop`/`Popup`, Accordion `Panel`, Select `Positioner` around `Popup`. The wrappers keep shadcn names (`DialogContent`, `SelectContent`); pass `side`/`align` to `SelectContent`.
- `Select` `onValueChange` may receive `null` - handle it. Accordion uses boolean `multiple` and array values.
- Type primitive props as `Primitive.Part.Props`; use `useRender`/`mergeProps`, not `Slot`.
- Tailwind 4 is CSS-first: no `tailwind.config`; theme tokens are in `packages/ui/src/styles/globals.css`.
- Use Sonner (`toast` from `sonner`) for notifications - the only toast system.

```tsx
// ✅ Do
<DialogClose render={<Button variant="outline" />}>Anuluj</DialogClose>
<Button render={<a href="/pomoc" />} nativeButton={false}>Pomoc</Button>
// ❌ Don't
<DialogClose asChild><Button>Anuluj</Button></DialogClose>
```

## Language and accessibility

- Write all user-visible text in Polish; code, identifiers, comments, commits and docs in English.
- Vendored components contain English `sr-only`/`aria-label` strings (e.g. "Close" in Dialog and Sheet) - translate them when you use the component.
- Give every form field a visible label.
- Make everything work by keyboard; never remove focus styles.
- Use the provided components, never clickable `div`s; keep their contrast.
- Give every image an `alt`.

## Verify in the browser

- After any visual change: run `npm run dev`, open `http://localhost:3000` with the Claude in Chrome, Browserbase or other chrome tools, check every state you touched, read the console for errors, and report what you saw.
