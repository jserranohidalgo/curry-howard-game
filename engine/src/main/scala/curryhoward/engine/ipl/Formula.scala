package curryhoward.engine
package ipl

/** A proposition, which is to say a type — the same object read two ways.
  *
  * The connectives of specification §3: `⊤`/`Unit`, `⊥`/`Nothing`, `∧`/product,
  * `∨`/sum, `→`/function, plus atoms. Negation is not primitive: `¬a` is
  * `a → ⊥`, which is how the parser desugars it and how [[Notation]] sugars it
  * back.
  *
  * The two readings live in [[Notation]] as two functions over this one type.
  * They cannot drift, because there is nothing for them to drift from.
  * Structural equality comes free with the case classes, which is all the rules
  * ever need of a formula.
  */
enum Formula:
  case False
  case True
  case Atom(name: String)
  case Implies(a: Formula, b: Formula)
  case And(a: Formula, b: Formula)
  case Or(a: Formula, b: Formula)

  infix def ==>(b: Formula): Formula = Implies(this, b)
  infix def /\(b: Formula): Formula = And(this, b)
  infix def \/(b: Formula): Formula = Or(this, b)

  /** `¬a` is `a → ⊥`. */
  def not: Formula = Implies(this, False)

object Formula:

  def atom(name: String): Formula = Atom(name)

  /** Sugar for writing formulae in tests and examples: `"A".atom ==> "B".atom`. */
  object Syntax:
    extension (s: String) def atom: Formula = Atom(s)
