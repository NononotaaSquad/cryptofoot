# D22-007: Wiring, classification source text, Mentions kept

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal (implementation detail) + owner answer on recording the interaction
- **Source:** D24-005 (orchestrator not yet wired); `dto/Responses.kt` `ExcludedAddressResponse(reason, classificationSource, justification)`; brief KROK 1.3 ("do not assign it as the person's private wallet, but record the fact of the interaction"); E5.3
- **Proposed by:** AI agent (Claude) · **Approved by:** the "Mentions kept" part by Maksym U., grilling 2026-11-21; the rest by nobody yet

## Decision
- `ExclusionClassifierService` is a plain class built by `config/ExclusionConfig` as the `ExclusionClassifier` bean, with the label directory and the ordered list of `ContractCodeSource`s (empty when switched off). The orchestrator stays a plain class (D24-005); whoever wires it injects this bean.
- `classificationSource` is a readable provenance string, in English like the other source names:
  - `Etherscan labels (github.com/brianleect/etherscan-labels, MIT, scraped 2023-06-18): Kraken 4 [kraken]`
  - `Own label list (<reference URL>): Binance.com`
  - `Ethereum JSON-RPC eth_getCode (ethereum-rpc.publicnode.com): executable code present`
- An Excluded Address keeps its candidate with all its `mentionIds`, so the report can show where the person published or referred to the exchange, mixer, fundraiser or contract (E5.3) without the address ever becoming an Attribution.
- No REST contract change: `ExcludedAddressResponse` already carries `reason`, `classificationSource` and `justification`. No api-sync needed.

## Reconsider when
E5 wants the label name and the source as separate fields: split the string and run api-sync.
