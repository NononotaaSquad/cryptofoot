# Verification record, #85 (Case list rows)

Edge (playwright-core) against a local backend on a dedicated Mongo database (`list85`, 13 seeded Cases) and `npm run dev`.

- `desktop.png`: 1440 px. Columns Sprawa / Status / Najwyższy stopień pewności / Ostatnia analiza; blue bar + tint only at a maximum >= 80 (80% blue, 79% not).
- `mobile.png`: 375 px. Cells stack and name themselves; no horizontal overflow, nothing clipped.
- `focus.png`: keyboard focus on the first row link (3 px inset ring).

Checked (all pass): 79/80 highlight edge; 0% with leads is a value and not blue; "Brak powiązań"; RUNNING shows "W toku" and "–" (no earlier 99%) and opens the latest finished Analysis; FAILED shows "–"; a Case without an Analysis shows "Bez analizy" and opens `/cases/{id}`; a result without saved figures is "–", never 0%; long wallet and profile addresses are shortened for the eye and keep the full value for assistive technology; `+N więcej` is the true remainder; no Case Number is shown (field not in the API until #56); Tab reaches the row link, the focus ring is visible, Enter opens the report; no console errors.

Not covered here (other tasks): search, groups, empty states and polling (#86); Case Number (#56, hidden until the API returns it).
