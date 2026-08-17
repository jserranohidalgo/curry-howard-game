package curryhoward.engine
package ipl
package nj

import Partial.*
import Partial.Path

/** The explored part of the search space — the game's memory.
  *
  * Nothing is ever destroyed: a move creates a *new* node whose parent is the
  * current one, so every position the player has reached stays reachable, and
  * backtracking is just moving `currentId` (interaction spec §6).
  *
  * **Children are keyed by the move that produced them**, which is the whole
  * point of this representation. The prototype appends a child per
  * application, so replaying a move from an earlier node yields two children
  * for one (hole, move) pair; its exhaustion check then compares a child
  * *count* against the number of available pairs and can conclude "everything
  * has been tried" with pairs still untried — announcing "This is not a
  * theorem!" about a theorem. Keyed children make that arithmetic impossible:
  * replaying a move returns the child that already exists.
  */
final case class GameTree(
    goal: Formula,
    nodes: Map[GameTree.NodeId, GameNode],
    rootId: GameTree.NodeId,
    currentId: GameTree.NodeId,
    private val nextId: Int
):

  def current: GameNode = nodes(currentId)

  def node(id: GameTree.NodeId): Option[GameNode] = nodes.get(id)

  /** Backtrack — or jump forward again, since nothing was thrown away. */
  def goTo(id: GameTree.NodeId): GameTree =
    if nodes.contains(id) then copy(currentId = id) else this

  def path(id: GameTree.NodeId): List[GameNode] =
    nodes.get(id) match
      case None => Nil
      case Some(n) => n.parentId.fold(List(n))(p => path(p) :+ n)

  def depth: Int = current.depth
  def size: Int = nodes.size

object GameTree:

  opaque type NodeId = Int

  object NodeId:
    def apply(i: Int): NodeId = i
    extension (id: NodeId) def value: Int = id

  /** Which move was taken: the hole it was taken at, and its index among the
    * moves legal there. Both are plain data, so a play is a list of these and
    * nothing more.
    */
  final case class MoveKey(hole: Path, moveIndex: Int)

  def start(goal: Formula): GameTree =
    val root = GameNode(
      id = NodeId(0),
      parentId = None,
      via = None,
      position = Partial.start(goal),
      children = Map.empty,
      depth = 0
    )
    GameTree(goal, Map(NodeId(0) -> root), NodeId(0), NodeId(0), 1)

  extension (tree: GameTree)

    /** Take a move at a hole of the current position.
      *
      * Returns the tree with a new current node. If this exact (hole, move)
      * has been played from here before, the existing child is reused rather
      * than duplicated — replaying is navigation, not exploration.
      */
    def play(hole: Path, moveIndex: Int): GameTree =
      val from = tree.current
      val key = MoveKey(hole, moveIndex)

      from.children.get(key) match
        case Some(existing) => tree.goTo(existing)
        case None =>
          from.position.movesAt(hole).lift(moveIndex) match
            case None => tree // no such move; leave the tree alone
            case Some(move) =>
              val id = NodeId(tree.nextId)
              val child = GameNode(
                id = id,
                parentId = Some(from.id),
                via = Some(key),
                position = from.position.fill(hole, move),
                children = Map.empty,
                depth = from.depth + 1
              )
              tree.copy(
                nodes = tree.nodes
                  + (id -> child)
                  + (from.id -> from.copy(children = from.children + (key -> id))),
                currentId = id,
                nextId = tree.nextId + 1
              )

    /** Play at the first open hole — the common case, since the UI keeps one
      * hole selected and defaults it to the first.
      */
    def playFirst(moveIndex: Int): GameTree =
      tree.current.position.holes.headOption
        .fold(tree)((path, _) => tree.play(path, moveIndex))

    /** Every (hole, move) pair available at the current node, whether or not it
      * has been played.
      */
    def options: List[(MoveKey, NJ[Sequent])] = tree.optionsAt(tree.currentId)

    /** The same, at any node — what exhaustion has to enumerate. */
    def optionsAt(id: NodeId): List[(MoveKey, NJ[Sequent])] =
      tree.nodes.get(id).toList.flatMap: node =>
        node.position.holes.flatMap: (path, _) =>
          node.position
            .movesAt(path)
            .zipWithIndex
            .map((move, i) => (MoveKey(path, i), move))
            .toList

    /** **Nothing can be built from here, and the player has shown it** by
      * trying everything (§4.7; interaction spec §6.2 calls it *lost*).
      *
      * Three cases, and the middle one is what keeps this affordable:
      *
      *   - a **won** position is a solution, so it is never exhausted;
      *   - a **dead** position has a hole no rule applies to, so no play can
      *     ever complete it — exhausted without exploring a thing, which
      *     prunes whole subtrees that would otherwise have to be walked;
      *   - otherwise, exhausted exactly when **every** (hole, move) pair
      *     available here has been played and led to an exhausted child.
      *
      * This reads the pair-keying rather than counting children, which is the
      * whole reason children are keyed. The prototype compares a child *count*
      * against the number of pairs, and nothing stops the same pair being
      * played twice from one node — jump back, replay, and the count reaches
      * the threshold with pairs still untried, announcing "This is not a
      * theorem!" about a theorem. Keyed children make the arithmetic
      * impossible: replaying returns the child that already exists, so
      * `children.get(pair)` is the exact question.
      *
      * The recursion terminates because it walks the *explored* tree, which is
      * finite and acyclic — a child's id is always greater than its parent's.
      * An unexplored branch is simply not exhausted, which is the honest
      * answer: an undecided search continues (D13).
      */
    def exhausted(id: NodeId): Boolean =
      tree.nodes.get(id).exists: node =>
        node.position.status match
          case Status.Won  => false
          case Status.Dead => true
          case Status.Open =>
            tree
              .optionsAt(id)
              .forall((key, _) => node.children.get(key).exists(tree.exhausted))

    /** The negative ending: every play from the opening has been tried, and
      * none of them proves the goal.
      *
      * Only ever true of a **finite** search space that has actually been
      * walked (D13). A goal whose space is infinite never reaches this, and the
      * game simply continues — which is what an undecided search honestly looks
      * like.
      */
    def refuted: Boolean = tree.exhausted(tree.rootId)

/** One explored position. */
final case class GameNode(
    id: GameTree.NodeId,
    parentId: Option[GameTree.NodeId],
    /** The move that led here, absent at the root. */
    via: Option[GameTree.MoveKey],
    position: Partial,
    /** Keyed by the move taken, so a pair can have at most one child. */
    children: Map[GameTree.MoveKey, GameTree.NodeId],
    depth: Int
)
