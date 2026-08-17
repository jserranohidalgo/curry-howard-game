package curryhoward.web

import org.scalajs.dom
import curryhoward.engine.ipl.nj.*

/** Local persistence (D3/D4/D5): the whole explored tree, in `localStorage`.
  *
  * There is no server to save to and none is wanted, so a game lives in the
  * browser it was played in. What is stored is the *play* — [[Save]]'s few
  * lines of text — so restoring is replaying, and every branch, dead end and
  * backtrack comes back with it. The tree is the teaching artefact, not a
  * cache.
  *
  * Writing is best-effort: a browser with storage disabled, or a full quota,
  * must not take the game down with it. A player who cannot save simply plays.
  */
object Persist:

  private val Key = "curry-howard/game"

  /** Save a game in progress; anything else clears the slot.
    *
    * Leaving Play — by winning, being refuted, restarting or quitting — is
    * abandoning the search, and the design treats an abandoned search as
    * gone.
    */
  def write(model: Model): Unit =
    (model.screen, model.tree) match
      case (Screen.Play, Some(tree)) if !model.won && !model.refuted => put(Save.encode(tree))
      case _                                                        => clear()

  def read: Option[GameTree] =
    for
      text <- get
      tree <- Save.decode(text).toOption
    yield tree

  def clear(): Unit =
    try dom.window.localStorage.removeItem(Key)
    catch case _: Throwable => ()

  private def put(text: String): Unit =
    try dom.window.localStorage.setItem(Key, text)
    catch case _: Throwable => ()

  private def get: Option[String] =
    try Option(dom.window.localStorage.getItem(Key)).filter(_.nonEmpty)
    catch case _: Throwable => None
