package curryhoward.engine
package term

import form.Form

/** Terms, tagless-final — the program half of the correspondence.
  *
  * One constructor or destructor per rule of specification §3.2. An
  * interpretation decides what a term *is*: a data structure to store and
  * serialize, Scala source to show the programmer, or a natural-deduction
  * derivation to show the logician.
  *
  * Variables are numbered rather than named. Naming is a rendering concern —
  * the prototype's `freshName` picks `x`, `qr`, `q` from the types involved —
  * and keeping it out of the engine means the engine never has to care what a
  * hole is called.
  *
  * Two departures from `hablapps/tdd`, both driven by what the game must
  * *display* rather than by what the calculus needs:
  *
  *   - `unit`, the constructor of `⊤`/`Unit`, which that formalisation omits.
  *   - binders carried explicitly on `matchOn` and `let`. A sequent calculus can
  *     substitute a bound value away; the game cannot, because the player sees
  *     `case Left(q) => …` and `val qr: Either[Q, R] = pqr._2` on screen and
  *     those names are part of the lesson.
  */
trait Term[F: Form, T]:

  extension (v: Int) def `var`: T

  /** `()` — the sole inhabitant of `⊤`, and the only constructor that closes a
    * hole outright.
    */
  val unit: T

  extension (p: (Int, F))
    /** `(x: A) => body`, binding `p`. */
    def lam(body: T): T

  /** `v match { case Left(x) => onLeft; case Right(y) => onRight }`. */
  def matchOn(scrutinee: T, left: (Int, F), onLeft: T, right: (Int, F), onRight: T): T

  /** `val x: A = value; body` — how a forward elimination adds a resource. */
  def let(binder: (Int, F), value: T, body: T): T

  extension (t: T)
    infix def and(t2: T): T
    def _1: T
    def _2: T
    def inl(rightType: F): T
    def inr(leftType: F): T
    def apply(arg: T): T

    /** `⊥E` — anything follows from an impossible value. */
    def absurd(goal: F): T

    def subst(v: Int, by: T): T

object Term:

  def apply[F: Form, T](using T: Term[F, T]): Term[F, T] = T

  type Aux[F] = [t] =>> Term[F, t]
