# curry-howard-game

A game that teaches **logic** and **programming** at once, through the **Curry–Howard correspondence**: constructing a proof and inhabiting a type are the same activity, so a single play can be read either as *proving a proposition* or as *programming a signature*. It is intended for university courses on logic and declarative programming, and is delivered as a web desktop application and a mobile application.

## Goal

Gamify logical proof systems. The player starts from a single typed **hole** — a signature to inhabit, or equivalently a proposition to prove — and fills it by applying **constructors** and **destructors**, which are exactly the introduction and elimination rules of natural deduction. Play ends when no holes remain (a **positive win**: the program is complete and well-typed, the proof is finished) or when every line of play has been shown to fail (a **negative win**: the type is uninhabited, the proposition is not a theorem).

The project is ultimately **not one game but many**. There are many logics — classical propositional, first- and second-order, linear, separation — and several proof systems for the same logic, and under Curry–Howard each corresponds to a genuinely different programming language. Natural deduction and the sequent calculus for propositional logic are not notational variants; they are different languages.

The **near-term target is one of them**: a working application for **intuitionistic propositional logic (IPL)** ≡ the simply typed λ-calculus with products, coproducts, `Unit` and `Nothing`. The plan to get there is [Roadmap.md](Roadmap.md).

## Approach

**The rule tables are the single source of truth.** §3 of [the specification](spec/specification.md) defines the legal moves; the engine implements exactly those, and every screen is a rendering of a game state — the open holes, and the resources in scope at each — plus the moves legal at it. Where an existing prototype and the specification disagree, the specification wins.

**One engine, two syntaxes.** The programmer and logician views must never become two implementations that can drift. The engine emits structured, notation-free descriptions; rendering to Scala or to a Gentzen derivation happens at the edge, as does translation to EN/ES.

**Engine first, and framework-free.** The engine is a pure module with no UI dependency, developed with its own tests. That is what makes the eventual mobile app, and the eventual second logic, cheap.

**Vertical slice early.** Get an ugly but genuinely playable game running end to end as soon as the engine and parser exist, before any branded UI work. It validates the port, and everything after it is improvement rather than integration.

**Leave room, build nothing.** Additional logics, hints, move evaluation, scoring, accounts and persistence are all out of scope for the first application. Structure for them — a list of one system on the home screen, a reserved slot in the left column — and build none of them.

## Where we stand

Three assets exist, and the workplan is largely a matter of joining them:

- **The rules** — [spec/specification.md](spec/specification.md) §§1–4: the two parallel rule tables (natural deduction ⟷ constructors and destructors of algebraic data types), the game mechanics (holes, moves, resources, dead ends, backtracking, both wins), and worked playthroughs including two negative ones. §5 (logics and languages) and §6 (platforms) are still stubs.
- **The design** — [doc/mockup/](doc/mockup/), analyzed in [doc/mockup.md](doc/mockup.md): a high-fidelity, URJC-branded handoff for the minimal IPL version (Home → Setup → Play, plus Help and a Result overlay), with an interaction spec and reference screenshots of every screen.
- **A working prototype** — inside that same handoff, `design/logic.jsx` and `design/parser.jsx` are a running engine and goal parser in plain JavaScript. The handoff is explicit that these two are to be **ported as logic, not re-derived from prose**; everything else in `design/` is presentation scaffolding to be rebuilt idiomatically.

So the first application is not a green-field build: it is a port, a hardening, and a faithful re-implementation of a UI that has already been designed down to the token.

## Reading order

- [Roadmap.md](Roadmap.md) — the phased workplan to a first working IPL application.
- [ToDo.md](ToDo.md) — what is pending and what is done.
- [spec/specification.md](spec/specification.md) — the design: rules, mechanics, playthroughs.
- [doc/mockup.md](doc/mockup.md) — the design handoff and its prototype.
- [index.md](index.md) — the machine-navigable entry point. If you are an AI agent, start from [AGENTS.md](AGENTS.md).

This repository is organized as an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) bundle — see [AGENTS.md](AGENTS.md) for the conventions.
