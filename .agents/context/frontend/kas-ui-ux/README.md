# KAS / MF UI-UX reference

Visual language of Krajowa Administracja Skarbowa (KAS), the Ministry of Finance (MF) and gov.pl, collected as a reference for the OŁCyŚ analyst tool. All screenshots were captured on 2026-11-21 with headless Chrome at a 1440 px width (one mobile shot at 390 px), and they are stored in `assets/`. Cookie banners are visible in most shots.

## 1. Official design system: "Gov.pl – wytyczne projektowe" (BETA)

- **URL:** https://aplikacje.gov.pl/app/govpl-front-styleguide/
- **Owner:** KPRM (Chancellery of the Prime Minister).
- **Contents:** guidelines (colors, icons, typography, spacing, grid, language, accessibility) and components (button, input, select, radio, checkbox, datepicker, alert, accordion, breadcrumbs, stepper, loader, uploader). It also covers structures: top bar, footer, menus.
- **Distribution:** the SCSS library is not public. It is requested by email from redakcja@cyfra.gov.pl. HTML snippets can be copied from each component page. No public npm package, GitHub repo or Figma kit was found. The license is not stated.

### Colors (verified in `assets/ds-govpl-styleguide-colors.png`)

| Role | Name | Hex | Notes |
|---|---|---|---|
| Primary action, links | Dark blue | `#0052a5` | 7.6:1 on white; the most used color in live gov.pl and podatki.gov.pl CSS |
| Secondary blue | Light blue | `#006cd7` | 5.1:1 |
| Accent / deep | Navy blue | `#00468d` | additional palette |
| Text | Black | `#1b1b1b` | |
| Secondary text | Gray 1 / Gray 2 | `#656565` / `#767676` | `#767676` is exactly 4.5:1, the minimum for text |
| Borders | Gray 3 / Gray 3a | `#b7b7b7` / `#dadada` | not for text (2:1) |
| Surface | Gray 4 | `#f1f1f1` | |
| Error, destructive | Dark red / Light red | `#a7162d` / `#d5233f` | |
| Warning | Yellow / Yellow bright | `#eba828` / `#ffc605` | |
| Success | Green | `#598527` | |

Contrast rule from the guide: at least 4.5:1 for text and at least 3:1 for UI components and icons.

### Typography (verified in `assets/ds-govpl-styleguide-typography.png`)

- **Font:** gov.pl uses Open Sans in weights 400, 600 and 700, each with an italic.
- **Scale:** 40 / 32 / 28 / 24 / 20 / 16 / 14 / 12 px at a width of 1500 px or more. The base is 16 px = 1 em, and sizes scale fluidly on narrower screens.
- **Line height:** 1.5 for body text and 1.25 for headings. Text is left-aligned, never justified.
- **Per-portal differences:** podatki.gov.pl adds Montserrat for headings. The standalone MF apps (Wykaz podatników VAT, eUrząd Skarbowy) use a Poppins-like face.

### Buttons and spacing

- **Button variants:** Primary, Secondary (outline), Tertiary (text), and red primary and red secondary for destructive actions.
- **Button rules:** use only one primary button per page, and never use a button for navigation (`assets/ds-govpl-styleguide-button.png`).
- **Spacing scale:** 4 / 8 / 10 / 16 / 20 / 24 / 28 / 32 / 40 / 56 / 72 px, with 20 px as the standard gap.

## 2. Recurring patterns across the portals

| Pattern | What it looks like | Screens |
|---|---|---|
| Header | Godło and a wordmark top-left; a white bar with a thin divider | `portal-podatki-home-desktop.png`, `tool-biala-lista-form.png` |
| Side menu | A blue (`#0052a5`) or white left menu with icons | `ds-*`, `tool-eureka.png` |
| Primary button | Solid blue, white bold label, about 4 px radius; secondary is an outline; "Wyczyść" is a text link | `tool-eureka.png`, `tool-biala-lista-form.png` |
| Search form | A radio group picks the criterion and only that input is shown; required fields get `*`; a grey hint sits under the field; the date defaults to today; "Szukaj" sits inline | `tool-biala-lista-form.png` |
| Advanced filters | Collapsible sections ("Podstawowe / Dodatkowe kryteria wyszukiwania") | `tool-eureka.png` |
| List controls | Result count ("Liczba wyników: N"), segmented tabs, sort select, pagination, page-size select | `tool-narzedzia.png`, `portal-podatki-narzedzia.png` |
| Error | A modal with a bold cause, one sentence with the next step, and a single "Rozumiem" button | `tool-biala-lista-blad.png` |
| Help content | Accordions with a chevron on the right | `portal-podatki-epit.png`, `tool-mikrorachunek.png` |
| Legal basis | A footnote block with the act and article under the form | `tool-biala-lista-form.png` |
| Footer | A thin red rule, MF/KAS logos, links (Deklaracja dostępności, Polityka cookies, Klauzula RODO), CC BY 3.0 PL | `portal-podatki-home-desktop.png`, `tool-eureka.png` |
| Copy | Imperative verbs on buttons (Szukaj, Sprawdź, Wyczyść, Rozumiem); informal 2nd person "Ty", not "Pan/Pani" | all |

