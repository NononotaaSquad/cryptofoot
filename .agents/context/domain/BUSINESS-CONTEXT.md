# OŁCyŚ – glossary

A tool for a KAS (National Revenue Administration) official. It connects public traces on the internet with the cryptocurrency addresses of an investigated person and rates how much this connection can be trusted.

Each term has a Polish label. The application interface is in Polish and uses these labels.

## Language

**Analyst** (PL: Analityk):
A KAS official without IT knowledge, the only user of the application. Works without login.
_Avoid_: user, operator, admin

**Case** (PL: Sprawa):
The object created for one investigated person from their identifiers (full name, nickname, login, e-mail, messenger ID, profile address, cryptocurrency wallet address). It needs at least one narrowing identifier; a full name alone is not enough. Later Analyses return to the Case.
_Avoid_: investigation, project

**Wallet address** (PL: Adres portfela):
A cryptocurrency address entered by the Analyst as an identifier of the Case (one address, Bitcoin or Ethereum). It is the Analyst's own input, not a finding from a source: it is never a Mention, never an independent confirmation and does not by itself make an Attribution.
_Avoid_: address candidate, found address

**Analysis** (PL: Analiza):
One run of search and scoring for a Case, saved in its history. Started manually (priority) or on a schedule.
_Avoid_: run, scan, pass

**Mention** (PL: Wzmianka):
A place in a public source where the data of the investigated person or a cryptocurrency address appeared.
_Avoid_: trace, hit, finding

**Attribution** (PL: Powiązanie):
A pair: the investigated person and a cryptocurrency address, with a Confidence Level and a Justification. It is a lead for further verification, not evidence.
_Avoid_: match, link, assignment

**Confidence Level** (PL: Stopień pewności):
A number from 0 to 100% that says how likely it is that the address belongs to the investigated person.
_Avoid_: Confidence Score, scoring, result

**Justification** (PL: Uzasadnienie):
References to the Mentions that the Confidence Level is based on. It points to sources; it is not a text description.
_Avoid_: premises, explanation, written description

**Excluded Address** (PL: Adres wykluczony):
An address that must not be connected with a person (exchange, smart contract, mixer, fundraiser). It has a Confidence Level of 0% and a stated reason.
_Avoid_: false positive, reject

**Address Activity** (PL: Aktywność adresu):
What an address from an Attribution did on the blockchain: volume, swaps, signals of exit to traditional currencies (Cash-Out).
_Avoid_: indicators, metrics

## Relationships

- A **Case** has one investigated person and many **Analyses**.
- An **Analysis** collects many **Mentions** and produces zero or more **Attributions**.
- An **Attribution** is based on one or more **Mentions** and has one **Justification**.
- An **Excluded Address** does not create an **Attribution**.
