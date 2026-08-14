---
type: Index
title: curry-howard-game
description: Root OKF entry point for the curry-howard-game repository.
resource: /
tags: [index, okf, curry-howard, logic, type-driven-development]
timestamp: 2026-08-14
---

# curry-howard-game

This repository is an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) bundle. It holds the design and (eventually) the implementation of a game that teaches logic and programming as one activity, via the Curry–Howard correspondence.

This `index.md` is the machine-navigable entry point. The bundle root is the repository root; every `*.md` outside dot-directories is an OKF document.

## Orientation (read these first)

- [Roadmap.md](/Roadmap.md) — the big picture: goal, approach, themes, known gaps. Narrative, no tasks.
- [ToDo.md](/ToDo.md) — the simple issue tracker: what is pending and what is done.
- [AGENTS.md](/AGENTS.md) — how to work in this repo (the OKF workflow and conventions).
- [README.md](/README.md) — short human-facing overview.

## Knowledge base

- [`spec/`](/spec/specification.md) — the game specification: goals, the two parallel rule tables (natural deduction ⟷ algebraic data types), the game mechanics, worked playthroughs, the intended family of logics/languages, and target platforms.
- [`doc/`](/doc/) — source material and design artefacts: the Scala teaching notebooks (`3.4 Isomorphisms.ipynb`, `4.1 CurryHoward.ipynb`) that the specification draws on, and [`doc/mockup/`](/doc/mockup/) for UI mockups.

<!-- okf:concepts -->

## Bundle conventions

- **Reserved files:** `index.md` (this file / per-directory tables of contents) and `log.md` (chronological provenance). Every other `*.md` is a **concept document** carrying a `type` in its frontmatter.
- **Outside the knowledge layer:** dot-directories hold tooling and VCS state, not knowledge — `.git/`, `.okf/` (OKF tooling: hooks, installer, concept template), and any harness config (`.claude/`). OKF consumers should ignore them.
- **Provenance:** file additions under the watched area are logged automatically to `log.md` by the `.okf/hooks/pre-commit` git hook (install once per clone with `sh .okf/install.sh`).

Per the OKF spec, consumption is permissive: missing concept docs, unknown `type` values, broken links, and partial indexes are all expected while the bundle grows.
