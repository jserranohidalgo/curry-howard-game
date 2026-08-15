package curryhoward.engine
package ipl


/** A sequent — which is to say, **a hole in the game**.
  *
  * `con` is the type the hole must be filled with; `ant` is the resources in
  * scope at it, each with the variable number it was bound to. The game's own
  * vocabulary and the proof theory coincide exactly here, which is why the
  * engine has one notion rather than two.
  */
case class Sequent(ant: List[Sequent.Prem], con: Formula):

  /** Next free variable number. Names are assigned at rendering time. */
  def nextVar: Int = if ant.isEmpty then 0 else ant.map(_._1).max + 1

  /** Every rotation of the antecedent.
    *
    * Rules match on the *head* of the antecedent, so presenting each resource
    * at the head in turn is how "this destructor applies to any variable in
    * scope" gets expressed — one move per applicable resource, which is
    * precisely what the rules table's instance count shows.
    */
  def rotations: LazyList[Sequent] =
    LazyList.range(0, ant.length).map(i => Sequent(ant.drop(i) ++ ant.take(i), con))

  def extend(p: Sequent.Prem): Sequent = Sequent(p :: ant, con)

  override def toString: String =
    val scope = ant.map((v, f) => s"$v: ${Notation.programmer(f)}").mkString(", ")
    (if scope.isEmpty then "" else s"$scope ") + s"⊢ ${Notation.programmer(con)}"

object Sequent:

  /** A resource in scope: its variable number and its type. */
  type Prem = (Int, Formula)

  /** The initial state of a game: one hole of the goal type, nothing in scope. */
  def initial(goal: Formula): Sequent = Sequent(Nil, goal)
