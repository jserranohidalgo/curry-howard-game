---
type: Task Tracker
title: ToDo
description: Simple issue tracker for curry-howard-game — pending and completed actions.
tags: [tasks, tracker]
timestamp: 2026-08-14
---

# ToDo

A simple issue tracker for `curry-howard-game`: what we are doing and what we have done. Newest items at the top of each section. These tasks come from the ten-phase workplan in [Roadmap.md](Roadmap.md); the goal and principles behind that plan are in [README.md](README.md).

Conventions:
- `- [ ]` pending · `- [x]` done. Move an item from **To do** to **Done** when finished, and append the completion date `(YYYY-MM-DD)`.
- Keep entries short and action-shaped. Longer discussion belongs in [Roadmap.md](Roadmap.md).

## To do

Active focus: **Phase 0 — decide the design space**. No code until it closes; see [Roadmap.md](Roadmap.md) for each decision's options, default and downstream effect.

**Phase 0 — Decide the design space**

- [ ] D1–D3 Product shape: which platforms ship first · hybrid or native if mobile is in scope · hosting, backend-or-not, and offline/PWA
- [ ] D4–D7 State and data: stateful across sessions? · what is saved (position vs. the whole tree) · shareable goals/plays by URL · does the course need to collect student work?
- [ ] D8–D11 Stack: engine language (TypeScript vs. Scala.js) · UI framework · one package or `engine/` + `web/` · property-based test tooling
- [ ] D12–D15 First-release scope: both views or programmer first · does the negative ending ship · EN+ES at launch · free-form goals and/or curated puzzles
- [ ] D16–D18 Institutional: URJC branding approval and real assets · licence and repository home · support matrix, accessibility target, and the course deadline
- [ ] Record the answers — D1–D7 and D12–D15 in [README.md](README.md), D8–D11 and D16–D18 in [Roadmap.md](Roadmap.md) — and revise Phases 1–9 where an answer diverges from its default

**Phase 1 — Foundations**

- [ ] Scaffold the `engine/` package (pure, no UI imports) and the `web/` app, with a test runner and linting
- [ ] Bring the URJC tokens from `doc/mockup/design/urjc/tokens/` in as the app's real token layer (not a second layer over legacy names)
- [ ] Vendor the URJC logo assets and add Lucide as a package rather than inline SVG

**Phase 2 — Engine core**

- [ ] Port `doc/mockup/design/logic.jsx`: types, terms, holes, scope, `legalMoves`, `applyMove`, game tree
- [ ] Strip i18n out of the engine — emit structured move descriptors, translate at the edge
- [ ] Write an independent type checker `check(term, ctx)` and use it to verify every term the engine builds
- [ ] Keep the stored state serializable if D4/D5 call for persistence (a `Move` currently carries a `build()` closure)
- [ ] Test: reproduce the §4.9 distributivity playthrough move for move
- [ ] Re-sugar `A ⟶ ⊥` as `¬a` in the logician printer

**Phase 3 — Goal parser**

- [ ] Port `doc/mockup/design/parser.jsx` (both notations, negation desugaring, positioned errors, `puzzleFromType`)
- [ ] Test: `parse(print(t))` ≡ `t` round-trip property in both notations

**Phase 4 — Playable vertical slice**

- [ ] Wire Home → Setup → Play end to end, programmer view, minimal styling, win ending only

**Later phases** (see [Roadmap.md](Roadmap.md) for scope and exit criteria)

- [ ] Phase 5 — search-path tree, jump-to-node backtracking, dead-end toast, confirm dialog
- [ ] Phase 6 — the negative ending: bound the move space, detect non-productive cycles, decide a theorem/non-theorem corpus
- [ ] Phase 7 — logician view: the ND derivation renderer and the notation switch
- [ ] Phase 8 — recreate the designed UI across all screens against the reference screenshots
- [ ] Phase 9 — EN/ES, accessibility, deploy, classroom trial

**Independent of the phases**

- [ ] Analyze `doc/4.1 CurryHoward.ipynb` into a concept doc → `doc/4.1 CurryHoward.md` (mine it for puzzles and framings)
- [ ] Analyze `doc/3.4 Isomorphisms.ipynb` into a concept doc → `doc/3.4 Isomorphisms.md`
- [ ] Write §5 "Logics and languages" of [spec/specification.md](spec/specification.md)
- [ ] Write §6 "Platforms" of [spec/specification.md](spec/specification.md) — most of it is Phase 0's D1–D3 answers, the rest is what Phase 9 learns
- [ ] Request the URJC *Manual de identidad visual* and the real logo files from the Dirección de Comunicación (D16)

## Done

- [x] Restructure: goal and approach to [README.md](README.md), the workplan alone to [Roadmap.md](Roadmap.md), and a Phase 0 that decides the design space before any code (2026-08-15)
- [x] Draft the phased workplan to a first IPL application in [Roadmap.md](Roadmap.md) (2026-08-14)
- [x] Add the design handoff under `doc/mockup/` and analyze it into [doc/mockup.md](doc/mockup.md) (2026-08-14)
- [x] Set up the repository as an OKF knowledge bundle (`AGENTS.md`, `index.md`, `Roadmap.md`, `ToDo.md`, `log.md`, `.okf/`, provenance hook) (2026-08-14)
- [x] Write §§1–4 of [spec/specification.md](spec/specification.md): goal, the two parallel rule tables, the game mechanics, and the worked playthroughs (2026-08-14)
