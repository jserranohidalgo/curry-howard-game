# Handoff: The Curry–Howard Game

## Overview

A single-player teaching game for a university logic / functional-programming course.
The player is given a **goal** — a type signature to inhabit (programmer view) or a
proposition to prove (logician view) — and fills its **typed holes** by applying
constructors/destructors, which are exactly the introduction/elimination rules of
propositional intuitionistic logic. Completing the program *is* completing the proof.

The design covers the minimal, codeable version: **Home → Setup → Play**, plus **Help**
and a **Result** overlay. Interaction rules are specified in `design/interaction.md`,
which is the companion document to this README and should be read alongside it.

Branding follows the **URJC design system** (Universidad Rey Juan Carlos).

## About the Design Files

The files in `design/` are **design references created in HTML/React-via-Babel** —
prototypes showing intended look and behaviour, **not production code to copy directly**.

The task is to **recreate these designs in the target codebase's existing environment**
(React, Vue, Svelte, SwiftUI, native, …) using its established patterns, component
library and build tooling. If no environment exists yet, choose the most appropriate
framework and implement there.

Two important exceptions, which *are* worth porting as logic rather than re-derived:

- **`design/logic.jsx`** — the game engine (type model, term/hole model, legal-move
  generation, move application, game tree, terminal-state detection). It is pure data +
  functions with no React dependency and translates almost verbatim into any language.
  Getting this right is what makes the game correct; do not reimplement it from prose.
- **`design/parser.jsx`** — the goal parser for both notations, with the same property.

Everything else (markup, CSS, the Babel-based script loading, the inline SVG icon set)
is presentation scaffolding to be rebuilt idiomatically.

## Fidelity

**High-fidelity.** Final colours, typography, spacing, states and copy — all sourced
from the URJC design system. Recreate the UI pixel-perfectly using the codebase's
existing libraries, substituting the URJC tokens in `design/urjc/tokens/` for whatever
token layer the codebase already has.

One caveat inherited from the design system itself: the URJC *Manual de identidad
visual* was not available, so **the typefaces are documented substitutes**
(Archivo / Source Sans 3 / Source Serif 4 / IBM Plex Mono). If the real corporate
typeface is obtained, swapping it is a one-file change in `urjc/tokens/typography.css`.
Likewise the three atom colours and all status colours are *derived*, not brand-sanctioned.

---

## Screens / Views

### 1. HOME

**Purpose:** explain what the game is in three lines and let the player pick a system.

**Layout:** single centred column, `max-width: 620px`, page padding `54px 40px 80px`,
left-aligned and flush. Background `--grey-50` (`#f7f7f8`), no pattern or texture.

**Components, top to bottom:**

| Component | Spec |
|---|---|
| Hero mark | `urjc/assets/logo-urjc.svg` (crowned U isotype), 54×54, margin-bottom 26px |
| Title `h1` | copy `home.title`; Archivo 800, 33px/1.24, `-0.022em`, colour `#000` |
| Red rule | `::after` on the title — 72px × 3px, `#e90129`, margin-top 16px |
| Tagline | copy `home.tagline`; Archivo 600, 22px/1.34, `-0.01em`, colour `--grey-800`, max-width 40ch |
| Lede | copy `home.lede.prog` / `home.lede.logic` (switches with the view); Source Sans 3 19px/1.62, colour `--grey-800`, max-width 66ch, margin-bottom 38px |
| Section label | copy `home.choose` ("Start game"); Archivo 700, 12px, uppercase, `0.10em`, colour `--grey-600` |
| System list | 5 rows, `display: flex; flex-direction: column; gap: 8px` |
| Footer | 1px `--grey-200` top border, 24px above, holding the "How to play" ghost button |

**System row (`.start-btn`)** — `display:flex; align-items:center; gap:15px; padding:15px 18px;`
white surface, 1px `--grey-300` hairline, **border-radius 0**, and a **3px left rule**
that carries the state:

- **Active** (only *Propositional intuitionistic logic*): left rule `#e90129`, full
  opacity, right-hand `arrow-right` icon in red, clickable → SETUP.
  Hover: `translateY(-2px)` + `--shadow-2`.
