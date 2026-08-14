---
type: Task Tracker
title: ToDo
description: Simple issue tracker for curry-howard-game — pending and completed actions.
tags: [tasks, tracker]
timestamp: 2026-08-14
---

# ToDo

A simple issue tracker for `curry-howard-game`: what we are doing and what we have done. Newest items at the top of each section. For the big-picture rationale behind these tasks, see [Roadmap.md](Roadmap.md).

Conventions:
- `- [ ]` pending · `- [x]` done. Move an item from **To do** to **Done** when finished, and append the completion date `(YYYY-MM-DD)`.
- Keep entries short and action-shaped. Longer discussion belongs in [Roadmap.md](Roadmap.md).

## To do

Active focus: **Step 1 — complete the specification**, with early work on **Step 2 — UI/UX design** (see [Roadmap.md](Roadmap.md)).

- [ ] Draft UI mockups for a game state (holes, per-hole scope, legal-move palette, viewpoint switch) → `doc/mockup/`
- [ ] Write §5 "Logics and languages" of [spec/specification.md](spec/specification.md): per logic/proof system, the corresponding language, the moves exposed, and the rendering of both viewpoints
- [ ] Write §6 "Platforms" of [spec/specification.md](spec/specification.md): technical and UX requirements for the web desktop app and the mobile app
- [ ] Analyze `doc/4.1 CurryHoward.ipynb` into a concept doc → `doc/4.1 CurryHoward.md`
- [ ] Analyze `doc/3.4 Isomorphisms.ipynb` into a concept doc → `doc/3.4 Isomorphisms.md`
- [ ] Decide the implementation stack and how much of the engine is shared between web and mobile → record the decision in [Roadmap.md](Roadmap.md)

## Done

- [x] Set up the repository as an OKF knowledge bundle (`AGENTS.md`, `index.md`, `Roadmap.md`, `ToDo.md`, `log.md`, `.okf/`, provenance hook) (2026-08-14)
- [x] Write §§1–4 of [spec/specification.md](spec/specification.md): goal, the two parallel rule tables, the game mechanics, and the worked playthroughs (2026-08-14)
