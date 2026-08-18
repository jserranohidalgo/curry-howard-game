# AGENTS.md

Instructions for any AI agent (or human) working in this repository. Harness-neutral; no tool-specific assumptions.

## Project

`curry-howard-game` is the design (and, in time, the implementation) of a game that teaches logic and programming as a single activity through the Curry–Howard correspondence: a play is at once the construction of a natural-deduction proof and the type-driven development of a program that inhabits a type. The authoritative design document is [`spec/specification.md`](spec/specification.md) — in particular the §3 rule tables, which define the legal moves. The source material it draws on (Scala teaching notebooks) and the UI mockups live under [`doc/`](doc/).

## At the start of a session

Before doing anything else, read these files to get oriented:

- **[`README.md`](README.md)** — the big picture: what the game is, the goal, the principles we work by, and the assets we start from. No plan and no tasks.
- **[`Roadmap.md`](Roadmap.md)** — the workplan: the phases to a first working application, each with its dependencies and exit criteria. Narrative context, no granular tasks.
- **[`ToDo.md`](ToDo.md)** — the simple issue tracker: what is pending and what is done. This is what we are actively doing right now.

As you work, keep `ToDo.md` current: add new pending items, and move finished items to **Done** with a completion date. Changes to the plan — a phase resequenced, an exit criterion met, a risk realised — belong in `Roadmap.md`; shifts in the goal or the principles belong in `README.md`.

## Persist your results

Do not leave substantive results only in the conversation. Analyses, surveys, findings, and decisions belong in the repository, as OKF docs in the area they concern. Persist the *full* result, not a condensed version; if a chat summary is richer than what you wrote to a file, the file is wrong. This is the analyze-once principle applied to every kind of output.

## How to work here

Two habits that this project has paid for the hard way. Both are harness-neutral: they describe what to do, not what to do it with.

### Check it, do not reason about it

**A claim about behaviour goes into the repository only after it has been observed.** Not deduced from the code, not inferred from a diff — run it.

- **Engine work** is checked by tests, and the interesting ones are properties rather than examples: the parser round-trips in both notations, the save round-trips over random plays with jumps, the game never refutes a theorem, what the game finishes LJT proves. `sbt engineJVM/test`.
- **UI work is checked by driving a real browser.** Build with `sbt distDev`, serve `dist/` (`python3 -m http.server`), start a browser with remote debugging enabled, then script it over the DevTools protocol: click the actual controls, type into the actual field, read back the DOM, and capture screenshots. The scripts are throwaway and belong in a scratch directory, not in the repository.

This is not ceremony. It has caught, among others: a move rendering `v1 match` instead of `qr match`, because the effect was built outside the selected hole's naming environment; a stylesheet block that silently failed to apply; a rules-table row shifting by two pixels mid-game, which is exactly what Phase 8's exit criterion forbids; a dead-end toast left standing underneath the verdict card; and a `let` rendered where Scala does not allow one. None of those was visible by reading.

Two things that will cost you an hour if you rediscover them: a browser may refuse the debugging socket unless remote origins are allowed, and typing into a Laminar-rendered input needs the native value setter plus a synthetic `input` event — assigning `element.value` changes the DOM and tells the application nothing.

### One screen at a time

Interface work goes **screen by screen**: build one, show it, take the correction, commit, and only then start the next. Corrections tend to arrive as a cropped screenshot and one sentence, and they are almost always about a case the happy path never reaches — a discharge label pointing at something no longer on screen, a selection frame drawn around the *missing* derivation instead of the goal it belongs to. Fix them in the same pass rather than filing them: they are cheap while the screen is fresh.

When the design and the built application disagree, see the standing instruction under Phase 8 in [`Roadmap.md`](Roadmap.md).

## This repository is an Open Knowledge Format bundle

The **whole repository** is an [Open Knowledge Format (OKF)](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) bundle: markdown files with YAML frontmatter, versioned alongside the material they describe. The **bundle root is the repository root**, so bundle-relative links start from there (e.g. `/src/thing.md`, `/Roadmap.md`).

- **Reserved files** (may appear per directory): `index.md` — table of contents / progressive disclosure; `log.md` — chronological provenance. **Every other `*.md` is a concept document** and must carry a `type` in its frontmatter.
- **Top-level knowledge docs:** [`index.md`](index.md), [`README.md`](README.md), [`Roadmap.md`](Roadmap.md), [`ToDo.md`](ToDo.md), this file, and [`CLAUDE.md`](CLAUDE.md).
- **Outside the knowledge layer:** dot-directories hold tooling and VCS state, not knowledge — `.git/`, `.okf/` (hooks, installer, `concept-template.md`), and any harness config (`.claude/`, etc.). OKF consumers should ignore them, and their `*.md` files are **not** concept documents.

## Core workflow: analyze once, never re-preprocess

The knowledge layer is built **incrementally and on demand** — never in a bulk preprocessing pass. When you analyze a source:

1. **Check for an existing concept doc first.** By convention it is a sibling of the source at the mirrored path — e.g. `src/foo.md` describes `src/foo/`. If it exists and covers what you need, **read it instead of re-reading the raw source.** This is the whole point: preprocessing a source is expensive, so it is done exactly once and the result is committed.
2. **If none exists,** analyze the source and write a concept doc from `.okf/concept-template.md`, placed as a sibling of the source it describes.
3. **Register it:** add a one-line pointer under the `<!-- okf:concepts -->` marker in the relevant `index.md`.

### Concept document shape

Frontmatter — `type` is the only required field (a free descriptive string, e.g. `Academic Paper`, `Design Note`, `Meeting Summary`, `API Reference`). Recommended: `title`, `description`, `resource` (bundle-relative path to the source it describes), `tags`, `timestamp` (ISO 8601). Body sections are all optional — see `.okf/concept-template.md` for the full template. Cross-link concepts with bundle-relative paths starting from the repo root.

## Provenance log (automated)

New files added under the watched area are recorded in a `log.md` automatically by a git pre-commit hook. The hook is tracked at `.okf/hooks/pre-commit`; each clone installs it once:

```sh
sh .okf/install.sh
```

Do not hand-edit the dated entries in `log.md`; let the hook maintain them. (Configure the watched directory and log path at the top of the hook.)

## Be permissive (per the OKF spec)

While the bundle is partial, expect and tolerate: missing concept docs, unknown `type` values, broken links, and an absent/stale index. Never reject or "repair" the bundle over these — they are the normal state of an incrementally built knowledge layer.
