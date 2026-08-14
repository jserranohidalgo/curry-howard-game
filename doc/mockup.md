---
type: Design Handoff
title: Curry–Howard Game — design handoff (mockup)
description: High-fidelity UI/UX handoff for the minimal IPL version of the game, plus a working JavaScript prototype of the engine and goal parser.
resource: /doc/mockup
tags: [design, ui, ux, prototype, engine, urjc, ipl]
timestamp: 2026-08-14
---

# Curry–Howard Game — design handoff (mockup)

The design handoff for the **minimal, codeable version** of the game: three screens (Home → Setup → Play) plus Help and a Result overlay, covering exactly one system — intuitionistic propositional logic ≡ the simply typed λ-calculus with algebraic data types. It is high-fidelity (final colours, typography, spacing, states and copy, branded to the **URJC** design system) and it ships a **running React prototype** rather than static comps: opening [`design/Curry-Howard App.html`](/doc/mockup/design/Curry-Howard%20App.html) plays the actual game.

This is what turns [spec/specification.md](/spec/specification.md) from a design into something buildable: the spec fixes the *rules*, this bundle fixes the *interaction*, the *look*, and — in [`design/logic.jsx`](/doc/mockup/design/logic.jsx) — a first executable *implementation* of the rules. Its own README ([`doc/mockup/README.md`](/doc/mockup/README.md)) and interaction spec ([`design/interaction.md`](/doc/mockup/design/interaction.md)) are the primary sources; this doc records what they contain and what we found by reading the code.

# Key concepts

- **Two regions, two tenses.** Play is `356px | 1fr`: the left column is *past and future* (the explored search path, and the rules table of moves available next); the right column is *the present* (goal card, term card, holes, resources in scope). This split is the interaction spec's second principle and shapes the whole screen.
- **The rules table is the syllabus.** The complete I/E table of IPL is *always fully visible*, with fixed cell geometry so it never reflows during play. Applicable moves carry a 3px green left rule, inapplicable ones a red one, and cells where **no such rule exists** (⊥-introduction, ⊤-elimination, hypothesis-introduction) render as a frameless "—": an absence, not a state. A cell with several concrete instances shows a count badge and unfolds in place.
- **Nothing is ever destroyed.** A move creates a *new node* whose parent is the current node; backtracking is just `currentId = someEarlierNode`, and every state explored stays reachable from the search-path tree.
- **One game, two readings.** The view switch (Programmer ⟷ Logician) re-notates goal, term, scope, rules table and help prose — and never changes game state. The term card renders either Scala or a real Gentzen derivation with fraction bars, right-hand rule labels and bracketed discharged assumptions.
- **Three endings** (interaction spec §6.2): *won* (no holes remain) and *lost* ("This is not a theorem!", the whole search space explored) both end the game and return Home; a *dead end* closes only a **branch** and offers a Backtrack toast.
- **Shape language.** Every type has a glyph (pair, sum, function, unit, void, atom crystal), reused across hole chips, scope chips, the goal field and the Home system rows.
- **Scope is per hole.** Each hole carries its own resource list; selecting a hole is what the rules table refers to.
- **Deliberately out of scope:** hints, move evaluation/commentary, scoring, puzzle libraries, accounts, persistence. The left column reserves a slot for Hint and per-move evaluation but builds neither.

# Schema

The prototype's engine data model ([`design/logic.jsx`](/doc/mockup/design/logic.jsx)), which is the thing to port:

| Concept | Shape |
|---|---|
| Type | `{k:'var',name}` · `{k:'unit'}` · `{k:'void'}` · `{k:'prod',a,b}` · `{k:'sum',a,b}` · `{k:'fun',a,b}` |
| Term | `hole` · `lam` · `pair` · `inl` · `inr` · `var` · `unit` · `app` · `proj` · `match` · `absurd` · `let` |
| Hole | `{k:'hole', id, type, scope:[{name,type}]}` — scope is carried **on the hole** |
| Move | `{rule, kind:'con'\|'des', via, forward?, prog, logic, title, blurb, opens, build()}` |
| Node | `{id, parentId, term, move, actedHoleId, status:'open'\|'win'\|'dead', childrenIds, depth}` |
| Game | `{puzzle, nodes, rootId, currentId}` |

App state (per the handoff README): `screen`, `view`, `locale`, `puzzle`, `game`, `selId`, `treeOpen`, `confirm`. Everything is in memory; a refresh restarts. Derived per render: `collectHoles(term)`, `deadIds`, `legalMoves(selected)`, `won`, `lost = nodeExhausted(root)`.

Move generation (`legalMoves`) implements §3.2 of the spec directly: constructors keyed on the hole's type (`⟶.I`, `∧.I`, `∨.I₁`, `∨.I₂`, `⊤.I`; none for `void` or a type variable), and destructors keyed on each in-scope variable (`Ax`, `∧.E₁/₂` both **backward** and **forward**, `⟶.E` both backward and forward, `∨.E`, `⊥.E`).

# Examples

