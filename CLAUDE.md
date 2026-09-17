# CLAUDE.md

Project guidance for Claude Code. [README.md](README.md) covers what Linger is;
the product specification lives in `docs/`.

## House style

No em dashes in prose.

## Vendored agent skills

`.claude/skills/` contains 25 skills vendored from
[mattpocock/skills](https://github.com/mattpocock/skills), copied at commit
`74ca5fe` (2026-09-17).

They are committed as real files rather than installed as a plugin, because
Claude Code cloud sessions (claude.ai/code and the mobile Code tab) clone this
repository and do not carry locally installed plugins or marketplaces.

### What was copied

| Source directory | Skills | Included |
| --- | --- | --- |
| `skills/engineering/` | 18 | yes |
| `skills/productivity/` | 7 | yes |
| `skills/in-progress/` | 9 | no |
| `skills/misc/` | 4 | no |
| `skills/deprecated/` | 0 | no |

The category level is flattened, so each skill sits at
`.claude/skills/<skill-name>/SKILL.md`. Sidecar files travel with each skill
(`LOGIC.md`, `DEEPENING.md`, `issue-tracker-*.md`, `template.sh`, `agents/`,
`scripts/` and so on) because the `SKILL.md` files read them at runtime and
break without them.

### Updating

```
npx skills update
```

Then re-check that only `skills/engineering/` and `skills/productivity/` landed
in `.claude/skills/`, and that nothing arrived as a symlink into `.agents/`.
The upstream installer writes real files to `.agents/skills/` and symlinks
`.claude/skills/` at them, plus a `skills-lock.json`; this repo deliberately
keeps neither, so a plain `npx skills update` needs that cleanup afterwards.

Nothing is installed globally and nothing in `~/.claude/` is touched.

### One-time setup

Run `/setup-matt-pocock-skills` once. It generates the project context files the
other skills read, under `docs/agents/`: `CONTEXT.md`, `CONTEXT-MAP.md`,
`domain.md`, `issue-tracker.md` and `triage-labels.md`. Several skills degrade
without them.

### Name collision to be aware of

The vendored `code-review` skill shares a name with Claude Code's built-in
`/code-review`. Within a session the project-scoped one wins, so `/code-review`
here invokes Matt Pocock's version, not the built-in reviewer.

### Invocation

Fourteen skills are user-invoked only (`disable-model-invocation: true`) and must
be called by name:

`/ask-matt` `/grill-me` `/grill-with-docs` `/handoff` `/implement`
`/improve-codebase-architecture` `/setup-matt-pocock-skills` `/teach`
`/to-questionnaire` `/to-spec` `/to-tickets` `/triage` `/wait-what` `/wayfinder`

Eleven can be triggered automatically by Claude when the task matches:

`code-review` `codebase-design` `diagnosing-bugs` `domain-modeling` `grilling`
`prototype` `research` `resolving-merge-conflicts` `tdd` `wizard`
`writing-for-agents`

These skills are third-party and run with full agent permissions. Review a
`SKILL.md` before relying on it.
