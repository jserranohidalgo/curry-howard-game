package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import cats.syntax.all.*
import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*
import curryhoward.engine.ipl.nj.ToScala.Piece
import Model.*

/** Phase 4: the vertical slice. Home → Setup → Play, end to end, on the same
  * engine the console plays. Minimal styling — Phase 8 recreates the design.
  */
object App:

  private val model = Var(Model.empty)

  def apply(): HtmlElement =
    div(cls := "app", child <-- model.signal.map(render))

  private def render(m: Model): HtmlElement = m.screen match
    case Screen.Home  => home
    case Screen.Setup => setup(m)
    case Screen.Play  => if m.won then won(m) else play(m)

  // --- Home -----------------------------------------------------------------

  private def home: HtmlElement =
    div(
      cls := "sheet",
      img(cls := "hero-mark", src := "./assets/logo-urjc.svg", alt := "URJC"),
      h1(cls := "title", "The Curry–Howard Game"),
      p(cls := "tagline", "Construir un programa y demostrar un teorema son el mismo movimiento."),
      p(
        cls := "lede",
        "Parte de un hueco con un tipo y ve rellenándolo con constructores y destructores. ",
        "Cuando no queden huecos, tendrás un programa — y con él, una demostración."
      ),
      p(cls := "overline", "Empezar"),
      div(
        cls := "systems",
        systemRow("Lógica proposicional intuicionista", "λ simplemente tipado con tipos algebraicos", true),
        systemRow("Lógica proposicional clásica", "λμ — continuaciones", false),
        systemRow("Lógica de primer orden", "λΠ — tipos dependientes", false),
        systemRow("Lógica lineal multiplicativa", "λ lineal", false)
      )
    )

  private def systemRow(logic: String, language: String, active: Boolean): HtmlElement =
    button(
      cls := "start-btn",
      cls("inactive") := !active,
      disabled := !active,
      onClick.filter(_ => active) --> { _ => model.update(_.copy(screen = Screen.Setup)) },
      div(span(cls := "system-logic", logic), span(cls := "system-language", language)),
      if active then span(cls := "arrow", "→") else span(cls := "soon", "Próximamente")
    )

  // --- Setup ----------------------------------------------------------------

  private def setup(m: Model): HtmlElement =
    div(
      cls := "sheet",
      backButton(Screen.Home),
      h2(cls := "title", "Escribe una signatura"),
      p(cls := "lede", "O la proposición que le corresponde — son la misma cosa escrita de dos maneras."),
      input(
        cls := "goal-input",
        placeholder := "(A, B) => (B, A)",
        value := m.input,
        onInput.mapToValue --> { v => model.update(_.copy(input = v)) },
        onKeyDown.filter(_.key == "Enter").mapTo(()) --> { _ => begin() }
      ),
      feedback(m),
      button(
        cls := "primary",
        disabled := m.parsed.isLeft,
        onClick --> { _ => begin() },
        "Empezar"
      ),
      p(cls := "overline", "Ejemplos"),
      div(
        cls := "examples",
        Examples.all.map { ex =>
          button(
            cls := "example",
            onClick --> { _ => model.update(_.copy(input = ex.programmer)) },
            span(cls := "mono", ex.programmer),
            span(cls := "muted", ex.logician)
          )
        }
      )
    )

  /** The core teaching moment of this screen: the same goal, echoed in both
    * notations at once.
    */
  private def feedback(m: Model): HtmlElement =
    div(
      cls := "feedback",
      m.parsed match
        case Right(goal) =>
          div(
            echoRow("PROGRAMADOR", Notation.programmer(goal.formula)),
            echoRow("LÓGICO", Notation.logician(goal.formula))
          )
        case Left(ParseError.Empty) => div(cls := "muted", "Escribe un objetivo.")
        case Left(err) =>
          div(cls := "error", s"${describe(err)} (posición ${err.pos})")
    )

  private def echoRow(label: String, text: String): HtmlElement =
    div(cls := "echo", span(cls := "echo-label", label), span(cls := "mono", text))

  private def begin(): Unit =
    model.update(m => m.parsed.fold(_ => m, goal => Model.start(goal)))

  // --- Play -----------------------------------------------------------------

  private def play(m: Model): HtmlElement =
    div(
      cls := "play",
      div(
        cls := "left",
        h3(cls := "overline", "Movimientos"),
        movesPanel(m),
        div(
          cls := "footer",
          button(cls := "ghost", onClick --> { _ => model.update(_.restart) }, "Reiniciar"),
          button(cls := "ghost", onClick --> { _ => model.set(Model.empty) }, "Salir")
        )
      ),
      div(
        cls := "right",
        goalCard(m),
        termCard(m),
        hint(m),
        resourcesCard(m)
      )
    )

  private def goalCard(m: Model): HtmlElement =
    val goal = m.goal.get
    val params = if goal.tyParams.isEmpty then "" else goal.tyParams.mkString("[", ", ", "]")
    div(
      cls := "card goal-card",
      div(cls := "eyebrow", "Signatura a habitar"),
      div(cls := "mono goal", s"def solution$params: ${Notation.programmer(goal.formula)}")
    )

  /** The term, with its holes as chips. Exactly one is selected, and the moves
    * panel refers to it.
    */
  private def termCard(m: Model): HtmlElement =
    val position = m.position.get
    val holes = m.holes
    val selectedPath = m.selectedHole.map(_._1)

    div(
      cls := "card term-card mono",
      ToScala.pieces(ToLambda.position(position)).map {
        case Piece.Text(t) => span(t)
        case Piece.HoleAt(i, goal) =>
          val path = holes.lift(i).map(_._1)
          span(
            cls := "hole",
            cls("selected") := path == selectedPath,
            onClick --> { _ => path.foreach(p => model.update(_.select(p))) },
            s"… : ${Notation.programmer(goal)}"
          )
      }
    )

  private def hint(m: Model): HtmlElement =
    m.selectedHole match
      case None => div()
      case Some((_, hole)) =>
        div(
          cls := "hint",
          "Hueco seleccionado ",
          span(cls := "mono strong", Notation.programmer(hole.con)),
          " — elige una regla a la izquierda."
        )

  private def resourcesCard(m: Model): HtmlElement =
    m.selectedHole match
      case None => div()
      case Some((_, hole)) =>
        val names = ToScala.names(hole.ant.reverse)
        div(
          cls := "resources",
          div(cls := "eyebrow", "Recursos disponibles"),
          if hole.ant.isEmpty then div(cls := "muted", "Nada en contexto todavía.")
          else
            div(
              cls := "chips",
              hole.ant.reverse.map { (v, ty) =>
                div(
                  cls := "chip",
                  span(cls := "mono strong", names.getOrElse(v, s"v$v")),
                  span(cls := "mono", Notation.programmer(ty)),
                  span(cls := "kind", kind(ty))
                )
              }
            )
        )

  /** The rules table, as the design has it: every rule shown, applicable ones
    * actionable, inapplicable ones de-emphasised rather than hidden — the table
    * is the syllabus. Phase 8 gives it its real geometry.
    */
  private def movesPanel(m: Model): HtmlElement =
    val byLabel = m.moves.zipWithIndex.groupBy((move, _) => NJ.label(move))
    // Names already in force at the selected hole, so a move reads
    // `qr match { … }` rather than `v1 match { … }`.
    val names = m.selectedHole.map((_, hole) => ToScala.names(hole.ant.reverse)).getOrElse(Map.empty)

    div(
      cls := "rules",
      Rules.rows.map { row =>
        div(
          cls := "rules-row",
          div(cls := "rules-label mono", row.label),
          div(cls := "rules-cell", cell(row.construct, byLabel, row.constructAbsent, names)),
          div(cls := "rules-cell", cell(row.destruct, byLabel, row.destructAbsent, names))
        )
      }
    )

  private def cell(
      labels: List[String],
      byLabel: Map[String, List[(NJ[Sequent], Int)]],
      absent: Boolean,
      names: Map[Int, String]
  ): HtmlElement =
    if absent then div(cls := "absent", "—")
    else
      val entries = labels.flatMap(byLabel.getOrElse(_, Nil))
      if entries.isEmpty then div(cls := "inapplicable", "·")
      else
        div(
          entries.map { (move, index) =>
            button(
              cls := "move",
              onClick --> { _ => model.update(_.play(index)) },
              span(cls := "move-name mono", NJ.label(move)),
              span(cls := "move-effect mono", effect(move, names))
            )
          }
        )

  private def effect(move: NJ[Sequent], names: Map[Int, String]): String =
    val rendered = ToScala.fragment(ToLambda.apply(move.map(s => Lambda.Hole(s.con))), names)
    // A `let` renders as `val x: T = …; …`; the binding alone says what it does.
    val shown =
      if NJ.binds(move).isDefined then rendered.takeWhile(_ != ';').replaceAll("= … : .*$", "= …")
      else rendered
    if shown.length > 40 then shown.take(39) + "…" else shown

  // --- Won ------------------------------------------------------------------

  private def won(m: Model): HtmlElement =
    val goal = m.goal.get
    val term = ToLambda.complete(m.position.get).get
    val params = if goal.tyParams.isEmpty then "" else goal.tyParams.mkString("[", ", ", "]")

    div(
      cls := "scrim",
      div(
        cls := "card result",
        div(cls := "tick", "✓"),
        h2("Resuelto"),
        p(cls := "lede", "El tipo está habitado — y la proposición, demostrada."),
        div(cls := "eyebrow", "Como lo construiste"),
        pre(cls := "mono", ToScala(term)),
        div(cls := "eyebrow", "Limpio"),
        pre(cls := "mono", s"def solution$params = ${ToScala.bare(Cleanup.simplify(term))}"),
        button(cls := "primary", onClick --> { _ => model.set(Model.empty) }, "Nueva partida")
      )
    )

  // --- Odds and ends --------------------------------------------------------

  private def backButton(to: Screen): HtmlElement =
    button(cls := "ghost back", onClick --> { _ => model.update(_.copy(screen = to)) }, "← Volver")

  private def kind(ty: Formula): String = ty match
    case Formula.And(_, _)     => "par"
    case Formula.Or(_, _)      => "suma"
    case Formula.Implies(_, _) => "función"
    case Formula.Atom(_)       => "átomo"
    case Formula.True          => "unit"
    case Formula.False         => "vacío"

  private def describe(err: ParseError): String = err match
    case ParseError.Empty                   => "Escribe un objetivo"
    case ParseError.UnknownChar(c, _)       => s"No reconozco '$c'"
    case ParseError.ExpectedBracket(_)      => "Either necesita un [ aquí"
    case ParseError.ExpectedComma(_)        => "Falta una coma"
    case ParseError.ExpectedCloseBracket(_) => "Falta un ]"
    case ParseError.ExpectedCloseParen(_)   => "Falta un )"
    case ParseError.UnexpectedEnd(_)        => "El objetivo se corta"
    case ParseError.Unexpected(text, _)     => s"No esperaba '$text'"
    case ParseError.TrailingInput(_)        => "Sobra texto tras el objetivo"

/** The §3.2 table: which rules live in which cell, and which cells hold no rule
  * at all.
  */
object Rules:
  final case class Row(
      label: String,
      construct: List[String],
      destruct: List[String],
      constructAbsent: Boolean = false,
      destructAbsent: Boolean = false
  )

  val rows: List[Row] = List(
    Row("=>", List("⟶.I"), List("⟶.E")),
    Row("( , )", List("∧.I"), List("∧.E₁", "∧.E₂")),
    Row("Either", List("∨.I₁", "∨.I₂"), List("∨.E")),
    Row("Unit", List("⊤.I"), Nil, destructAbsent = true),
    Row("Nothing", Nil, List("⊥.E"), constructAbsent = true),
    Row("hyp", Nil, List("Ax"), constructAbsent = true),
    Row("let", List("let"), Nil, destructAbsent = true)
  )