- **Inactive** (the other four): left rule `--grey-200`, `opacity: .62`, `cursor:not-allowed`,
  `disabled`, and a pill tag reading `home.soon.tag` ("Later" / "Próximamente") —
  10px uppercase `0.1em`, 1px `--grey-300` border, `border-radius: 999px`, padding `3px 9px`.
  Hover lifts opacity to `.78` only.

Row internals: a fixed `min-width: 68px` glyph cluster on the left (shape glyphs for the
first two rows, a mono `∀∃` / `∀¬` / `⊗⊸` symbol at 17px `--grey-600` for the rest), then
the logic name (Archivo 600, 15px) over the language name (11.5px, `--grey-600`, 1.5).

**The five systems** (logic ≡ the language whose programs are its proofs):

| Logic | Language | State |
|---|---|---|
| Propositional intuitionistic logic | Simply typed λ-calculus with algebraic data types | **active** |
| Propositional classical logic | λμ-calculus — first-class continuations (call/cc) | later |
| First-order intuitionistic logic | Dependently typed λ-calculus (λΠ) | later |
| First-order classical logic | Dependent λμ-calculus | later |
| Multiplicative linear logic | Linear λ-calculus — every resource used exactly once | later |

### 2. SETUP

**Purpose:** obtain a well-formed goal. Reached only from Home.

Same 620px sheet. Back ghost-button (`arrow-left` + `nav.back`) 26px above an `h2`
(`setup.title.prog` / `setup.title.logic`, Archivo 700 28px) carrying the same 72px red rule,
then a muted lede.

**Goal input** — full-width row, 1.5px `--grey-300` border, radius 2px, white, inner
`input` in IBM Plex Mono 14.5px with 14px 12px padding. Focus: border `#e90129` +
`box-shadow: 0 0 0 3px rgba(233,1,41,.35)`. When the goal parses, a shape glyph for the
parsed type appears at the right end of the field.

**Live feedback area** (`min-height: 86px`):
- *Empty:* syntax hint grid, `repeat(auto-fit, minmax(168px, 1fr))`, 11.5px `--grey-600`,
  each token in a `.kbd` chip — implication, conjunction, disjunction, negation, `Unit`/`Nothing`.
- *Invalid:* `circle-alert` icon + message + " (at position N)", all `--urjc-red-dark`.
- *Valid:* two rows, tagged `PROGRAMMER` and `LOGICIAN`, echoing the **same parsed goal in
  both notations** — this is the core teaching moment of the screen.

**Begin game** — primary button, full width, height 52px, `#e90129`, white Archivo 600 17px,
radius 2px, disabled at 42% opacity until the goal parses. `Enter` also submits.

**Examples** — 5 rows above a hairline; each fills the field with the example written in the
*current* notation. Notes: distributivity · commutativity · the K combinator · transitivity ·
excluded middle (deliberately **not** provable, so the negative ending is reachable on purpose).

**Accepted grammar** (both notations, freely mixed):
`=>` `->` `→` implication (right-assoc) · `(A, B)` `∧` `&` conjunction ·
`Either[A,B]` `∨` `|` disjunction · `¬A` `~A` `!A` `not A` negation (desugars to `A → ⊥`) ·
`Unit` `⊤` · `Nothing` `⊥` · parentheses · identifiers (upper-cased in prog view, lower in logic view).

### 3. PLAY

**Purpose:** where the whole search happens. Two regions, `grid-template-columns: 356px 1fr`.

**Top bar** (60px, white, 1px bottom border): URJC ETSII lockup 30px tall · thin divider ·
live counters ("N open holes", "N moves") · spacer · **view switch** · **language switch**.
The view switch is a squared segmented control whose active tab is marked by a 3px red
**underline** (no pill, no fill). The language switch is a 2-button EN/ES group,
1px border, radius 2px, mono 13px, active tab on `--grey-100`.

**Left column — the past and the future.**
- *Search path*, collapsible (chevron; collapsed by default). Header shows "N nodes · depth D".
  Expanded: an indented node tree; each node is a mono 11.5px row with a status dot,
  the rule name, and `Nh` / `✓ solved` / `✗ dead`. The current node has a **3px red left bar**.
  Clicking any node jumps to it — that *is* backtracking.
