package curryhoward.engine
package calculus

import form.Form
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
final case class GameTree[F, C[_, _]](
    goal: F,
    nodes: Map[GameTree.NodeId, GameNode[F, C]],
    rootId: GameTree.NodeId,
    currentId: GameTree.NodeId,
    private val nextId: Int
):

  def current: GameNode[F, C] = nodes(currentId)

  def node(id: GameTree.NodeId): Option[GameNode[F, C]] = nodes.get(id)

  /** Backtrack — or jump forward again, since nothing was thrown away. */
  def goTo(id: GameTree.NodeId): GameTree[F, C] =
    if nodes.contains(id) then copy(currentId = id) else this

  def path(id: GameTree.NodeId): List[GameNode[F, C]] =
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

  def start[F: Form, C[_, _]: Calculus](goal: F): GameTree[F, C] =
    val root = GameNode[F, C](
      id = NodeId(0),
      parentId = None,
      via = None,
      position = Partial.start[F, C](goal),
      children = Map.empty,
      depth = 0
    )
    GameTree(goal, Map(NodeId(0) -> root), NodeId(0), NodeId(0), 1)

  extension [F: Form, C[_, _]](tree: GameTree[F, C])(using Calculus[C])

    /** Take a move at a hole of the current position.
      *
      * Returns the tree with a new current node. If this exact (hole, move)
      * has been played from here before, the existing child is reused rather
      * than duplicated — replaying is navigation, not exploration.
      */
    def play(hole: Path, moveIndex: Int): GameTree[F, C] =
      val from = tree.current
      val key = MoveKey(hole, moveIndex)

      from.children.get(key) match
        case Some(existing) => tree.goTo(existing)
        case None =>
          from.position.movesAt(hole).lift(moveIndex) match
            case None => tree // no such move; leave the tree alone
            case Some(move) =>
              val id = NodeId(tree.nextId)
              val child = GameNode[F, C](
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
    def playFirst(moveIndex: Int): GameTree[F, C] =
      tree.current.position.holes.headOption
        .fold(tree)((path, _) => tree.play(path, moveIndex))

    /** Every (hole, move) pair available at the current node, whether or not it
      * has been played.
      */
    def options: List[(MoveKey, C[F, Sequent[F]])] =
      tree.current.position.holes.flatMap: (path, _) =>
        tree.current.position
          .movesAt(path)
          .zipWithIndex
          .map((move, i) => (MoveKey(path, i), move))
          .toList

/** One explored position. */
final case class GameNode[F, C[_, _]](
    id: GameTree.NodeId,
    parentId: Option[GameTree.NodeId],
    /** The move that led here, absent at the root. */
    via: Option[GameTree.MoveKey],
    position: Partial[F, C],
    /** Keyed by the move taken, so a pair can have at most one child. */
    children: Map[GameTree.MoveKey, GameTree.NodeId],
    depth: Int
)
