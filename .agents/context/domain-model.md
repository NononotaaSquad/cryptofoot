# Domain Model

Agents: read this file with `product.md` before implementing any feature. It describes what exists in the domain, not how it is stored or rendered. Use only names from `glossary.md`. Distilled from the raw materials in `domain/`; when they change, update this file.

Status: empty. Filled by the `hackathon-kickoff` skill at the event.

## Domains

One bullet per domain (bounded context): name and the one responsibility it owns. Every backlog task belongs to exactly one domain.

## Aggregates

Per aggregate: its root, the entities and value objects inside it, and the invariants from `product.md` it protects.

## Lifecycles

States and allowed transitions for each aggregate that has a lifecycle, with who or what triggers each transition.

## Relationships

How aggregates reference each other (by ID across aggregates) and the cardinality.

## Sources

Files in `domain/` this model is based on, and what each contributed.
