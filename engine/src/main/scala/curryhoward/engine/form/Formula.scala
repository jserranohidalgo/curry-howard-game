package curryhoward.engine
package form

import cats.Show

/** The initial interpretation of [[Form]]: a plain algebraic data type.
  *
  * This is what the game stores and serializes. The two notations are two
  * `Show`-like renderings of it, not two representations — see
  * [[Notation]].
  */
enum Formula:
  case False
  case True
  case Atom(name: String)
  case Implies(a: Formula, b: Formula)
  case And(a: Formula, b: Formula)
  case Or(a: Formula, b: Formula)

object Formula:

  given Form[Formula] with

    val False: Formula = Formula.False
    val True: Formula = Formula.True

    extension (s: String) def atom: Formula = Atom(s)

    extension (f: Formula)
      infix def implies(b: Formula): Formula = Formula.Implies(f, b)
      infix def and(b: Formula): Formula = Formula.And(f, b)
      infix def or(b: Formula): Formula = Formula.Or(f, b)

      def fold[W](
          `false`: W,
          `true`: W,
          atom: String => W,
          implies: (Formula, Formula) => W,
          and: (Formula, Formula) => W,
          or: (Formula, Formula) => W
      ): W = f match
        case Formula.False      => `false`
        case Formula.True       => `true`
        case Atom(n)            => atom(n)
        case Formula.Implies(a, b) => implies(a, b)
        case Formula.And(a, b)  => and(a, b)
        case Formula.Or(a, b)   => or(a, b)

  /** Default rendering is the logician's. The programmer's is
    * [[Notation.programmer]].
    */
  given Show[Formula] = Notation.logician