- *Rules table* — the complete I/E table of IPL ≡ the constructors/destructors of the ADTs,
  **always fully visible**. `grid-template-columns: 88px 1fr 1fr; gap: 7px`.
  Rows: `=>` `( , )` `Either` `Unit` `Nothing` `hyp`; columns *Construct / Destruct*
  (relabelled *Introduction / Elimination* in logician view).
  - Every cell is one fixed-height row (`min-height: 36px`) showing **only the rule name**
    (`→I`, `∧E₂`, `Ax`, …) so the table geometry never changes as you play.
  - **Applicable:** white surface, 1px `--grey-200`, **3px left rule `#1e7b3c` (green)**,
    rule name in `#000` Archivo 600. Hover → green border + `--green-tint` wash.
  - **Not applicable:** same frame on `--grey-50`, **3px left rule `#8c0018` (red)**,
    name in `--grey-600` regular. Disabled.
  - **No such rule exists** (⊥-introduction, ⊤-elimination, hypothesis-introduction):
    frameless "—" in `--grey-300`. An absence, not a state.
  - **Several instances:** a red circular count badge at the top-right and a subtle stacked
    -card shadow; clicking unfolds the concrete instances *in place* as a small list
    (each showing its title and its code/logic form), which then applies on click.
- Footer: `Backtrack` (disabled at the root) · `Restart` · `Cancel`, all ghost buttons.

**Right column — the present.** Scrollable canvas, plain background.
- *Goal card*: white, radius 0, 1px hairline, **3px red top rule** (the brand's featured-card
  treatment), 18px 22px padding. Eyebrow left ("Signature to inhabit" / "Proposition to prove"),
  "Your goal" right in red. The signature renders as Scala (`def solution[P, Q, R]: …`) or the
  proposition renders in Source Serif 4 19px.
- *Term card*: white, radius 0, hairline, 26px 30px padding, horizontally scrollable.
  - **Programmer view:** the partial program as Scala in IBM Plex Mono 15.5px/1.9.
  - **Logician view:** the *same term* as a Gentzen natural-deduction derivation — real
    fraction bars (1.4px `--grey-800`), the rule name to the right of each bar in mono 11px
    red, discharged assumptions in brackets with a superscript label. `let`-bindings inline
    at their use sites so the derivation reads correctly. `.nd-prem { gap: 58px }` and
    `padding-right: 58px` reserve space for the absolutely-positioned rule labels.
- *Holes*: inline chips — dashed 1.5px `--grey-300` border, white, radius 2px, showing the
  hole's **shape glyph** + `… : Type`. Exactly one is selected: solid `#e90129` border +
  the red focus ring. A dead hole is struck through with a `--grey-400` border on `--grey-100`
  with `--urjc-red-dark` text — deliberately *not* the same red-outline treatment as selection.
- *Hint line*: "Selected hole ⟨glyph⟩ **Type** — pick a rule on the left."
- *Resources in scope*: chip row, `repeat(auto-fill, minmax(230px, 1fr))`. Each chip shows the
  resource's glyph (atom crystal for type variables), its name in mono 600, its type, and a
  kind label (pair/sum/function ↔ conjunction/disjunction/implication).

### 4. HELP

620→860px sheet. Back button, `h2` + red rule, lede, then **six numbered steps**, each a
27px red circular numeral, a title, prose that follows the selected view, and a small
still built from the *real* UI components (a hole chip, a scope chip, two table cells,
a breadcrumb). Step 6 lists the three endings as bordered rows with `check` / `undo-2` / `x`
icons. Closes with a two-column glossary pairing the two vocabularies.

### 5. RESULT (overlay)

Centred card, max-width 430px, white, radius 0, `--shadow-3`, 30px padding, over a
`rgba(20,20,22,.62)` scrim (no blur). A 46px circular icon — green `check` for the win,
red `x` for the refutation — then the verdict, an explanatory paragraph, and a single
full-width primary **New game** button returning to HOME.

---

## Interactions & Behavior

- **Navigation.** HOME is the only hub: Back from Setup and Help, Cancel from Play
  (confirmed), New game from Result all return to it. There is no Play → Setup path.
