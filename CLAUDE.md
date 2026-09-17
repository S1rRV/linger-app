# CLAUDE.md

Project guidance for Claude Code. [README.md](README.md) covers what Linger is;
the product specification lives in `docs/`.

## House style

No em dashes in prose.

## Agent skills

### Issue tracker

Issues live as GitHub issues in `S1rRV/linger-app`, through the `gh` CLI locally
and the GitHub MCP tools in cloud sessions, where `gh` is absent. See
`docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, with the default label strings (`needs-triage`,
`needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and one `docs/adr/` at the repo root, both
created lazily by `/domain-modeling` rather than upfront. See
`docs/agents/domain.md`.

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

Done. `/setup-matt-pocock-skills` has been run, and wrote the three config files
the other skills read: `docs/agents/issue-tracker.md`,
`docs/agents/triage-labels.md` and `docs/agents/domain.md`, summarised under
"Agent skills" above.

Edit those files directly to change tracker, labels or layout. Re-running the
skill is only needed to switch issue trackers or start over.

`CONTEXT.md` and `docs/adr/` are deliberately not created here. `/domain-modeling`
writes them lazily, when a term or a decision actually gets resolved.

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