- **Goal grammar** (both notations, freely mixed): `=>` `->` `→` · `(A,B)` `∧` `&` · `Either[A,B]` `∨` `|` · `¬A` `~A` `!A` `not A` (desugars to `A → ⊥`) · `Unit` `⊤` · `Nothing` `⊥` · parentheses · identifiers.
- **Seeded examples** ([`design/parser.jsx`](/doc/mockup/design/parser.jsx)): distributivity, commutativity, the K combinator, transitivity, and excluded middle — the last deliberately *not* provable, so the negative ending is reachable on purpose.
- **Screenshots** in [`screenshots/`](/doc/mockup/screenshots), in the order a player meets them: `01-home-en` · `03-setup-goal-entered` · `04-play-programmer` · `05-play-logician-proof` (the *same* state as a derivation) · `06-play-search-path-open` · `07-confirm-abandon` · `09/10/11-help-*` · `12-home-es`.

# What to port vs. what to rebuild

The handoff is explicit about this, and it matters for the workplan in [Roadmap.md](/Roadmap.md):

- **Port as logic, do not re-derive from prose:** [`design/logic.jsx`](/doc/mockup/design/logic.jsx) (the engine) and [`design/parser.jsx`](/doc/mockup/design/parser.jsx) (the goal parser). Both are pure data + functions with no React dependency and translate almost verbatim into any language. "Getting this right is what makes the game correct."
- **Rebuild idiomatically:** everything else — markup, CSS, the Babel-based script loading, the inline Lucide icon set. In particular, do **not** reproduce the two-layer CSS arrangement (`styles.css`/`screens.css` use legacy token names that `urjc-theme.css` re-points at URJC tokens); use the URJC tokens directly and read `urjc-theme.css` as the specification of each brand decision.
- **Tokens** in [`design/urjc/tokens/`](/doc/mockup/design/urjc/tokens) are copied verbatim from the URJC design system: brand `#e90129`, twelve neutrals, derived status green/amber/blue, radius 0 on cards and 2px on controls, no shadow at rest, motion 80/140/200/320ms.
- **Brand caveats inherited from the design system:** the *Manual de identidad visual* was unavailable, so the typefaces (Archivo / Source Sans 3 / Source Serif 4 / IBM Plex Mono) are documented **substitutes**, and the atom and status colours are *derived*, not brand-sanctioned. Swapping a real corporate face is a one-file change in `urjc/tokens/typography.css`. Logos must never be restyled, recoloured or rotated; no emoji and no Unicode characters as icons (logical symbols are notation, not icons).

# Gaps found in the prototype

Read from the code, not stated in the handoff — these are real work items, not defects of the design:

- **No non-productive-cycle detection.** `nodeExhausted()` treats a node as exhausted when every (hole, move) pair has an exhausted child; its `seen` set guards recursion over a tree that is already acyclic, so it never detects the *recurring goal* of specification §4.10. Excluded middle (finite tree) is refuted correctly; double-negation elimination (infinite tree) is not — the handoff acknowledges this as "an undecidable search simply continues".
- **The move set makes the search space infinite.** Forward projections and forward applications are offered unconditionally, so a player can keep binding the same value forever; `pairs` in `nodeExhausted` therefore counts moves that can always be re-taken, and exhaustion is unreachable in general. Bounding this (subsumption on already-derived bindings, and identifying states up to goal-type + scope) is what makes the negative ending decidable.
- **The engine depends on i18n.** `legalMoves` calls `t()` to build `title`/`blurb`/`ltitle`/`lcode`, coupling pure game logic to the UI's locale. The port should emit structured move descriptors and translate at the edge.
- **No type checker and no tests.** Nothing independently verifies that the terms `build()` produces are well-typed; the win condition trusts move generation.
- **Negation is not re-sugared on output.** `printType` renders `A => Nothing` literally rather than as `¬a` in logician view, though the parser accepts `¬`.

# Relevance

This bundle is the reference for every UI decision and the starting point for the engine; the phased workplan in [Roadmap.md](/Roadmap.md) is built around it. Read [`README.md`](/doc/mockup/README.md) for per-screen specs and [`design/interaction.md`](/doc/mockup/design/interaction.md) for the interaction rules before touching the UI; read `logic.jsx` before touching the engine.

# Citations

- [`/doc/mockup/README.md`](/doc/mockup/README.md) — handoff overview, per-screen specs, tokens, assets, screenshots index.
- [`/doc/mockup/design/interaction.md`](/doc/mockup/design/interaction.md) — principles, screen map, per-screen interaction rules, terminal states.
- [`/doc/mockup/design/logic.jsx`](/doc/mockup/design/logic.jsx) — engine: types, terms, holes, `legalMoves`, `applyMove`, game tree.
- [`/doc/mockup/design/parser.jsx`](/doc/mockup/design/parser.jsx) — dual-notation goal parser and the example list.
- [`/doc/mockup/design/app-min.jsx`](/doc/mockup/design/app-min.jsx) — router, Play screen, `nodeExhausted`.
- Source archive: `Curry-Howard (1).zip`, received 2026-08-14, extracted here in full.
