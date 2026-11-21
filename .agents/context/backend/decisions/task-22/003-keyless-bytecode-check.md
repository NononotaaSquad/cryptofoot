# D22-003: Keyless bytecode check: public JSON-RPC, Blockchair fallback

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision
- **Source:** brief KROK 3.2 and KROK 1.4 ("bytecode != 0x → smart contract, not a private account"); owner requirement: free / keyless only
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U., grilling 2026-11-21

## Options considered
1. Etherscan `eth_getCode` proxy. Rejected: it needs an API key.
2. A public Ethereum JSON-RPC endpoint (`eth_getCode`, no key).
3. Option 2 plus Blockchair's Ethereum address dashboard (`type: account|contract`, no key) as a fallback (chosen).

## Decision
- **Primary:** `POST https://ethereum-rpc.publicnode.com` with `eth_getCode(address, "latest")`. `0x` → account; a delegation designator → account (D22-004); anything else → contract.
- **Fallback**, only when the primary is unavailable: `GET https://api.blockchair.com/ethereum/dashboards/address/{address}`. `context.code` must be 200; `type` `account` → account; `contract` → contract (unless `contract_code_hex` is a delegation designator); anything else → unavailable.
- **Unavailable** = timeout (5 s connect and request), connection error, non-200 HTTP status, JSON-RPC error, or an unusable body. Each is logged at WARN with the source name and never turned into a result.
- Configurable: `app.exclusion.eth-rpc-url`, `app.exclusion.blockchair-url`, `app.exclusion.timeout` (`ExclusionProperties`). Requests carry a generic `User-Agent` and only the address.

## Consequences and risks
- Public endpoints have no SLA. When both fail, the ETH address is `unclassified` (D22-001): the Analysis becomes `PARTIAL`, never wrong.
- Blockchair already refused this machine (HTTP 430) during development, so the fallback must not be relied on alone.
- One request per unlisted ETH address, sent sequentially. That is fine for a handful of candidates per Analysis; switch to JSON-RPC batching if volume grows.

## Verification
`ContractCodeSourcesSpec` (local stub HTTP server: account, contract, delegation, JSON-RPC error, HTTP 503, non-JSON, null result, unreachable host, Blockchair refusal). Live case in `ExclusionClassifierWiringSpec` (`EXCLUSION_LIVE_CHECK=true`): Uniswap V2 Router → `SMART_CONTRACT`; passed against the real endpoint on 2026-11-21.

## Reconsider when
The KAS environment provides its own Ethereum node: point `eth-rpc-url` at it and drop the fallback.
