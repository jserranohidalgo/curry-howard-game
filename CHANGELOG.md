# Changelog

The version numbers say where the game is on its own roster: **the minor number
counts the games**, so `0.1.x` is the simply typed lambda calculus alone, `0.2.0`
is the day the next one lands, and `1.0.0` waits until at least two of them are
here and stable.

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
