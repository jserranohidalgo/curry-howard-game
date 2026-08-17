package curryhoward.engine
package ipl
package nj

import cats.syntax.all.*

import Partial.*
import GameTree.*

/** Writing a game down, and reading it back (D4/D5).
  *
  * **A game is saved as the play that produced it**, not as the positions it
  * reached. Every node is its parent's position with one move filled in, so the
  * hole's path and the move's index at that hole are enough to rebuild it —
  * which is exactly what D4/D5 asked Phase 2 for when it said moves must be
  * data rather than closures. Nothing here would work with the prototype's
  * `build()` function.
  *
  * The format is a few lines of text rather than JSON: the engine has no JSON
  * dependency, a save is small, and a human can read one. It is also the same
  * shape a shareable link would need (D6), which is deferred but not designed
  * away.
  *
  * {{{
  * chg1
  * (P, Either[Q, R]) => Either[(P, Q), (P, R)]
  * 3
  * 1 0 - 0
  * 2 1 0 4
  * 3 2 0.1 1
  * }}}
  *
  * Version, goal, current node, then one line per move in the order it was
  * first played: `id parent hole-path move-index`, with `-` for the root hole.
  * Replaying in ascending id order reassigns exactly the same ids, since the
  * tree hands them out in that order — so the saved `current` still names the
  * node the player was on.
  *
  * **A save is tied to this rule set.** Move indices are positions in
  * `NJ.coalg`'s output, so reordering the rules invalidates saved games. That
  * is what the version tag is for: bump it when the coalgebra changes, and old
  * saves are refused rather than silently misread.
  */
object Save:

  val Version = "chg1"

  enum SaveError:
    /** Not a save file, or truncated. */
    case Malformed(line: Int)

    /** Written by a different rule set (see the note above). */
    case BadVersion(found: String)

    /** The goal line does not parse. */
    case BadGoal(why: ParseError)

    /** Replaying the moves did not reproduce the tree: the save is stale, or
      * the rules have changed under it.
      */
    case Stale(atMove: Int)

  def encode(tree: GameTree): String =
    val edges = tree.nodes.values.toList
      .flatMap(node => (node.parentId, node.via).mapN((parent, via) => (node.id, parent, via)))
      .sortBy(_._1.value)
      .map((id, parent, via) =>
        s"${id.value} ${parent.value} ${path(via.hole)} ${via.moveIndex}"
      )

    (List(Version, Notation.programmer(tree.goal), tree.currentId.value.toString) ++ edges)
      .mkString("\n")

  def decode(text: String): Either[SaveError, GameTree] =
    val lines = text.linesIterator.map(_.trim).filter(_.nonEmpty).toList
    lines match
      case version :: _ if version != Version => Left(SaveError.BadVersion(version))
      case _ :: goal :: current :: moves =>
        for
          formula <- Parser.parseGoal(goal).left.map(SaveError.BadGoal(_))
          currentId <- current.toIntOption.toRight(SaveError.Malformed(3))
          replayed <- replay(GameTree.start(formula.formula), moves)
          _ <- Either.cond(replayed.nodes.contains(NodeId(currentId)), (), SaveError.Malformed(3))
        yield replayed.goTo(NodeId(currentId))
      case _ => Left(SaveError.Malformed(lines.length))

  /** Play the saved moves back, checking as it goes that each one produced the
    * node the save says it did. A move that no longer exists — or that lands
    * somewhere else — fails the whole restore rather than yielding a tree that
    * is quietly not the player's.
    */
  private def replay(from: GameTree, moves: List[String]): Either[SaveError, GameTree] =
    moves.zipWithIndex.foldLeft[Either[SaveError, GameTree]](Right(from)):
      case (Left(err), _) => Left(err)
      case (Right(tree), (line, i)) =>
        line.split(' ').toList match
          case id :: parent :: hole :: index :: Nil =>
            val parsed =
              for
                expected <- id.toIntOption
                parentId <- parent.toIntOption
                moveIndex <- index.toIntOption
                holePath <- unpath(hole)
              yield (expected, parentId, holePath, moveIndex)

            parsed match
              case None => Left(SaveError.Malformed(i + 4))
              case Some((expected, parentId, holePath, moveIndex)) =>
                val grown = tree.goTo(NodeId(parentId)).play(holePath, moveIndex)
                // `play` leaves the tree alone when the move does not exist, and
                // reuses an existing child rather than making a new one — either
                // way the current node is not the fresh id the save expects.
                Either.cond(
                  grown.currentId == NodeId(expected) && grown.size == tree.size + 1,
                  grown,
                  SaveError.Stale(i + 1)
                )
          case _ => Left(SaveError.Malformed(i + 4))

  private def path(p: Path): String = if p.isEmpty then "-" else p.mkString(".")

  private def unpath(s: String): Option[Path] =
    if s == "-" then Some(Nil)
    else s.split('.').toList.traverse(_.toIntOption)
