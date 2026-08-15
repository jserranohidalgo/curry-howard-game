package curryhoward.engine
package form

/** Formulae, tagless-final.
  *
  * A proposition and a type are the same thing read two ways, so the engine
  * never commits to either syntax: it works against this interface, and the two
  * notations of the game are two interpretations of it. That is what makes
  * "one engine, two syntaxes" structural rather than a discipline.
  *
  * The connectives are those of specification §3: `⊤`/`Unit`, `⊥`/`Nothing`,
  * `∧`/product, `∨`/sum, `→`/function, plus atoms. Negation is not primitive —
  * `¬a` is `a → ⊥`, as the parser already desugars it.
  *
  * Adapted from `hablapps/tdd`, whose `Form` has no `True`: the game's type
  * language includes `Unit`, which has an introduction rule (`⊤I`) and no
  * elimination, so it has to be here.
  */
trait Form[F]:

  val False: F
  val True: F

  extension (p: String) def atom: F

  extension (a: F)
    infix def implies(b: F): F
    infix def and(b: F): F
    infix def or(b: F): F

    def not: F = a implies False

    /** The eliminator: every observation of a formula goes through this, so an
      * interpretation only has to say what each connective means.
      */
    def fold[W](
        `false`: W,
        `true`: W,
        atom: String => W,
        implies: (F, F) => W,
        and: (F, F) => W,
        or: (F, F) => W
    ): W

object Form:

  def apply[F](using F: Form[F]): Form[F] = F

  object Atom:
    def unapply[F: Form](f: F): Option[String] =
      f.fold[Option[String]](None, None, Some(_), (_, _) => None, (_, _) => None, (_, _) => None)

  object False:
    def unapply[F: Form](f: F): Boolean =
      f.fold[Boolean](true, false, _ => false, (_, _) => false, (_, _) => false, (_, _) => false)

  object True:
    def unapply[F: Form](f: F): Boolean =
      f.fold[Boolean](false, true, _ => false, (_, _) => false, (_, _) => false, (_, _) => false)

  object Implies:
    def unapply[F: Form](f: F): Option[(F, F)] =
      f.fold[Option[(F, F)]](None, None, _ => None, (a, b) => Some((a, b)), (_, _) => None, (_, _) => None)

  object And:
    def unapply[F: Form](f: F): Option[(F, F)] =
      f.fold[Option[(F, F)]](None, None, _ => None, (_, _) => None, (a, b) => Some((a, b)), (_, _) => None)

  object Or:
    def unapply[F: Form](f: F): Option[(F, F)] =
      f.fold[Option[(F, F)]](None, None, _ => None, (_, _) => None, (_, _) => None, (a, b) => Some((a, b)))

  /** Structural equality, derived from `fold` so that it works for any
    * interpretation. Rules need it: `Ax` fires only when a resource's type *is*
    * the goal, and the backward eliminations only when a component is.
    */
  def eqv[F: Form](x: F, y: F): Boolean = (x, y) match
    case (Atom(p), Atom(q))               => p == q
    case (True(), True())                 => true
    case (False(), False())               => true
    case (Implies(a, b), Implies(c, d))   => eqv(a, c) && eqv(b, d)
    case (And(a, b), And(c, d))           => eqv(a, c) && eqv(b, d)
    case (Or(a, b), Or(c, d))             => eqv(a, c) && eqv(b, d)
    case _                                => false

  /** Sugar for writing formulae in tests and examples. */
  object Syntax:
    import scala.language.implicitConversions

    given [F: Form]: Conversion[String, F] = _.atom

    extension [F: Form, A](f: A)(using Conversion[A, F])
      def ->:[B](f2: B)(using Conversion[B, F]): F = (f2: F) implies (f: F)
      def ==>[B](f2: B)(using Conversion[B, F]): F = (f: F) implies f2
      def /\[B](f2: B)(using Conversion[B, F]): F = (f: F) and f2
      def \/[B](f2: B)(using Conversion[B, F]): F = (f: F) or f2