Each portal has its own variant of this language:
- **gov.pl:** a dense, news-driven platform shell with a left sidebar.
- **podatki.gov.pl:** airy and task-tile oriented.
- **eUrząd Skarbowy:** a minimal page with method-choice cards.
- **PUESC:** its own red identity.
- **EUREKA and KSeF:** an MF red header bar with blue primary buttons.

## 3. Gaps: not observed in public KAS/MF UIs

- No dense data tables, status badges, empty states or print/report layouts were reachable without logging in. The biała lista results need a form submit.
- No precedent exists for a 0–100% confidence indicator. It has to be our own component: show the number together with a text label (e.g. "Wysoki") and do not rely on color alone.
- The raw biała lista API stamps each answer with a `requestId` and `requestDateTime`. This is a useful pattern for stamping a report, but it was not seen in the UI.

## 4. Recommendations for OŁCyŚ

**Borrow:**
- The gov.pl palette and Open Sans, mapped to shadcn/Tailwind tokens: `--primary` = `#0052a5`, `--destructive` = `#d5233f`, borders `#dadada`, muted surface `#f1f1f1`, body text `#1b1b1b`, muted text `#656565`.
- One primary action per screen.
- The biała lista form pattern for creating a Case.
- EUREKA-style collapsible advanced criteria.
- A result count with tabs (Wszystkie / Powiązania / Wzmianki / Wykluczone).
- Modal or inline errors with a cause and a next step.
- A legal-basis footnote on the report.

**Avoid:**
- Hero photos, carousels and promo banners.
- Large task-tile grids, which are too sparse for analyst work.
- The PUESC and EUREKA red themes, which are separate identities.

**Confirm with the organizers (open question):**
- Whether we may use the KAS sign or the godło. The KAS sign is protected (art. 4 of the KAS act; Dz.U. 2017 poz. 305). The lead decided on 2026-11-21 to show the KAS sign in the header of the demo (see `../views.md`, Header); the godło is not used. The permission is still to be confirmed with the organizers.

**Accessibility:**
- The ustawa o dostępności cyfrowej (Dz.U. 2019 poz. 848) points to WCAG 2.1. Level AA is stated only by secondary sources, so it is unverified in the statute text.
- Design for AA anyway: keyboard operation, contrast as given above, no information conveyed by color alone.

## Sources

- Design system: https://aplikacje.gov.pl/app/govpl-front-styleguide/ (pages: `dokumentacja/wytyczne/kolory`, `typografia`, `odstepy`, `komponenty/button`, `dokumentacja/jak-otrzymac-biblioteke`)
- Live CSS: https://www.gov.pl/css/main-ff1e87c8f81.css, https://www.podatki.gov.pl/bootstrap/css/style.css
- KAS sign: https://eli.gov.pl/api/acts/DU/2017/305/text.html
- Accessibility: https://www.gov.pl/web/dostepnosc-cyfrowa
- Portals and tools: podatki.gov.pl, gov.pl/web/kas, gov.pl/web/finanse, urzadskarbowy.gov.pl, puesc.gov.pl, wykazpodatnikowvat.mf.gov.pl, eureka.mf.gov.pl, ksef.podatki.gov.pl, ap.ksef.mf.gov.pl, ppuslugi.mf.gov.pl

## Screenshot index (`assets/`)

| File | Shows |
|---|---|
| `ds-govpl-styleguide-colors.png` | gov.pl palette with hex codes |
| `ds-govpl-styleguide-typography.png` | Open Sans weights and size scale |
| `ds-govpl-styleguide-button.png` | Button variants, states, side-menu layout |
| `portal-podatki-home-desktop.png` / `-mobile.png` | podatki.gov.pl home, desktop and 390 px |
| `portal-podatki-narzedzia.png` | Finder with filters, tabs, pagination |
| `portal-podatki-epit.png` | Content page with accordions |
| `portal-govpl-kas.png` / `portal-govpl-finanse.png` | gov.pl shell for KAS and MF |
| `portal-eus-landing.png` | eUrząd Skarbowy login method cards (rendered in English and dark mode) |
| `portal-puesc-landing.png` | PUESC landing (red identity, contrast and font-size controls) |
| `tool-biala-lista-form.png` | Wykaz podatników VAT search form |
| `tool-biala-lista-blad.png` | Error modal ("Nie udało się wyszukać podmiotu") |
| `tool-eureka.png` | EUREKA search with collapsible criteria |
| `tool-narzedzia.png` | podatki.gov.pl tools finder |
| `tool-mikrorachunek.png` | Accordion FAQ page |
| `tool-ksef-landing.png` / `tool-ksef-ap.png` | KSeF info portal and app login chooser |
| `tool-ppuslt.png` | Retired-service notice |
