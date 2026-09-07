# Changelog

The version numbers say where the game is on its own roster: **the minor number
counts the games**, so `0.1.x` is the simply typed lambda calculus alone, `0.2.0`
is the day the next one lands, and `1.0.0` waits until at least two of them are
here and stable.

## 0.1.2 — 2026-09-07

One fix: the goal field's name and a long formula no longer crowd each other.
The name never wrapped or grew, but it still sat beside the formula at full
width; hovering or focusing the field now collapses it to nothing, by a
transition rather than by unmounting it, so the input still never jumps on its
own.

## 0.1.1 — 2026-09-05

A catalogue twice the size, the rules table in each reading's own words, and
four fixes to what was on the screen.

**Sixty-six goals to play, and every one of them named.** The catalogue grew from
thirty-five: the named argument forms (*modus ponens* and *tollens*, the
syllogisms, the constructive dilemma, the *praeclarum theorema*), the laws of the
propositional algebra, the axioms of the intermediate logics (Gödel–Dummett's
linearity, Jankov's weak excluded middle, Gödel's chain), *consequentia
mirabilis*, and four named fallacies. Every entry's classification is checked by
the game's own two oracles rather than taken from a source, and **the name of
what you typed now appears beside the goal field** — matched however you spell
the letters, and saying what the formula *is*, never how it comes out. Where the
formulas came from is in the credits.

**The rules table speaks each reading's own language.** The cells were labelled
`⟶.I` and `∧.E₁` in both columns of both readings; the programmer's are now the
type's own mark and which side of it the rule is — `=>C`, `=>D`, `(,)D₁`,
`EitherD` in Scala, `→C` and `⊕C` in Lean, `->C` and `ResultD` in Rust. **Hover a
cell and the whole rule appears**: the natural-deduction figure to the logician,
the rewrite as code to the programmer, in the language on the switch. A cell that
offers several moves shows one card per move.

**Scala's hole is `???`.** It was `…`, which is the specification's notation for
a gap and not the language's — and every other language now writes its own too.

**Four fixes.** A `case` branch that returns what it assumed no longer renders
empty in the line-for-line reading. The idiomatic reading writes Scala 3's
braceless `match`, and its partial-function literal where a function's whole body
matches its own argument. *Back up* over a forward move returns to the position
before it, rather than to the half-finished binding inside it. And a search-tree
row on a line already lost says so instead of reading as though work remained.

## 0.1.0 — 2026-09-03

The first public release: one game, two talks and a tutor.

**The game.** Intuitionistic propositional logic and the simply typed lambda
calculus with algebraic data types. Type a goal in either notation or take one
from a catalogue of thirty-five classified goals, then fill the holes. Four
levels of help — *Blind*, *Legal*, *Prune*, *Guided* — changeable in the middle
of a game. A clock of 3, 5, 10 or 20 minutes, or none at all. Both endings: a
finished program, or a proof that there is none.

**Two readings, and neither is the real one.** One switch turns the whole screen
between the programmer's reading and the logician's, and changes nothing about
the game. Programs can be read as Scala, Haskell, OCaml, Rust or Lean; proofs as
a Gentzen figure or as numbered lines.

**Two talks**, inside the application, at
[#correspondence](https://jserranohidalgo.github.io/curry-howard-game/#correspondence)
and [#guide](https://jserranohidalgo.github.io/curry-howard-game/#guide): what
the correspondence is, and how the game is played.

**Both languages.** Spanish and English, throughout.

**It works offline.** No account, no server, nothing sent anywhere. The page
installs as an app and plays with no network; a game survives a closed laptop
with its whole explored tree.

**The tutor.** A Claude Code plugin that opens the game in your own browser and
follows the position, answering questions about what is in front of you — and
never about whether your goal can be proved.
