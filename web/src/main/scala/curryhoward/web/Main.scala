package curryhoward.web

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** Phase 4: the playable vertical slice — Home → Setup → Play, on the same
  * engine the console plays. Phase 8 recreates the designed interface.
  */
object Main:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), App())
