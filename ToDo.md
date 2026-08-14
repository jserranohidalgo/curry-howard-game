---
type: Task Tracker
title: ToDo
description: Simple issue tracker for curry-howard-game — pending and completed actions.
tags: [tasks, tracker]
timestamp: 2026-08-15
---

# ToDo

A simple issue tracker for `curry-howard-game`: what we are doing and what we have done. Newest items at the top of each section. These tasks come from the ten-phase workplan in [Roadmap.md](Roadmap.md); the goal and principles behind that plan are in [README.md](README.md).

Conventions:
- `- [ ]` pending · `- [x]` done. Move an item from **To do** to **Done** when finished, and append the completion date `(YYYY-MM-DD)`.
- Keep entries short and action-shaped. Longer discussion belongs in [Roadmap.md](Roadmap.md).

## To do

Active focus: **Phase 1 — Foundations**. Phase 0 is closed; the decisions are recorded in [Roadmap.md](Roadmap.md), and the scope they set for the first release is in [README.md](README.md).

**Open from Phase 0**

- [ ] Confirm whether the copyright holder is you or URJC, and correct the line in [LICENSE](LICENSE) if it is the university
- [ ] Re-evaluate the 10 September date at the Phase 4 checkpoint (end of August) and reschedule or reduce scope if it is not closing

**Phase 1 — Foundations**

- [ ] sbt build: `engine` as a Scala 3 cross-project (JVM + JS), `web` as a Scala.js/Laminar app, with ScalaCheck on the JVM side
- [ ] Bundling and dev server for the Laminar app; formatting and linting
- [ ] Stand up the offline/PWA shell early — service worker plus cache manifest, verified with the network off
- [ ] Bring the URJC tokens from `doc/mockup/design/urjc/tokens/` in as the app's real token layer (not a second layer over legacy names)
- [ ] Vendor the URJC logo assets and the Lucide glyphs as plain SVG

**Phase 2 — Engine core**

- [ ] Re-implement `doc/mockup/design/logic.jsx` in Scala 3 as sealed hierarchies: types, terms, holes, scope, `legalMoves`, `applyMove`, game tree
- [ ] Make moves data rather than closures, so game state can be serialized (D4/D5)
- [ ] Key a node's children by the (hole, move) pair that produced them — the fix that makes Phase 6's exhaustion sound
- [ ] Strip i18n out of the engine — emit structured move descriptors, translate at the edge
- [ ] Write an independent type checker `check(term, ctx)` and use it to verify every term the engine builds
- [ ] Test: reproduce the §4.9 distributivity playthrough move for move
- [ ] Test: ScalaCheck properties for well-typedness and for game-tree serialization round-trip
- [ ] Re-sugar `A ⟶ ⊥` as `¬a` in the logician printer

**Phase 3 — Goal parser**

- [ ] Re-implement `doc/mockup/design/parser.jsx` in the engine module (both notations, negation desugaring, positioned errors, `puzzleFromType`)
- [ ] Return error codes and positions rather than translated strings
- [ ] Test: `parse(print(t))` ≡ `t` round-trip property in both notations

**Phase 4 — Playable vertical slice**

- [ ] Wire Home → Setup → Play end to end in Laminar, programmer view, minimal styling, win ending only

**Later phases** (see [Roadmap.md](Roadmap.md) for scope and exit criteria)

- [ ] Phase 5 — search-path tree, jump-to-node backtracking, dead-end toast, confirm dialog, and local persistence of the whole tree
- [ ] Phase 6 — the negative ending, finite case: sound exhaustion, a regression test against false refutation, a decided corpus
- [ ] Phase 7 — logician view: the ND derivation renderer and the notation switch
- [ ] Phase 8 — recreate the designed UI across all screens against the reference screenshots
- [ ] Phase 9 — ES/EN, static deploy, classroom trial

**Independent of the phases**

- [ ] Analyze `doc/4.1 CurryHoward.ipynb` into a concept doc → `doc/4.1 CurryHoward.md` (mine it for puzzles and framings)
- [ ] Analyze `doc/3.4 Isomorphisms.ipynb` into a concept doc → `doc/3.4 Isomorphisms.md`
- [ ] Write §5 "Logics and languages" of [spec/specification.md](spec/specification.md)
- [ ] Write §6 "Platforms" of [spec/specification.md](spec/specification.md) — most of it is Phase 0's D1–D3 answers, the rest is what Phase 9 learns
- [ ] Optional (D16): request the URJC *Manual de identidad visual* and the real logo files from the Dirección de Comunicación — arriving before Phase 8 makes replacing the substitute typefaces a one-file change, afterwards a re-do

## Done

- [x] Licence: MIT, added as [LICENSE](LICENSE) (2026-08-15)
- [x] Set the release date: Thursday 10 September 2026, the course start, with scope kept and the Phase 4 checkpoint as the reschedule trigger (2026-08-15)
- [x] Close Phase 0 — decide the design space (D1–D19), record the answers in [README.md](README.md) and [Roadmap.md](Roadmap.md), and revise Phases 1–9 against them (2026-08-15)
- [x] Restructure: goal and approach to [README.md](README.md), the workplan alone to [Roadmap.md](Roadmap.md), and a Phase 0 that decides the design space before any code (2026-08-15)
- [x] Draft the phased workplan to a first IPL application in [Roadmap.md](Roadmap.md) (2026-08-14)
- [x] Add the design handoff under `doc/mockup/` and analyze it into [doc/mockup.md](doc/mockup.md) (2026-08-14)
- [x] Set up the repository as an OKF knowledge bundle (`AGENTS.md`, `index.md`, `Roadmap.md`, `ToDo.md`, `log.md`, `.okf/`, provenance hook) (2026-08-14)
- [x] Write §§1–4 of [spec/specification.md](spec/specification.md): goal, the two parallel rule tables, the game mechanics, and the worked playthroughs (2026-08-14)
