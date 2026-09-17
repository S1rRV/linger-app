# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

**This repo is single-context.** One `CONTEXT.md` and one `docs/adr/` at the
root. There is no `CONTEXT-MAP.md` and no per-package split, because there are
no workspace or monorepo signals here.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root, or
- **`CONTEXT-MAP.md`** at the repo root if it exists: it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

Neither `CONTEXT.md` nor `docs/adr/` exists yet. That is expected and is not a
gap to fix upfront.

## File structure

Single-context repo, which is what this repo uses:

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-slug.md
│   └── 0002-slug.md
└── src/
```

Multi-context repo, for reference only (signalled by a `CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← system-wide decisions
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← context-specific decisions
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## Existing documentation in this repo

`docs/` already holds the product specification, written before any code:
`STORYBOARD.md`, `INGESTION.md`, `DATA-MODEL.md`, `STATE-MACHINES.md`,
`REMINDERS.md`, `CALENDAR-AND-PASSES.md`, `ROADMAP.md`.

These are **not** a substitute for `CONTEXT.md` or ADRs, but they are the best
existing source of the project's vocabulary. `DATA-MODEL.md` in particular names
the domain entities (Trip, Booking, TimelineEvent, Reminder, PassArtifact,
IngestionItem, Place) and states five invariants. Read it before proposing terms,
and prefer its language over synonyms.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
