# Jev stories: test scenarios for the Confidence Level with a decision model

Status: design, agreed in a grilling session on 2026-11-22. No code yet. These stories are the base for the golden set (hand-labelled Mentions with an expected Confidence Level range).

Points in this file are a **starting point** (bands from the challenge text: 80 / 60 / 40 / 10, +15 for a second independent service). They will be tuned on the golden set. The request shape shown for Jev is illustrative; read the System One contract before the first real call.

## The rule behind every story

- **Code answers "whose?"**: address detection and validation, Excluded Addresses, the link to the investigated person (identifier, author, profile), independence of sources, the arithmetic of points.
- **Jev answers "what does this text say about the address?"**: the kind of post, whether the author declares the address as their own, whether the address appears in a warning.
- Jev never gets the full name, e-mail or messenger ID of the investigated person, and is never asked "is this person X". It gets the text window and neutral facts computed by code.
- Jev may move the kind of a Premise up or down. It never creates a link to the person that code did not find.
- The unit is a **Premise** (PL: Przesłanka): one address in one Mention, with its assessment. Dismissed Premises stay visible to the Analyst with the reason.
- Each Analysis asks Jev again and saves the answers in its own record. Two Analyses of the same Mentions may differ; the comparison marks this as "model assessment changed, no new Mentions".

## Questions sent for one Premise

One request per Premise, three questions, each answer saved separately.

| Question | Type | Meaning |
|---|---|---|
| `rodzaj_wpisu` (kind of post) | choice | own declaration, sale or settlement, discussion, mass comment |
| `deklaracja` (declaration) | yes/no | The author of the text presents the assessed address as their own. It is about the author of the text, not about the investigated person; code knows separately whether the author is the investigated person. |
| `ostrzezenie` (warning) | yes/no | The text tells readers not to use or not to trust the assessed address. |

Thresholds in code: 0.7 or more is yes, 0.3 or less is no, between them is undetermined.

## Worked example: one Mention, two addresses

Case with the nickname `kryptomarek`.

Mention 1 (Bitcointalk, author `kryptomarek`):

> "Sprzedam koparkę, płatność na mój adres bc1qAAA. Uwaga, ktoś się pode mnie podszywa i podaje bc1qBBB, nie wysyłajcie tam nic."

Mention 2 (GitHub, profile `kryptomarek`):

> "Donations: bc1qAAA"

| Premise | Mention | Address | Assessment |
|---|---|---|---|
| P1 | 1 | bc1qAAA | own address, sale context: counts |
| P2 | 1 | bc1qBBB | warning, address of an impersonator: dismissed |
| P3 | 2 | bc1qAAA | own address on own profile: counts |

Attributions: `bc1qAAA` from P1 and P3 (two independent services, high Confidence Level); `bc1qBBB` from P2 only (Confidence Level 0, Premise shown as dismissed).

Request for P2 (illustrative):

```
state:
  tekst: "Sprzedam koparkę, płatność na mój adres bc1qAAA. Uwaga, ktoś
          się pode mnie podszywa i podaje bc1qBBB, nie wysyłajcie tam nic."
  oceniany_adres: "bc1qBBB"
  autor_jest_badana_osoba: true
  liczba_adresow_we_wpisie: 2
answers:
  rodzaj_wpisu = sale or settlement (confidence 0.71)
  deklaracja   = 0.06
  ostrzezenie  = 0.95   -> 0.7 or more -> Premise dismissed, reason: warning
```

## Stories

### S1. Post of the investigated person without their nickname in the text

- Text: "Sprzedam koparkę, płatność na bc1qAAA", author `kryptomarek`.
- Code: the author is the investigated person, so the link to the person exists.
- Jev: kind = sale, declaration 0.90, warning 0.03.
- Expected: **60**.
- `confidence-1` today: 0 (scoring does not read the author). False negative.

### S2. Another user's comment on the profile of the investigated person

- Text under the profile of `kryptomarek`: "Dzięki za poradnik! Mój adres na napiwki: 0xCCC", author `janek99`.
- Code: the URL is under the profile, but the author is not the investigated person.
- Jev: declaration 0.92 (the author presents the address as their own; the author is someone else).
- Expected: **0**, Premise dismissed, reason "address declared by another person".
- `confidence-1` today: 80. False positive.

### S3. Change of address worded like a warning

- Text: "Nie wysyłajcie już na stary adres, mój nowy to bc1qNEW", author `kryptomarek`.
- Jev for `bc1qNEW`: declaration 0.94, warning 0.07.
- Expected: **80**.
- `confidence-1` today: 0 (the sentence contains "nie wysyłaj"). False negative.

### S4. Jev is not sure

- Text: "kryptomarek coś wspominał o bc1qDDD chyba", author `anon12`.
- Code: nickname in the text, weak link.
- Jev: declaration 0.48 (between the thresholds: undetermined), warning 0.22 (no).
- Expected: undetermined; the Premise counts as discussion (**40**) and is marked "for manual verification".

### S5. Code sees no link to the person, Jev sees a declaration

- The Analyst entered `bc1qEEE` as the Wallet address of the Case. Found text: "Mój portfel: bc1qEEE. Marek Kowalski", author unknown.
- Code: a full name alone never links an address to the person.
- Jev: declaration 0.95.
- Expected: **0**, marked "for manual verification". Jev does not create the link.

### S6. Jev does not answer

- Code falls back to the keyword rules of the version without the model.
- Expected: a Confidence Level from the rules, and a Report Limitation saying the assessment was made without the model.
