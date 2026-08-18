package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import curryhoward.engine.ipl.*

/** Scala source, coloured.
  *
  * Three kinds of token get a colour and nothing else does: the keywords that
  * carry the move (`val`, `case`, `match`, `def`), the constructors a player
  * types (`Left`, `Right`), and the **atoms**, which keep the same tint here as
  * in the goal, in the hole chips and in the derivation. Following one
  * proposition through a play by colour is the point; syntax highlighting for
  * its own sake is not, so punctuation and the rest stay as body text.
  *
  * Tokenised over the printed string for the same reason [[TypeText]] is: the
  * renderer in the engine owns what the program *is*, and a second one that
  * could disagree with it would be a bug waiting to happen.
  */
object Code:

  private val Keywords = Set("val", "case", "match", "def", "type")
  private val Constructors = Set("Left", "Right", "Unit", "Nothing", "Either")

  def apply(text: String, atoms: Set[String]): List[HtmlElement] =
    val tokens = "[A-Za-z_][A-Za-z0-9_]*|[^A-Za-z_]+".r.findAllIn(text).toList
    tokens.map { token =>
      if Keywords.contains(token) then span(cls := "kw", token)
      else if atoms.contains(token.capitalize) then
        span(cls := "atom", styleAttr := s"color: var(--atom-${token.capitalize}, inherit)", token)
      else if Constructors.contains(token) then span(cls := "ctor", token)
      else span(token)
    }

  /** The atoms of a goal, which are the names worth tinting on this screen. */
  def atomsOf(goal: Goal): Set[String] = goal.tyParams.toSet