- **Selecting a hole** sets which hole the rules table refers to; the table recomputes.
- **Applying a move** creates a *new node* in the game tree whose parent is the current
  node — nothing is ever destroyed, so every earlier state stays reachable.
- **Backtracking** is just `currentId = someEarlierNode`.
- **Cancel / Restart** open a confirm dialog: title, body, `Keep playing` (ghost) /
  destructive action (red). `Esc` dismisses, never confirms. No other action confirms.
- **Terminal states**, evaluated after every move:
  1. *No holes remain* → **won** → Result overlay → New game.
  2. *Some hole has no applicable move* → **dead end** — a toast offering Backtrack. This
     closes a **branch**, not the game.
  3. *The whole finite search space has been explored* → **lost** → Result overlay reading
     **"This is not a theorem!"**. Computed by `nodeExhausted()`: a node is exhausted if it
     is a dead end, or if every (hole, move) pair already has a child and all children are
     exhausted; a cycle guard stops infinite recursion, and an undecidable search simply
     continues.
- **View switch** re-notates everything — goal, term, scope, rules table, help prose and
  illustrations — and **never** changes game state.
- **Language switch** swaps EN/ES across the whole interface including engine-generated
  move labels and parser errors; it also sets `document.documentElement.lang`.
- **Motion.** 140ms controls, 200ms cards, 320ms overlays, `cubic-bezier(.2,0,.2,1)`.
  Fades and 2px translations only. **Never let an entrance animation leave content at
  `opacity: 0`** if it may not run — that bug bit this design twice.

## State Management

```
screen    : 'home' | 'setup' | 'help' | 'play'
view      : 'program' | 'proof'          // global, persists across screens
locale    : 'en' | 'es'                  // global
puzzle    : { goal: Type, tyParams: string[], binder: string } | null
game      : { nodes: Record<id, Node>, rootId, currentId }
  Node    : { id, parentId, term, move, actedHoleId, status, childrenIds, depth }
selId     : id of the selected hole (falls back to the first open hole)
treeOpen  : boolean
confirm   : { title, body, label, act } | null
```

Derived per render: `holes = collectHoles(term)`, `deadIds`, `moves = legalMoves(selected)`,
`won = status === 'win'`, `lost = nodeExhausted(root)`.

No data fetching, no persistence, no accounts. Everything is in memory; a refresh restarts.

## Design Tokens

All tokens are copied verbatim into `design/urjc/tokens/`. The highlights:

