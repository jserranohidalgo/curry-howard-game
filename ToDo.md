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

Active focus: **Phase 4 — the playable vertical slice**, and the end-of-August checkpoint that goes with it. Phase 1's build is up and verified; Phase 0's decisions are recorded in [Roadmap.md](Roadmap.md), and the scope they set for the first release is in [README.md](README.md).

**Open from Phase 0**

- [ ] Confirm whether the copyright holder is you or URJC, and correct the line in [LICENSE](LICENSE) if it is the university
- [ ] Re-evaluate the 10 September date at the Phase 4 checkpoint (end of August) and reschedule or reduce scope if it is not closing

**Phase 1 — Foundations** (done bar the tail)

- [ ] Add scalafmt, and CI if we want it
- [ ] Vendor the four typefaces as woff2 into `static/fonts/` and declare them in `static/styles/fonts.css` — the design system @imports them from Google Fonts, which contradicts D3 (Phase 8 fidelity item)

**Phase 2 — Engine core**

- [x] Add cats to the engine; the skeleton compiles on Scala 3.3 LTS without `-experimental` (2026-08-15)
- [x] Port `Sequent`, `Rule`, `Calculus`, `SearchSpace`, `Proof`, `SearchStrategy`, `Mu` as the engine skeleton (2026-08-15)
- [x] Instantiate `Form` and `Term`: the ADT interpretations, both notations, and a Scala-source renderer with type-derived names (2026-08-15)
- [x] Write `Calculus[NJ]` — the §3.2 rule set, backward and forward destructors included (2026-08-15)
- [x] Bring LJT across as the reference decision procedure for Phase 6 — Dyckhoff's rules, a terminating `Decide.provable`, and a cross-check that the game never finishes a non-theorem (2026-08-15)
- [x] Restructure into `ipl/` (Formula, Notation, Sequent), `ipl/nj/`, `ipl/ljt/` and `util/` (2026-08-15)
- [ ] `Tree`, and a `Show` for search spaces — deferred until the search-path panel needs it
- [x] Game tree over partial positions, children keyed by the (hole, move) pair that produced them (2026-08-15)
- [x] Write an independent type checker `check(term, ctx)` (2026-08-15)
- [x] Test: reproduce the §4.9 distributivity playthrough move for move (2026-08-15)
- [ ] Keep engine state serializable — moves as data, no closures (D4/D5); hole paths and move indices are already plain data, so this is codecs plus a round-trip property
- [ ] Structured move descriptors, no i18n inside the engine — `label` and `isForward` exist; the Play screen will want premises, bindings and the opened holes too
- [ ] Run *every* engine-built term through the type checker in the property tests, not just the worked examples
- [ ] Test: ScalaCheck properties for well-typedness and for game-tree serialization round-trip
- [x] Re-sugar `A ⟶ ⊥` as `¬a` in the logician printer (2026-08-15)

**Phase 3 — Goal parser** (done 2026-08-15)

- [x] Re-implement `doc/mockup/design/parser.jsx` in the engine (both notations freely mixed, negation desugaring, `Goal.from` for the type parameters)
- [x] Return error codes and positions rather than translated strings
- [x] Test: `parse(print(t))` ≡ `t` round-trip property in both notations, plus both notations agreeing with each other

**Phase 4 — Playable vertical slice**

- [x] A console client (`repl/`) — the same interaction with no design surface, to judge it before building it (2026-08-16)
- [ ] Decide whether the Play screen keeps the REPL's "moves across all holes" or returns to the design's selected-hole model
- [ ] Wire Home → Setup → Play end to end in Laminar, programmer view, minimal styling, win ending only

**Later phases** (see [Roadmap.md](Roadmap.md) for scope and exit criteria)

- [ ] Phase 5 — search-path tree, jump-to-node backtracking, dead-end toast, confirm dialog, and local persistence of the whole tree
- [ ] Phase 6 — the negative ending, finite case: sound exhaustion, a regression test against false refutation, a decided corpus
- [ ] Phase 7 — logician view: the ND derivation renderer and the notation switch
- [ ] Decide, before writing that renderer, how a `let` appears to the logician — inline, explicit cut bar, or cut-during-play-then-normalised (open question in [Roadmap.md](Roadmap.md); the LJ-shaped option is ruled out by D22)
- [ ] Phase 8 — recreate the designed UI across all screens against the reference screenshots
- [ ] Phase 9 — ES/EN, static deploy, classroom trial

**Independent of the phases**

- [ ] Analyze `doc/4.1 CurryHoward.ipynb` into a concept doc → `doc/4.1 CurryHoward.md` (mine it for puzzles and framings)
- [ ] Analyze `doc/3.4 Isomorphisms.ipynb` into a concept doc → `doc/3.4 Isomorphisms.md`
- [ ] Write §5 "Logics and languages" of [spec/specification.md](spec/specification.md)
- [ ] Write §6 "Platforms" of [spec/specification.md](spec/specification.md) — most of it is Phase 0's D1–D3 answers, the rest is what Phase 9 learns
- [ ] Before the repository goes public in Phase 9: relicense the reused parts of [hablapps/tdd](https://github.com/hablapps/tdd/tree/lambdadays25) from CC BY-SA 4.0 to MIT (D20) — same author, so a formality, but it should be on the record
- [ ] Optional (D16): request the URJC *Manual de identidad visual* and the real logo files from the Dirección de Comunicación — arriving before Phase 8 makes replacing the substitute typefaces a one-file change, afterwards a re-do

## Done

- [x] Adopt the coalgebraic engine architecture of [hablapps/tdd](https://github.com/hablapps/tdd/tree/lambdadays25) (D20), analyzed in [doc/tdd-lambdadays25.md](doc/tdd-lambdadays25.md), and revise Phase 2 and Phase 6 around it (2026-08-15)
- [x] Phase 1 — sbt/Scala.js/Laminar build, engine cross-built JVM+JS, no-bundler static assembly, PWA shell verified offline (2026-08-15)
- [x] Licence: MIT, added as [LICENSE](LICENSE) (2026-08-15)
- [x] Set the release date: Thursday 10 September 2026, the course start, with scope kept and the Phase 4 checkpoint as the reschedule trigger (2026-08-15)
- [x] Close Phase 0 — decide the design space (D1–D19), record the answers in [README.md](README.md) and [Roadmap.md](Roadmap.md), and revise Phases 1–9 against them (2026-08-15)
- [x] Restructure: goal and approach to [README.md](README.md), the workplan alone to [Roadmap.md](Roadmap.md), and a Phase 0 that decides the design space before any code (2026-08-15)
- [x] Draft the phased workplan to a first IPL application in [Roadmap.md](Roadmap.md) (2026-08-14)
- [x] Add the design handoff under `doc/mockup/` and analyze it into [doc/mockup.md](doc/mockup.md) (2026-08-14)
- [x] Set up the repository as an OKF knowledge bundle (`AGENTS.md`, `index.md`, `Roadmap.md`, `ToDo.md`, `log.md`, `.okf/`, provenance hook) (2026-08-14)
- [x] Write §§1–4 of [spec/specification.md](spec/specification.md): goal, the two parallel rule tables, the game mechanics, and the worked playthroughs (2026-08-14)
