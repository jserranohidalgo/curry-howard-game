package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import cats.syntax.all.*
import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.Sequent.Prem
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*
import curryhoward.engine.ipl.nj.ToScala.Piece
import Model.*

/** Phase 4: the vertical slice. Home → Setup → Play, end to end, on the same
  * engine the console plays. Minimal styling — Phase 8 recreates the design.
  */
object App:

  private val model = Var(Model.empty)

  /** Every change to the model goes through here, and every change is saved.
    *
    * D4/D5 wants the explored tree to survive a closed laptop, and the cheapest
    * way to be sure of that is to leave no path that mutates the model without
    * writing it down. [[Persist.write]] decides what a given state means for
    * the saved slot — including clearing it, since leaving Play is abandoning
    * the search.
    */
  private def edit(f: Model => Model): Unit =
    model.update(f)  // the one place that writes to the Var
    Persist.write(model.now())

  def apply(): HtmlElement =
    div(
      cls := "app",
      child <-- model.signal.map(render),
      // `Esc` dismisses a dialog and never confirms — the interaction spec is
      // explicit, and it is the one keyboard behaviour that ships (D18a).
      documentEvents(_.onKeyDown).filter(_.key == "Escape") --> { _ => edit(_.dismiss) }
    )

  private def render(m: Model): HtmlElement = m.screen match
    case Screen.Home  => home
    case Screen.Setup => setup(m)
    case Screen.Play =>
      // The three endings of §6.2, in the order they are decided: a solution
      // beats everything, and refutation is only ever reached with no solution
      // anywhere in the tree.
      if m.won then won(m)
      else if m.refuted then lost(m)
      else play(m)

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
      resumeBanner,
      p(cls := "overline", "Empezar"),
      div(
        cls := "systems",
        systemRow("Lógica proposicional intuicionista", "λ simplemente tipado con tipos algebraicos", true),
        systemRow("Lógica proposicional clásica", "λμ — continuaciones", false),
        systemRow("Lógica de primer orden", "λΠ — tipos dependientes", false),
        systemRow("Lógica lineal multiplicativa", "λ lineal", false)
      )
    )

  /** A game left unfinished is offered back, never resumed behind the player's
    * back: they may well have come here to start something else. Declining
    * throws the tree away, which is why it says so.
    */
  private def resumeBanner: HtmlElement =
    Persist.read match
      case None => div()
      case Some(tree) =>
        div(
          cls := "resume",
          div(
            div(cls := "eyebrow", "Partida sin terminar"),
            div(cls := "mono", Notation.programmer(tree.goal)),
            div(cls := "muted", s"${tree.size} posiciones exploradas")
          ),
          div(
            cls := "resume-actions",
            button(cls := "primary", onClick --> { _ => edit(_ => Model.resume(tree)) }, "Continuar"),
            button(
              cls := "ghost",
              onClick --> { _ =>
                Persist.clear()
                edit(identity)
              },
              "Descartar"
            )
          )
        )

  private def systemRow(logic: String, language: String, active: Boolean): HtmlElement =
    button(
      cls := "start-btn",
      cls("inactive") := !active,
      disabled := !active,
      onClick.filter(_ => active) --> { _ => edit(_.copy(screen = Screen.Setup)) },
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
        onInput.mapToValue --> { v => edit(_.copy(input = v)) },
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
            onClick --> { _ => edit(_.copy(input = ex.programmer)) },
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
    edit(m => m.parsed.fold(_ => m, goal => Model.start(goal)))

  // --- Play -----------------------------------------------------------------

  private def play(m: Model): HtmlElement =
    div(
      cls := "play",
      div(
        cls := "left",
        searchPath(m),
        h3(cls := "overline", "Movimientos"),
        movesPanel(m),
        div(
          cls := "footer",
          // Backtrack and Restart are always reachable, per the interaction
          // spec. Backtrack needs no confirmation — nothing is lost by it.
          button(
            cls := "ghost",
            disabled := m.backtrackTarget.isEmpty,
            onClick --> { _ => edit(_.backtrack) },
            "Retroceder"
          ),
          button(cls := "ghost", onClick --> { _ => edit(_.ask(Confirm.Restart)) }, "Reiniciar"),
          button(cls := "ghost", onClick --> { _ => edit(_.ask(Confirm.Quit)) }, "Salir")
        )
      ),
      div(
        cls := "right",
        goalCard(m),
        termCard(m),
        hint(m),
        resourcesCard(m)
      ),
      if m.deadEnd then deadEndToast(m)
      else if m.lineExhausted then exhaustedToast(m)
      else emptyNode,
      m.confirm.fold(emptyNode)(confirmDialog)
    )

  /** The left column's *past*: every position explored, and the way back to any
    * of them.
    *
    * Nothing is ever destroyed, so this is the whole game — jumping to a node
    * *is* backtracking, and the branch you leave stays where it was. Collapsed
    * by default: the rules table has first claim on the room.
    */
  private def searchPath(m: Model): HtmlElement =
    m.tree match
      case None => div()
      case Some(tree) =>
        div(
          cls := "search-path",
          button(
            cls := "path-toggle",
            onClick --> { _ => edit(_.toggleTree) },
            span(cls := "overline", "Camino de búsqueda"),
            span(cls := "muted", s"${tree.size} · ${if m.treeOpen then "−" else "+"}")
          ),
          if !m.treeOpen then emptyNode
          else div(cls := "path-rows", pathRows(tree, tree.rootId, 0))
        )

  /** One row per position, depth-first, indented by depth.
    *
    * **A forward move is one row, not two.** Playing `∧.E₂` forward builds a
    * `let` and then fills its value (D24), so the tree holds two nodes for what
    * the player did once; the intermediate node — a `let` with its value still
    * a hole — is a state they never chose to be in, so the path draws the pair
    * as the move they actually made and jumps to the end of it.
    */
  private def pathRows(tree: GameTree, id: GameTree.NodeId, depth: Int): List[HtmlElement] =
    val node = tree.nodes(id)
    val children = node.children.values.toList.sortBy(_.value)

    collapsedLet(tree, node) match
      case Some(child) =>
        pathRow(tree, child, depth, label = moveLabel(tree, child)) ::
          child.children.values.toList.sortBy(_.value).flatMap(pathRows(tree, _, depth + 1))
      case None =>
        pathRow(tree, node, depth, label = moveLabel(tree, node)) ::
          children.flatMap(pathRows(tree, _, depth + 1))

  /** The elimination node that finishes a `let`, if this node is the start of
    * one such pair: a `let` with exactly one child, and that child filling the
    * `let`'s value hole.
    */
  private def collapsedLet(tree: GameTree, node: GameNode): Option[GameNode] =
    for
      via <- node.via
      move <- moveOf(tree, node)
      if NJ.binds(move).isDefined
      child <- Option.when(node.children.sizeIs == 1)(node.children.values.head)
      childNode = tree.nodes(child)
      childVia <- childNode.via
      if childVia.hole == via.hole :+ 0
    yield childNode

  private def moveOf(tree: GameTree, node: GameNode): Option[NJ[Sequent]] =
    for
      via <- node.via
      parent <- node.parentId
      move <- tree.nodes(parent).position.movesAt(via.hole).lift(via.moveIndex)
    yield move

  private def moveLabel(tree: GameTree, node: GameNode): String =
    moveOf(tree, node).fold("inicio")(NJ.label)

  private def pathRow(tree: GameTree, node: GameNode, depth: Int, label: String): HtmlElement =
    val position = node.position
    val holes = position.holes.length
    val status = position.status
    div(
      cls := "path-row",
      cls("current") := node.id == tree.currentId,
      cls("won") := status == Status.Won,
      cls("dead") := status == Status.Dead,
      styleAttr := s"padding-left: ${8 + depth * 12}px",
      onClick --> { _ => edit(_.jump(node.id)) },
      span(cls := "path-dot"),
      span(cls := "path-rule mono", label),
      span(
        cls := "path-meta",
        status match
          case Status.Won  => "✓ resuelto"
          case Status.Dead => "✗ sin salida"
          case Status.Open => s"${holes}h"
      )
    )

  /** A dead end closes a *branch*, not the game (§4.7): a toast with a way
    * back, and the board left exactly as it is.
    */
  private def deadEndToast(m: Model): HtmlElement =
    div(
      cls := "toast",
      div(
        div(cls := "toast-title", "Sin salida"),
        div(cls := "muted", "Este hueco no admite ninguna regla. Vuelve a un punto de elección.")
      ),
      button(
        cls := "primary",
        disabled := m.backtrackTarget.isEmpty,
        onClick --> { _ => edit(_.backtrack) },
        "Retroceder"
      )
    )

  /** Every continuation from here has been tried, and none of them worked.
    *
    * The other half of §4.9's lesson, and the half the dead-end toast cannot
    * see: a premature `∨.I₁` leaves a position that looks perfectly ordinary —
    * it has legal moves — and only turns out to be a wrong turn once the lines
    * below it have been played out. Saying so here is the difference between a
    * player learning something and a player grinding.
    */
  private def exhaustedToast(m: Model): HtmlElement =
    div(
      cls := "toast",
      div(
        div(cls := "toast-title", "Esta línea no lleva a ninguna parte"),
        div(cls := "muted", "Has probado todas sus continuaciones. Vuelve a un punto de elección.")
      ),
      button(
        cls := "primary",
        disabled := m.backtrackTarget.isEmpty,
        onClick --> { _ => edit(_.backtrack) },
        "Retroceder"
      )
    )

  /** The negative ending (§4.7): the search space was finite, it has been
    * walked, and there is no program of this type.
    *
    * Note what is *not* claimed — that the goal is unprovable in general. What
    * the player has established is that this game has no solution, which for a
    * finite space is the same thing, and which they established by playing
    * rather than by being told.
    */
  private def lost(m: Model): HtmlElement =
    val goal = m.goal.get
    div(
      cls := "scrim",
      div(
        cls := "card result",
        div(cls := "cross", "✗"),
        h2("¡Esto no es un teorema!"),
        p(
          cls := "lede",
          "Has agotado la búsqueda: no queda ningún movimiento por probar, y ninguno construye el tipo."
        ),
        div(cls := "eyebrow", "La signatura que no se puede habitar"),
        pre(cls := "mono", Notation.programmer(goal.formula)),
        div(cls := "eyebrow", "Como proposición"),
        pre(cls := "mono", Notation.logician(goal.formula)),
        p(
          cls := "muted",
          s"${m.tree.fold(0)(_.size)} posiciones exploradas, todas sin salida."
        ),
        button(cls := "primary", onClick --> { _ => edit(_ => Model.empty) }, "Nueva partida")
      )
    )

  /** The only confirmation in the application (interaction spec §6.1). */
  private def confirmDialog(what: Confirm): HtmlElement =
    div(
      cls := "scrim",
      onClick --> { _ => edit(_.dismiss) },
      div(
        cls := "card dialog",
        onClick.stopPropagation --> { _ => () },
        h2("¿Abandonar esta partida?"),
        p(cls := "lede", "Se perderá todo lo que has explorado."),
        div(
          cls := "dialog-actions",
          button(
            cls := "primary",
            onClick --> { _ =>
              Persist.clear()
              edit(m => what match
                case Confirm.Restart => m.restart.dismiss
                case Confirm.Quit    => Model.empty
              )
            },
            "Abandonar"
          ),
          button(cls := "ghost", onClick --> { _ => edit(_.dismiss) }, "Seguir jugando")
        )
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
            onClick --> { _ => path.foreach(p => edit(_.select(p))) },
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
    val byRule = offers(m).groupBy(_.rule)
    // Names already in force at the selected hole, so a move reads
    // `qr match { … }` rather than `v1 match { … }`.
    val names = m.selectedHole.map((_, hole) => ToScala.names(hole.ant.reverse)).getOrElse(Map.empty)
    val goal = m.selectedHole.map((_, hole) => hole.con)

    div(
      cls := "rules",
      Rules.rows.map { row =>
        div(
          cls := "rules-row",
          div(cls := "rules-label mono", row.label),
          div(cls := "rules-cell", cell(row.key + "I", row.construct, byRule, m.openCell, names, goal)),
          div(cls := "rules-cell", cell(row.key + "E", row.destruct, byRule, m.openCell, names, goal))
        )
      }
    )

  /** One entry in the table: the cell it is filed under, the engine move that
    * starts it, and — for a forward use — the resource whose elimination
    * finishes it.
    */
  private final case class Offer(rule: String, move: NJ[Sequent], index: Int, via: Option[Prem]):
    def andThen: Option[(String, Prem)] = via.map(rule -> _)

  /** What each cell of the table offers at the selected hole.
    *
    * Every move but a `let` is filed under the cell of its own rule. A `let`
    * has no cell — §3.2 has no such rule, and the engine identifies it by the
    * type it binds rather than by a resource. What it *is*, to the player, is
    * the forward use of a destructor: the value it binds can only come from one
    * elimination step on something in scope. So it is filed under that
    * destructor, once per resource that could supply it — the engine
    * deduplicates two resources offering the same type into one move, and here
    * they are two things a player might reach for. Which resource it was chosen
    * from is what `via` carries, and it is what lets the elimination be played
    * in the same stroke.
    *
    * This mirrors the candidate types in `NJ.lets`; if that ever admits another
    * source, this has to grow the same case.
    *
    * **A forward use is dropped where the same rule on the same resource closes
    * the hole outright.** When `pqr._2` *is* what the hole wants, binding it to
    * a name first is the same move with a detour through scope, and offering
    * both invites the player to take the long way round for nothing.
    */
  private def offers(m: Model): List[Offer] =
    val all = m.selectedHole.toList.flatMap { (_, hole) =>
      m.moves.zipWithIndex.flatMap { (move, index) =>
        NJ.binds(move) match
          case None => List(Offer(NJ.label(move), move, index, None))
          case Some(bound) =>
            hole.ant.flatMap { p =>
              p._2 match
                case Formula.And(a, b) =>
                  List(
                    Option.when(a == bound)(Offer("∧.E₁", move, index, Some(p))),
                    Option.when(b == bound)(Offer("∧.E₂", move, index, Some(p)))
                  ).flatten
                case Formula.Implies(_, b) =>
                  Option.when(b == bound)(Offer("⟶.E", move, index, Some(p))).toList
                case _ => Nil
            }
      }
    }
    val direct = all.collect { case o if o.via.isEmpty => (o.rule, NJ.actsOn(o.move)) }.toSet
    all.filter(o => o.via.isEmpty || !direct.contains((o.rule, o.via)))

  /** One cell: an absence, an inapplicable rule, a single move, or a stack that
    * unfolds in place.
    */
  private def cell(
      key: String,
      spec: Rules.Cell,
      byRule: Map[String, List[Offer]],
      openCell: Option[String],
      names: Map[Int, String],
      goal: Option[Formula]
  ): HtmlElement =
    spec match
      case Rules.Cell.Absent => div(cls := "absent", "—")
      case Rules.Cell.Holds(label, rules) =>
        rules.flatMap(byRule.getOrElse(_, Nil)) match
          case Nil            => div(cls := "inapplicable mono", label)
          case offer :: Nil   => moveButton(offer, names, goal)
          case several =>
            val open = openCell.contains(key)
            div(
              cls := "cell-wrap",
              button(
                cls := "move stack",
                cls("open") := open,
                onClick --> { _ => edit(_.toggleCell(key)) },
                span(cls := "move-name mono", label),
                span(cls := "count", several.length.toString)
              ),
              if !open then emptyNode
              else div(cls := "cell-menu", several.map(moveButton(_, names, goal)))
            )

  private def moveButton(offer: Offer, names: Map[Int, String], goal: Option[Formula]): HtmlElement =
    button(
      cls := "move",
      onClick --> { _ => edit(_.play(offer.index, offer.andThen)) },
      span(cls := "move-name mono", offer.rule),
      span(cls := "move-effect mono", effect(offer, names, goal))
    )

  /** What the player will see happen.
    *
    * For a forward use that means the `val` **and** the elimination that fills
    * it — `val qr: Either[Q, R] = pqr._2`, not `= …` — because one stroke plays
    * both. The value is built here rather than read off the engine: it exists
    * only after the `let` has been played, and this text is what persuades a
    * player to play it.
    */
  private def effect(offer: Offer, names: Map[Int, String], goal: Option[Formula]): String =
    val lambda = (offer.move, offer.via) match
      case (NJ.Let(bound, _, _), Some(on)) =>
        Lambda.Let(bound, elimination(offer.rule, on), Lambda.Hole(goal.getOrElse(bound._2)))
      case _ => ToLambda.apply(offer.move.map(s => Lambda.Hole(s.con)))
    val rendered = ToScala.fragment(lambda, names)
    // A `let` renders as `val x: T = v._1; …`; the binding alone says what it does.
    val shown = if offer.via.isDefined then rendered.takeWhile(_ != ';') else rendered
    if shown.length > 44 then shown.take(43) + "…" else shown

  /** The elimination a forward use applies to its resource — the second half of
    * the stroke, rendered before it is played.
    */
  private def elimination(rule: String, on: Prem): Lambda = (rule, on._2) match
    case ("∧.E₁", _)               => ToLambda.apply(NJ.AndE1Back(on))
    case ("∧.E₂", _)               => ToLambda.apply(NJ.AndE2Back(on))
    case ("⟶.E", Formula.Implies(a, _)) => ToLambda.apply(NJ.ImpliesEBack(on, Lambda.Hole(a)))
    case _                         => Lambda.Hole(on._2)

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
        button(cls := "primary", onClick --> { _ => edit(_ => Model.empty) }, "Nueva partida")
      )
    )

  // --- Odds and ends --------------------------------------------------------

  private def backButton(to: Screen): HtmlElement =
    button(cls := "ghost back", onClick --> { _ => edit(_.copy(screen = to)) }, "← Volver")

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
  *
  * Six rows, two columns, and nothing else — a `let` has no row of its own; it
  * is offered under the destructor that will produce what it binds (see
  * `App.cellsOf`). The table is the syllabus, so it shows the rules of the
  * calculus and not the engine's way of encoding forward reasoning.
  */
object Rules:

  /** A cell either holds rules — a display name, and the engine labels whose
    * moves land there — or holds none at all, which is an absence rather than
    * an empty state.
    */
  enum Cell:
    case Holds(label: String, rules: List[String])
    case Absent

  /** `key` names the row for the unfolded-cell state; `label` is what the row
    * shows the player.
    */
  final case class Row(key: String, label: String, construct: Cell, destruct: Cell)

  import Cell.*

  val rows: List[Row] = List(
    Row("fun", "=>", Holds("⟶.I", List("⟶.I")), Holds("⟶.E", List("⟶.E"))),
    Row("prod", "( , )", Holds("∧.I", List("∧.I")), Holds("∧.E", List("∧.E₁", "∧.E₂"))),
    Row("sum", "Either", Holds("∨.I", List("∨.I₁", "∨.I₂")), Holds("∨.E", List("∨.E"))),
    Row("unit", "Unit", Holds("⊤.I", List("⊤.I")), Absent),
    Row("void", "Nothing", Absent, Holds("⊥.E", List("⊥.E"))),
    Row("hyp", "hyp", Absent, Holds("Ax", List("Ax")))
  )