**Colour** — brand `#e90129`, hover `#b80020`, press `#8c0018`, washes `#fdeaee` / `#f9c3cd`.
Neutrals `#ffffff #f7f7f8 #f0f0f1 #e2e2e4 #c9c9cd #a3a3a9 #7d7d84 #666666 #4a4a4f #2a2a2d #141416 #000000`.
Derived status: green `#1e7b3c` (+ `#e8f4ec`), amber `#b57200` (+ `#fdf3e0`), blue `#14507d` (+ `#e8f0f6`).
Focus ring `0 0 0 3px rgba(233,1,41,.35)`. Scrim `rgba(20,20,22,.62)`.
Atom palette (this design's own, derived from the status hues): P amber, Q blue, R green.

**Typography** — display/UI **Archivo** 400–800; body **Source Sans 3** 17/26;
quotes/derivations **Source Serif 4**; code, types and figures **IBM Plex Mono**.
Scale 64 / 48 / 36 / 28 / 22 / 18 / 19 / 17 / 15 / 13 / 12. Tracking −0.02em display,
−0.01em headings, +0.10em overlines. Measure 66ch (46ch for quotes).

**Spacing** — 4 8 12 16 20 24 32 40 48 64 80 96; gutter 24; page max 1240; page pad 32;
section 64; card pad 20; control heights 32 / 42 / 52; tap min 44.

**Radius** — 0 on cards, panels and bands; 2px on controls; 999px pills only.
Nothing is rounder than 4px.

**Elevation** — `--shadow-1` 0 1px 2px rgba(20,20,22,.08); `--shadow-2` 0 2px 8px rgba(20,20,22,.10);
`--shadow-3` 0 12px 32px rgba(20,20,22,.16). Cards have **no shadow at rest** — hairlines separate.

**Motion** — 80 / 140 / 200 / 320ms on `cubic-bezier(.2,0,.2,1)`.

## Assets

- `urjc/assets/logo-urjc.svg` — crowned-U isotype (Home hero, 54px).
- `urjc/assets/logo-etsii.svg` — horizontal URJC + ETSII lockup (top bar, 30px tall).
- `-white.svg` variants of both, for dark surfaces.
  Rules: never restyle, recolour, outline or rotate; clear space ≥ the crown's height;
  minimum on-screen height 24px. For another centre, request that centre's file from the
  Dirección de Comunicación rather than editing the lockup.
- **Icons:** Lucide (the URJC system's documented substitute) — 24×24 grid, 2px stroke,
  round caps, outline only, `currentColor`. Vendored inline in `design/icons.jsx` because
  remote CSS masks were blocked in the preview sandbox; in a real app, use the codebase's
  Lucide package. Glyphs used: `arrow-left arrow-right chevron-down chevron-right check x
  circle-alert undo-2 rotate-ccw git-branch sun moon`.
- **No emoji, and no Unicode characters used as icons** — a hard rule of this brand.
  (Logical symbols `∧ ∨ → ⊤ ⊥ ∀ ∃ ⊗ ⊸` are *notation*, not icons, and are correct.)
- No photography or illustration is used anywhere.

## Screenshots

`screenshots/` holds reference captures of every screen, in the order a player meets them:

| File | What it shows |
|---|---|
| `01-home-en.png` | Home, English — title, tagline, the five systems with only IPL active |
| `03-setup-goal-entered.png` | Setup with the distributivity goal typed, showing the dual-notation echo and the enabled Begin button |
| `04-play-programmer.png` | Play, programmer view — Scala term with a selected hole, rules table mid-game, scope chips |
| `05-play-logician-proof.png` | Play, logician view — the *same* state as a natural-deduction derivation |
| `06-play-search-path-open.png` | Play with the search path expanded, showing explored nodes |
| `07-confirm-abandon.png` | The Cancel confirmation dialog |
| `09-help-top.png` `10-help-steps.png` `11-help-glossary.png` | Help, scrolled through its six steps and the glossary |
| `12-home-es.png` | Home in Spanish — the same layout with the full ES translation |

Note the rules table across `04` and `05`: green left rules mark applicable moves, red mark
inapplicable ones, and the cell geometry is identical in both — that stability is intentional.

## Files

Everything lives in `design/`:

| File | What it is |
|---|---|
| `Curry-Howard App.html` | Entry point — open this to see the whole thing |
| `interaction.md` | **Interaction spec** — screen map, per-screen rules, terminal states |
| `logic.jsx` | **Game engine** — types, terms, holes, legal moves, game tree. Port this |
| `parser.jsx` | **Goal parser** for both notations + the example list. Port this |
| `app-min.jsx` | Router, Play screen, exhaustion detection, global state |
| `screens.jsx` | Home, Setup, Help, Result overlay, confirm dialog, top bar |
| `panels.jsx` | Goal banner, scope chips, the rules table, search path / tree |
| `views.jsx` | The two renderings of a term: Scala code and the ND derivation |
| `shapes.jsx` | The "a type has a shape" glyph language + colour-aware type text |
| `i18n.jsx` | EN/ES dictionaries and `t()` |
| `icons.jsx` | Inline Lucide glyph set |
| `styles.css` | Base layout and component styles (legacy token names) |
| `screens.css` | Home / Setup / Help / overlay styles |
| `urjc-theme.css` | **The URJC layer** — maps every legacy token onto a URJC token, then applies the brand's structural signatures. Read this to see the brand decisions in one place |
| `urjc/tokens/*.css` | The URJC token files, copied verbatim from the design system |
| `urjc/assets/*.svg` | The logo files |

### A note on the CSS structure

`styles.css` and `screens.css` were written before the brand was applied and use generic
token names (`--panel`, `--ink`, `--r`, `--sh-2`); `urjc-theme.css` re-points all of them at
URJC tokens. **Do not reproduce this two-layer arrangement** — it is an artefact of how the
design evolved. In the real codebase, use the URJC tokens directly and treat
`urjc-theme.css` as the specification of what each brand decision should be.
