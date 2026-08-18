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
      child <-- model.signal.map(topBar),
      child <-- model.signal.map(render),
      // `Esc` dismisses a dialog and never confirms — the interaction spec is
      // explicit, and it is the one keyboard behaviour that ships (D18a).
      documentEvents(_.onKeyDown).filter(_.key == "Escape") --> { _ => edit(_.dismiss) }
    )

  private def render(m: Model): HtmlElement = m.screen match
    case Screen.Home  => home(m)
    case Screen.Setup => setup(m)
    case Screen.Play =>
      // The three endings of §6.2, in the order they are decided: a solution
      // beats everything, and refutation is only ever reached with no solution
      // anywhere in the tree.
      if m.won then won(m)
      else if m.refuted then lost(m)
      else play(m)

  // --- The chrome -----------------------------------------------------------

  /** The bar across every screen: the institutional lockup, and the switch
    * between the two readings.
    *
    * The switch lives here rather than in the Play screen's column, as the
    * handoff has it — it belongs to the application, not to one screen, and on
    * Home it already changes something: which way round the lede tells you the
    * correspondence.
    */
  private def topBar(m: Model): HtmlElement =
    div(
      cls := "topbar",
      img(
        cls := "brand-lockup",
        src := "./assets/logo-etsii.svg",
        alt := "Universidad Rey Juan Carlos — Escuela Técnica Superior de Ingeniería Informática"
      ),
      // While a game is on, the bar carries the two numbers that say where it
      // stands: how much is left to do, and how much has been done.
      if m.screen == Screen.Play && m.tree.nonEmpty then
        div(
          cls := "counters",
          span(cls := "divider"),
          span(strong(m.holes.length.toString), " ", if m.holes.sizeIs == 1 then "hueco abierto" else "huecos abiertos"),
          span(strong(m.tree.fold(0)(_.depth).toString), " movimientos")
        )
      else emptyNode,
      div(cls := "topbar-spacer"),
      viewSwitch(m),
      langSwitch
    )

  /** The language switch, shown inactive.
    *
    * D14 puts Spanish and English in the first release, and Phase 9 builds the
    * i18n behind them; until then the control is here, in its designed place,
    * doing nothing — the project's own rule, leave room and build nothing,
    * applied to a switch rather than a screen.
    */
  private def langSwitch: HtmlElement =
    div(
      cls := "seg-group lang",
      button(cls := "seg-btn", disabled := true, "EN"),
      button(cls := "seg-btn on", disabled := true, "ES")
    )

  // --- Home -----------------------------------------------------------------

  /** The systems on offer: each a logic paired with the language whose programs
    * are its proofs. Only IPL is playable; the rest are listed on a par, so the
    * roadmap is legible from the first screen.
    */
  private val systems: List[(String, String, List[HtmlElement], Boolean)] = List(
    (
      "Lógica intuicionista proposicional",
      "Cálculo lambda con tipos simples y tipos de datos algebraicos",
      List(
        div(Shapes.glyph(Formula.Implies(atomA, atomB))),
        div(Shapes.glyph(Formula.And(atomA, atomB))),
        div(Shapes.glyph(Formula.Or(atomA, atomB)))
      ),
      true
    ),
    (
      "Lógica clásica proposicional",
      "Cálculo λμ — continuaciones de primera clase (call/cc)",
      List(div(Shapes.glyph(Formula.Implies(atomA, atomB))), div(Shapes.glyph(Formula.False))),
      false
    ),
    (
      "Lógica intuicionista de primer orden",
      "Cálculo lambda con tipos dependientes (λΠ)",
      List(span(cls := "start-sym mono", "∀∃")),
      false
    ),
    (
      "Lógica clásica de primer orden",
      "Cálculo λμ con tipos dependientes",
      List(span(cls := "start-sym mono", "∀¬")),
      false
    ),
    (
      "Lógica lineal multiplicativa",
      "Cálculo lambda lineal — cada recurso se usa exactamente una vez",
      List(span(cls := "start-sym mono", "⊗⊸")),
      false
    )
  )

  private def atomA = Formula.Atom("A")
  private def atomB = Formula.Atom("B")

  private def home(m: Model): HtmlElement =
    div(
      cls := "sheet",
      img(cls := "hero-mark", src := "./assets/logo-urjc.svg", alt := "URJC"),
      h1(cls := "title", "The Curry–Howard Game"),
      p(cls := "tagline", "Construir un programa y demostrar un teorema son el mismo movimiento."),
      // The lede tells the correspondence from whichever end you are standing
      // at — which is the first thing the view switch does.
      p(
        cls := "lede",
        if m.view.isLogician then
          "Parte de un objetivo con una obligación abierta y ve descargándola con reglas " +
            "de introducción y eliminación. Cuando no quede ninguna, tendrás una demostración " +
            "— y con ella, un programa."
        else
          "Parte de un hueco con un tipo y ve rellenándolo con constructores y destructores. " +
            "Cuando no queden huecos, tendrás un programa — y con él, una demostración."
      ),
      resumeBanner,
      p(cls := "overline", "Empezar partida"),
      div(
        cls := "systems",
        systems.map((logic, language, glyphs, active) => systemRow(logic, language, glyphs, active))
      ),
      // Help is Phase 10's screen. The button stands in its designed place,
      // inactive, so the shape of the finished screen is honest.
      div(cls := "home-footer", button(cls := "ghost", disabled := true, "Cómo se juega"))
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

  private def systemRow(
      logic: String,
      language: String,
      glyphs: List[HtmlElement],
      active: Boolean
  ): HtmlElement =
    button(
      cls := "start-btn",
      cls("inactive") := !active,
      disabled := !active,
      onClick.filter(_ => active) --> { _ => edit(_.copy(screen = Screen.Setup)) },
      div(cls := "start-glyphs", glyphs),
      div(cls := "start-body", span(cls := "system-logic", logic), span(cls := "system-language", language)),
      if active then span(cls := "arrow", Icons.arrowRight())
      else span(cls := "soon", "Próximamente")
    )

  // --- Setup ----------------------------------------------------------------

  private def setup(m: Model): HtmlElement =
    div(
      cls := "sheet",
      backButton(Screen.Home),
      h2(
        cls := "title",
        if m.view.isLogician then "¿Qué quieres demostrar?" else "¿Qué programa quieres escribir?"
      ),
      p(
        cls := "lede lede-tight",
        if m.view.isLogician then "Escribe la proposición que quieres demostrar. Se aceptan las dos notaciones."
        else "Escribe la signatura que quieres habitar. Se aceptan las dos notaciones."
      ),
      // The field and the shape of what is in it, framed as one control: the
      // glyph appears as soon as the goal parses, so the player sees the shape
      // of the thing before playing a single move.
      div(
        cls := "goal-field",
        cls("invalid") := m.input.nonEmpty && m.parsed.isLeft,
        input(
          cls := "goal-input mono",
          placeholder := (if m.view.isLogician then "a ∧ b → b ∧ a" else "(A, B) => (B, A)"),
          value := m.input,
          onInput.mapToValue --> { v => edit(_.copy(input = v)) },
          onKeyDown.filter(_.key == "Enter").mapTo(()) --> { _ => begin() }
        ),
        div(cls := "goal-shape", m.parsed.toOption.map(g => Shapes.glyph(g.formula, 24)).toList)
      ),
      feedback(m),
      button(
        cls := "primary",
        disabled := m.parsed.isLeft,
        onClick --> { _ => begin() },
        "Empezar partida"
      ),
      div(
        cls := "examples-block",
        p(cls := "overline", "O empieza con un ejemplo"),
        div(
          cls := "examples",
          Examples.all.map { ex =>
            button(
              cls := "example",
              onClick --> { _ =>
                edit(m => m.copy(input = if m.view.isLogician then ex.logician else ex.programmer))
              },
              span(cls := "mono", if m.view.isLogician then ex.logician else ex.programmer),
              span(cls := "example-note", note(ex.id))
            )
          }
        )
      )
    )

  private def note(id: String): String = id match
    case "distributivity"  => "distributividad"
    case "commutativity"   => "conmutatividad — para empezar"
    case "k"               => "el combinador K"
    case "transitivity"    => "transitividad"
    case "excludedMiddle"  => "tercio excluso — aquí no es demostrable"
    case other             => other

  /** The core teaching moment of this screen: the same goal, echoed in both
    * notations at once.
    */
  /** Three states in one fixed-height area, so nothing below it ever jumps:
    * the grammar while the field is empty, the error while it does not parse,
    * and — the point of the screen — the same goal in *both* notations as soon
    * as it does.
    */
  private def feedback(m: Model): HtmlElement =
    div(
      cls := "feedback",
      m.parsed match
        case Right(goal) =>
          div(
            echoRow("PROGRAMADOR", TypeText(goal.formula, View.Programmer)),
            echoRow("LÓGICO", TypeText(goal.formula, View.Logician))
          )
        case Left(ParseError.Empty) => grammarHint
        case Left(err) =>
          div(
            cls := "error",
            Icons.alert(15),
            span(describe(err)),
            span(cls := "muted", s"en la posición ${err.pos}")
          )
    )

  /** What the field accepts, in both notations at once — the same lesson the
    * echo rows teach, taught before there is anything to echo.
    */
  private def grammarHint: HtmlElement =
    div(
      cls := "hints",
      List(
        (List("=>", "->", "→"), "implicación"),
        (List("(A, B)", "∧", "&"), "conjunción"),
        (List("Either[A, B]", "∨", "|"), "disyunción"),
        (List("¬A", "~A", "!A"), "negación"),
        (List("Unit", "⊤"), "verdad"),
        (List("Nothing", "⊥"), "falsedad")
      ).map { (tokens, label) =>
        div(
          cls := "hint-item",
          div(cls := "hint-tokens", tokens.map(t => span(cls := "kbd mono", t))),
          span(cls := "hint-label", label)
        )
      }
    )

  private def echoRow(label: String, text: HtmlElement): HtmlElement =
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
        div(
          cls := "panel-head",
          h3(cls := "overline", if m.view.isLogician then "Reglas" else "Constructores y destructores"),
          span(cls := "muted", s"${offers(m).length} disponibles")
        ),
        movesPanel(m),
        div(
          cls := "footer",
          // Backtrack and Restart are always reachable, per the interaction
          // spec. Backtrack needs no confirmation — nothing is lost by it.
          button(
            cls := "ghost",
            disabled := m.backtrackTarget.isEmpty,
            onClick --> { _ => edit(_.backtrack) },
            Icons.undo(14),
            "Retroceder"
          ),
          button(
            cls := "ghost",
            onClick --> { _ => edit(_.ask(Confirm.Restart)) },
            Icons.restart(14),
            "Reiniciar"
          ),
          button(cls := "ghost", onClick --> { _ => edit(_.ask(Confirm.Quit)) }, "Cancelar")
        )
      ),
      div(
        cls := "right",
        goalCard(m),
        if m.view.isLogician then proofCard(m) else termCard(m),
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
            span(cls := "muted path-count", s"${tree.size} nodos · profundidad ${tree.depth}"),
            span(cls := "path-chevron", if m.treeOpen then Icons.chevronDown() else Icons.chevronRight())
          ),
          if !m.treeOpen then emptyNode
          else div(cls := "path-rows", pathRows(tree, tree.rootId, 0, m.view))
        )

  /** One row per position, depth-first, indented by depth.
    *
    * **A forward move is one row, not two.** Playing `∧.E₂` forward builds a
    * `let` and then fills its value (D24), so the tree holds two nodes for what
    * the player did once; the intermediate node — a `let` with its value still
    * a hole — is a state they never chose to be in, so the path draws the pair
    * as the move they actually made and jumps to the end of it.
    */
  private def pathRows(tree: GameTree, id: GameTree.NodeId, depth: Int, view: View): List[HtmlElement] =
    val node = tree.nodes(id)
    val children = node.children.values.toList.sortBy(_.value)

    collapsedLet(tree, node) match
      case Some(child) =>
        pathRow(tree, child, depth, label = moveLabel(tree, child, view)) ::
          child.children.values.toList.sortBy(_.value).flatMap(pathRows(tree, _, depth + 1, view))
      case None =>
        pathRow(tree, node, depth, label = moveLabel(tree, node, view)) ::
          children.flatMap(pathRows(tree, _, depth + 1, view))

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

  private def moveLabel(tree: GameTree, node: GameNode, view: View): String =
    moveOf(tree, node).fold("inicio")(move => ruleName(NJ.label(move), view))

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
      div(
        cls := "goal-top",
        div(cls := "eyebrow", if m.view.isLogician then "Proposición que demostrar" else "Signatura que habitar"),
        div(cls := "eyebrow accent", "Tu objetivo")
      ),
      if m.view.isLogician then div(cls := "goal-prop", TypeText(goal.formula, View.Logician))
      else
        div(
          cls := "mono goal",
          Code(s"def solution$params: ${Notation.programmer(goal.formula)}", Code.atomsOf(goal))
        )
    )

  /** The switch. It changes the notation of everything on screen and the game
    * of nothing — which is the claim the whole project rests on, so the button
    * says which reading you are in rather than which one you would get.
    */
  private def viewSwitch(m: Model): HtmlElement =
    div(
      cls := "view-switch",
      button(
        cls := "seg",
        cls("on") := !m.view.isLogician,
        onClick.filter(_ => m.view.isLogician) --> { _ => edit(_.toggleView) },
        "Programador"
      ),
      button(
        cls := "seg",
        cls("on") := m.view.isLogician,
        onClick.filter(_ => !m.view.isLogician) --> { _ => edit(_.toggleView) },
        "Lógico"
      )
    )

  /** The same position as a natural-deduction derivation (D25): one main
    * figure, and the facts derived beside it.
    */
  private def proofCard(m: Model): HtmlElement =
    val forest = m.forest.get
    val names = m.selectedHole.map((_, hole) => ToScala.names(hole.ant.reverse)).getOrElse(Map.empty)
    val selected = m.selectedIndex
    val pick: Int => Unit = i => edit(_.selectAt(i))

    div(
      div(
        cls := "card proof-card",
        div(cls := "eyebrow", "Demostración en construcción"),
        div(cls := "proof-scroll", Derivation(forest.main, pick, selected))
      ),
      Derivation.shelf(forest.derived, names, pick, selected)
    )

  /** The term, with its holes as chips. Exactly one is selected, and the moves
    * panel refers to it.
    */
  private def termCard(m: Model): HtmlElement =
    val position = m.position.get
    val holes = m.holes
    val selectedPath = m.selectedHole.map(_._1)

    val atoms = m.goal.map(Code.atomsOf).getOrElse(Set.empty)
    val dead = position.deadHoles.toSet

    div(
      cls := "card term-card mono",
      ToScala.formatted(ToLambda.position(position)).map {
        case Piece.Text(t) => span(Code(t, atoms))
        case Piece.HoleAt(i, goal) =>
          val path = holes.lift(i).map(_._1)
          span(
            cls := "hole",
            cls("selected") := path == selectedPath,
            // A hole no rule applies to is struck out rather than red-outlined:
            // the red outline means "you are here", and a dead end is not a
            // place to be.
            cls("dead") := path.exists(dead.contains),
            onClick --> { _ => path.foreach(p => edit(_.select(p))) },
            Shapes.glyph(goal, 15),
            span("… : "),
            TypeText(goal, View.Programmer)
          )
      }
    )

  private def hint(m: Model): HtmlElement =
    m.selectedHole match
      case None => div()
      case Some((_, hole)) =>
        div(
          cls := "hint",
          if m.view.isLogician then "Objetivo pendiente " else "Hueco seleccionado ",
          Shapes.glyph(hole.con, 16),
          span(cls := "mono strong", TypeText(hole.con, m.view)),
          " — elige una regla a la izquierda."
        )

  private def resourcesCard(m: Model): HtmlElement =
    m.selectedHole match
      case None => div()
      case Some((_, hole)) =>
        val names = ToScala.names(hole.ant.reverse)
        // In the logician's reading a resource is a formula you may use as a
        // premise; the name is the programmer's handle on it, kept so the two
        // panels line up entry for entry (D25).
        div(
          cls := "resources",
          div(cls := "eyebrow", if m.view.isLogician then "Hipótesis en el ámbito" else "Recursos en el ámbito"),
          if hole.ant.isEmpty then
            div(cls := "muted", "Todavía no hay recursos. Las entradas y las ligaduras aparecerán aquí.")
          else
            div(
              cls := "chips",
              hole.ant.reverse.map { (v, ty) =>
                div(
                  cls := "chip",
                  ty match
                    case Formula.Atom(name) => Shapes.atom(name, 26)
                    case other              => Shapes.glyph(other, 26)
                  ,
                  div(
                    cls := "chip-body",
                    div(cls := "mono strong", names.getOrElse(v, s"v$v")),
                    div(cls := "mono chip-ty", TypeText(ty, m.view))
                  ),
                  span(cls := "kind", if m.view.isLogician then logicKind(ty) else kind(ty))
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
      div(
        cls := "rules-row rules-head",
        div(),
        // The columns are one thing under two names — which is the lesson the
        // header is carrying, so it switches with the view.
        div(cls := "col-head", if m.view.isLogician then "Introducción" else "Construir"),
        div(cls := "col-head", if m.view.isLogician then "Eliminación" else "Destruir")
      ),
      Rules.rows.map { row =>
        div(
          cls := "rules-row",
          div(
            cls := "rules-label mono",
            row.shape.map(Shapes.glyph(_, 19)).toList,
            span(if m.view.isLogician then row.logic else row.label)
          ),
          div(cls := "rules-cell", cell(row.key + "I", row.construct, byRule, m.openCell, names, goal, m.view)),
          div(cls := "rules-cell", cell(row.key + "E", row.destruct, byRule, m.openCell, names, goal, m.view))
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
      goal: Option[Formula],
      view: View
  ): HtmlElement =
    spec match
      case Rules.Cell.Absent => div(cls := "absent", title := "Esa regla no existe", "—")
      case Rules.Cell.Holds(label, rules) =>
        // Every cell is one fixed-height row showing **only the rule name**, so
        // the table's geometry never changes as the game is played — the table
        // is the syllabus, and a syllabus that reflows while you read it is
        // teaching you nothing. What a move does is in the tooltip when there
        // is one instance, and in the unfolded list when there are several.
        rules.flatMap(byRule.getOrElse(_, Nil)) match
          case Nil =>
            div(cls := "tcell off mono", title := "No se aplica al hueco seleccionado", ruleName(label, view))

          case offer :: Nil =>
            button(
              cls := "tcell on mono",
              title := effectTip(offer, names, goal, view),
              onClick --> { _ => edit(_.play(offer.index, offer.andThen)) },
              ruleName(offer.rule, view)
            )

          case several =>
            val open = openCell.contains(key)
            div(
              cls := "cell-wrap",
              button(
                cls := "tcell on stack mono",
                cls("open") := open,
                title := s"${several.length} instancias — elige una",
                onClick --> { _ => edit(_.toggleCell(key)) },
                span(ruleName(label, view)),
                span(cls := "count", several.length.toString)
              ),
              if !open then emptyNode
              else div(cls := "cell-menu", several.map(moveButton(_, names, goal, view)))
            )

  private def moveButton(
      offer: Offer,
      names: Map[Int, String],
      goal: Option[Formula],
      view: View
  ): HtmlElement =
    button(
      cls := "move",
      onClick --> { _ => edit(_.play(offer.index, offer.andThen)) },
      span(cls := "move-name mono", ruleName(offer.rule, view)),
      span(
        cls := "move-effect mono",
        if view.isLogician then logicalEffect(offer, goal) else effect(offer, names, goal)
      )
    )

  /** What a single-instance cell says when you hover it — the effect that no
    * longer fits inside the cell now that its height is fixed.
    */
  private def effectTip(
      offer: Offer,
      names: Map[Int, String],
      goal: Option[Formula],
      view: View
  ): String =
    if view.isLogician then logicalEffect(offer, goal) else effect(offer, names, goal)

  /** `⟶.E` to the programmer, `→E` to the logician — the same rule under §3.1's
    * name and §3.2's.
    */
  private def ruleName(rule: String, view: View): String =
    if view.isLogician then rule.replace(".", "").replace("⟶", "→") else rule

  /** What a move does, said in formulas rather than in Scala.
    *
    * A forward use *derives* something — which is the fact it will put on the
    * shelf. A destructor *uses* a premise. An introduction leaves you with
    * whatever its premises are still to prove. No prose, so nothing here needs
    * translating at the edge (D14).
    */
  private def logicalEffect(offer: Offer, goal: Option[Formula]): String =
    offer.via match
      case Some((_, from)) => "⊢ " + Notation.logician(bound(offer.move).getOrElse(from))
      case None =>
        NJ.actsOn(offer.move) match
          case Some((_, premise)) => "de " + Notation.logician(premise)
          case None =>
            val opens = NJ.subgoals(offer.move).map(s => Notation.logician(s.con))
            if opens.isEmpty then "cierra" else opens.mkString(", ")

  private def bound(move: NJ[Sequent]): Option[Formula] = NJ.binds(move)

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
    val atoms = Code.atomsOf(goal)

    div(
      cls := "scrim",
      div(
        cls := "card result",
        div(cls := "tick", "✓"),
        h2("Resuelto"),
        p(cls := "lede", "El tipo está habitado — y la proposición, demostrada."),
        // Both readings of the finished play, since the ending is where the
        // correspondence is easiest to see: one is the program you wrote, the
        // other the proof you drew — and the `let` has gone from both.
        if m.view.isLogician then
          div(
            div(cls := "eyebrow", "La demostración"),
            div(cls := "proof-scroll", Derivation(m.forest.get.main, _ => (), None))
          )
        else
          div(
            div(cls := "eyebrow", "Como lo construiste"),
            pre(cls := "mono code", Code(ToScala.plain(ToScala.formatted(term)), atoms)),
            div(cls := "eyebrow", "Limpio"),
            pre(
              cls := "mono code",
              // §4.9's own last step: the scaffolding goes, and *one* type
              // annotation stays — the type of the original hole. Everything
              // else the compiler infers.
              Code(
                s"def solution$params: ${Notation.programmer(goal.formula)} =\n  " +
                  ToScala.plain(ToScala.formatted(Cleanup.simplify(term), ascribe = false)).replace("\n", "\n  "),
                atoms
              )
            )
          ),
        div(
          cls := "dialog-actions",
          button(cls := "primary", onClick --> { _ => edit(_ => Model.empty) }, "Nueva partida"),
          button(
            cls := "ghost",
            onClick --> { _ => edit(_.toggleView) },
            if m.view.isLogician then "Ver el programa" else "Ver la demostración"
          )
        )
      )
    )

  // --- Odds and ends --------------------------------------------------------

  private def backButton(to: Screen): HtmlElement =
    button(
      cls := "ghost back",
      onClick --> { _ => edit(_.copy(screen = to)) },
      Icons.arrowLeft(15),
      "Volver al inicio"
    )

  /** The same shape, named in the other vocabulary — which is the whole point
    * of the switch, said in one word per chip.
    */
  private def logicKind(ty: Formula): String = ty match
    case Formula.And(_, _)     => "conjunción"
    case Formula.Or(_, _)      => "disyunción"
    case Formula.Implies(_, _) => "implicación"
    case Formula.Atom(_)       => "átomo"
    case Formula.True          => "verdad"
    case Formula.False         => "falsedad"

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

  /** `key` names the row for the unfolded-cell state; `label` and `logic` are
    * what the row shows in each reading — `( , )` and `∧` being the same thing
    * said twice, which is the point of the table.
    */
  final case class Row(
      key: String,
      label: String,
      logic: String,
      shape: Option[Formula],
      construct: Cell,
      destruct: Cell
  )

  import Cell.*

  private val a = Formula.Atom("A")
  private val b = Formula.Atom("B")

  val rows: List[Row] = List(
    Row("fun", "=>", "→", Some(Formula.Implies(a, b)), Holds("⟶.I", List("⟶.I")), Holds("⟶.E", List("⟶.E"))),
    Row("prod", "( , )", "∧", Some(Formula.And(a, b)), Holds("∧.I", List("∧.I")), Holds("∧.E", List("∧.E₁", "∧.E₂"))),
    Row("sum", "Either", "∨", Some(Formula.Or(a, b)), Holds("∨.I", List("∨.I₁", "∨.I₂")), Holds("∨.E", List("∨.E"))),
    Row("unit", "Unit", "⊤", Some(Formula.True), Holds("⊤.I", List("⊤.I")), Absent),
    Row("void", "Nothing", "⊥", Some(Formula.False), Absent, Holds("⊥.E", List("⊥.E"))),
    Row("hyp", "hyp", "hip.", None, Absent, Holds("Ax", List("Ax")))
  )
