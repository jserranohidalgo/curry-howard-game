package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L.svg as S

/** Lucide glyphs, drawn rather than imported.
  *
  * The design system's icon set is Lucide — 24×24 grid, 2px stroke, round caps,
  * outline only, `currentColor` — and the handoff vendors it inline. So do we,
  * for the same reason it gives: the app has no JavaScript dependencies and
  * fetches nothing (D3). Only the glyphs actually used are here.
  *
  * The brand's hard rule that makes this necessary: **no emoji, and no Unicode
  * character used as an icon.** An arrow in a button is an icon and must be
  * drawn; `→` inside a formula is notation and stays as text.
  */
object Icons:

  def arrowRight(size: Int = 18): SvgElement = icon(size)(
    S.path(S.d := "M5 12h14"),
    S.path(S.d := "m12 5 7 7-7 7")
  )

  def arrowLeft(size: Int = 18): SvgElement = icon(size)(
    S.path(S.d := "M19 12H5"),
    S.path(S.d := "m12 19-7-7 7-7")
  )

  def chevronRight(size: Int = 14): SvgElement = icon(size)(S.path(S.d := "m9 18 6-6-6-6"))

  def chevronDown(size: Int = 14): SvgElement = icon(size)(S.path(S.d := "m6 9 6 6 6-6"))

  def check(size: Int = 14): SvgElement = icon(size)(S.path(S.d := "M20 6 9 17l-5-5"))

  def cross(size: Int = 14): SvgElement = icon(size)(
    S.path(S.d := "M18 6 6 18"),
    S.path(S.d := "m6 6 12 12")
  )

  def alert(size: Int = 15): SvgElement = icon(size)(
    S.circle(S.cx := "12", S.cy := "12", S.r := "10"),
    S.path(S.d := "M12 8v4"),
    S.path(S.d := "M12 16h.01")
  )

  private def icon(size: Int)(paths: SvgElement*): SvgElement =
    S.svg(
      S.cls := "icon",
      S.width := size.toString,
      S.height := size.toString,
      S.viewBox := "0 0 24 24",
      S.fill := "none",
      S.stroke := "currentColor",
      S.strokeWidth := "2",
      S.strokeLineCap := "round",
      S.strokeLineJoin := "round",
      S.role := "presentation",
      paths
    )
