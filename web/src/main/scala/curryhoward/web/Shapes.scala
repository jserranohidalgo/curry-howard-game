package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.L.svg as S
import curryhoward.engine.ipl.*

/** The shape language: every type has a glyph.
  *
  * A player recognises a pair, a sum or a function before reading the formula,
  * and the same glyph follows that type everywhere — the system rows on Home,
  * the goal field, the hole chips, the resources in scope. The handoff's own
  * drawings, transcribed: a portal with an arrow through it for a function, two
  * fused cells for a pair, a forked cell for a sum, a filled token for `Unit`, a
  * struck circle for `Nothing`, and a crystal for an atom.
  *
  * These are notation, not decoration — which is also why they are SVG and not
  * characters. The brand's rule is explicit: no emoji and no Unicode used as an
  * icon (`∧ ∨ → ⊤ ⊥` are notation and stay).
  */
object Shapes:

  private val Stroke = 1.6

  def glyph(ty: Formula, size: Int = 22): SvgElement =
    S.svg(
      S.cls := "glyph",
      S.width := size.toString,
      S.height := size.toString,
      S.viewBox := "0 0 24 24",
      S.role := "presentation",
      body(ty)
    )

  private def body(ty: Formula): SvgElement = ty match
    // A function: a portal, with an arrow passing through it.
    case Formula.Implies(_, _) =>
      S.g(
        cell(),
        S.path(S.d := "M7 12 H15.5", S.fill := "none", stroke, S.strokeLineCap := "round"),
        S.path(
          S.d := "M13 8.5 L17 12 L13 15.5",
          S.fill := "none",
          stroke,
          S.strokeLineCap := "round",
          S.strokeLineJoin := "round"
        )
      )

    // A pair: one cell divided into two.
    case Formula.And(_, _) =>
      S.g(cell(), S.path(S.d := "M12 4.6 V19.4", S.fill := "none", stroke))

    // A sum: one cell forked — either this side or that.
    case Formula.Or(_, _) =>
      S.g(cell(), S.path(S.d := "M5 19 L19 5", S.fill := "none", stroke, S.strokeLineCap := "round"))

    // ⊤: the token that is always available.
    case Formula.True =>
      S.circle(S.cx := "12", S.cy := "12", S.r := "8.5", S.fill := "var(--shape-ink)")

    // ⊥: the empty one, struck through.
    case Formula.False =>
      S.g(
        S.circle(
          S.cx := "12",
          S.cy := "12",
          S.r := "8.5",
          S.fill := "none",
          S.stroke := "var(--urjc-red-darker)",
          S.strokeWidth := Stroke.toString
        ),
        S.path(
          S.d := "M6.5 17.5 L17.5 6.5",
          S.stroke := "var(--urjc-red-darker)",
          S.strokeWidth := Stroke.toString,
          S.strokeLineCap := "round"
        )
      )

    // An atom: a crystal, tinted by which atom it is.
    case Formula.Atom(name) => crystalPath(name)

  /** The atom crystal with its letter in it — the resources panel's token. */
  def atom(name: String, size: Int = 22): SvgElement =
    S.svg(
      S.cls := "glyph",
      S.width := size.toString,
      S.height := size.toString,
      S.viewBox := "0 0 24 24",
      S.role := "presentation",
      crystalPath(name),
      S.text(
        S.x := "12",
        S.y := "13.2",
        S.textAnchor := "middle",
        S.dominantBaseline := "middle",
        S.fontFamily := "var(--font-mono)",
        S.fontSize := "9.5",
        S.fontWeight := "600",
        S.fill := s"var(--atom-$name, var(--text-strong))",
        name
      )
    )

  private def crystalPath(name: String): SvgElement =
    S.path(
      S.d := "M12 2.6 L20.5 8 V16 L12 21.4 L3.5 16 V8 Z",
      S.fill := s"var(--atom-$name-soft, var(--shape-fill))",
      S.stroke := s"var(--atom-$name, var(--shape-ink))",
      S.strokeWidth := Stroke.toString,
      S.strokeLineJoin := "round"
    )

  private def cell(): SvgElement =
    S.rect(
      S.x := "2.5",
      S.y := "3.8",
      S.width := "19",
      S.height := "16.4",
      S.rx := "4",
      S.fill := "var(--shape-fill)",
      stroke,
      S.strokeLineJoin := "round"
    )

  private def stroke: Modifier[SvgElement] =
    List(S.stroke := "var(--shape-ink)", S.strokeWidth := Stroke.toString)
