package curryhoward.engine
package ipl

import Formula.*

/** Why a goal did not parse, and where.
  *
  * A code and a position, never a sentence: the message is chosen at the edge,
  * in the player's language (D14). The position is an index into the source, so
  * the Setup screen can point at the offending character.
  */
enum ParseError(val pos: Int):
  case Empty extends ParseError(0)
  case UnknownChar(char: Char, at: Int) extends ParseError(at)
  case ExpectedBracket(at: Int) extends ParseError(at)
  case ExpectedComma(at: Int) extends ParseError(at)
  case ExpectedCloseBracket(at: Int) extends ParseError(at)
  case ExpectedCloseParen(at: Int) extends ParseError(at)
  case UnexpectedEnd(at: Int) extends ParseError(at)
  case Unexpected(text: String, at: Int) extends ParseError(at)
  case TrailingInput(at: Int) extends ParseError(at)

/** A goal, ready to play: the formula and the type parameters it quantifies.
  *
  * The parameters are the distinct atoms in order of first appearance, which is
  * what the signature `def solution[P, Q, R]: …` binds.
  */
final case class Goal(formula: Formula, tyParams: List[String])

object Goal:
  def from(formula: Formula): Goal = Goal(formula, atoms(formula).distinct)

  private def atoms(f: Formula): List[String] = f match
    case Atom(name)    => List(name)
    case Implies(a, b) => atoms(a) ++ atoms(b)
    case And(a, b)     => atoms(a) ++ atoms(b)
    case Or(a, b)      => atoms(a) ++ atoms(b)
    case True | False  => Nil

/** Reading a goal typed by the player, in **either** notation.
  *
  * The two syntaxes are accepted freely mixed, because insisting on one would
  * be teaching the wrong lesson: `p ∧ (q ∨ r) → …` and
  * `((P, Either[Q, R])) => …` denote the same formula and the game's whole
  * claim is that they are the same thing.
  *
  * Accepted, after the prototype:
  *
  *   - implication `=>` `->` `→` `⇒`, right-associative and loosest;
  *   - disjunction `∨` `|` `+` `\/` `or`, and `Either[A, B]`;
  *   - conjunction `∧` `&` `×` `/\` `and`, and `(A, B)`;
  *   - negation `¬` `~` `!` `not`, which desugars to `A → ⊥`;
  *   - `⊤` `Unit` `True` `Top` and `⊥` `Nothing` `Void` `False` `Bottom`;
  *   - parentheses, and identifiers.
  *
  * Conjunction binds tighter than disjunction, which binds tighter than
  * implication — the same precedence [[Notation]] prints with, so the two are
  * inverse.
  *
  * Atoms are canonicalised to initial capitals, so `p` and `P` are one atom.
  * That is what makes `parse(logician(f)) == parse(programmer(f)) == f`.
  */
object Parser:

  def parse(src: String): Either[ParseError, Formula] =
    if src.trim.isEmpty then Left(ParseError.Empty)
    else tokenize(src).flatMap(toks => Impl(src, toks).run())

  def parseGoal(src: String): Either[ParseError, Goal] = parse(src).map(Goal.from)

  // --- Tokens ---------------------------------------------------------------

  private enum Tok:
    case Arrow, And, Or, Not, TrueT, FalseT
    case LParen, RParen, LBracket, RBracket, Comma, EitherT, Eof
    case Id(name: String)

  private case class Token(tok: Tok, pos: Int)

  private def tokenize(src: String): Either[ParseError, Vector[Token]] =
    val out = Vector.newBuilder[Token]
    var i = 0
    var failure: Option[ParseError] = None

    while i < src.length && failure.isEmpty do
      val c = src(i)
      val two = if i + 1 < src.length then src.substring(i, i + 2) else ""

      def take(t: Tok, width: Int): Unit =
        out += Token(t, i)
        i += width

      if c.isWhitespace then i += 1
      else if two == "=>" || two == "->" then take(Tok.Arrow, 2)
      else if two == "/\\" then take(Tok.And, 2)
      else if two == "\\/" then take(Tok.Or, 2)
      else
        c match
          case '→' | '⇒'             => take(Tok.Arrow, 1)
          case '∧' | '&' | '×'       => take(Tok.And, 1)
          case '∨' | '|' | '+'       => take(Tok.Or, 1)
          case '¬' | '~' | '!'       => take(Tok.Not, 1)
          case '⊤'                   => take(Tok.TrueT, 1)
          case '⊥'                   => take(Tok.FalseT, 1)
          case '('                   => take(Tok.LParen, 1)
          case ')'                   => take(Tok.RParen, 1)
          case '['                   => take(Tok.LBracket, 1)
          case ']'                   => take(Tok.RBracket, 1)
          case ','                   => take(Tok.Comma, 1)
          case ch if ch.isLetter =>
            val start = i
            var j = i
            while j < src.length && (src(j).isLetterOrDigit || src(j) == '_' || src(j) == '\'') do j += 1
            val word = src.substring(start, j)
            val tok = word.toLowerCase match
              case "unit" | "true" | "top"                     => Tok.TrueT
              case "nothing" | "void" | "false" | "bottom"     => Tok.FalseT
              case "not"                                       => Tok.Not
              case "and"                                       => Tok.And
              case "or"                                        => Tok.Or
              case "either"                                    => Tok.EitherT
              case _                                           => Tok.Id(word)
            out += Token(tok, start)
            i = j
          case other => failure = Some(ParseError.UnknownChar(other, i))

    failure.toLeft((out += Token(Tok.Eof, src.length)).result())

  // --- Grammar --------------------------------------------------------------

  private class Impl(src: String, toks: Vector[Token]):
    private var i = 0
    private def peek: Token = toks(i)
    private def advance(): Unit = i += 1
    private def eat(t: Tok): Boolean =
      if peek.tok == t then { advance(); true } else false

    def run(): Either[ParseError, Formula] =
      expr().flatMap: f =>
        if peek.tok == Tok.Eof then Right(f)
        else Left(ParseError.TrailingInput(peek.pos))

    /** Implication: right-associative, loosest. */
    private def expr(): Either[ParseError, Formula] =
      disjunction().flatMap: left =>
        if eat(Tok.Arrow) then expr().map(Implies(left, _))
        else Right(left)

    private def disjunction(): Either[ParseError, Formula] =
      def more(left: Formula): Either[ParseError, Formula] =
        if eat(Tok.Or) then conjunction().flatMap(right => more(Or(left, right)))
        else Right(left)
      conjunction().flatMap(more)

    private def conjunction(): Either[ParseError, Formula] =
      def more(left: Formula): Either[ParseError, Formula] =
        if eat(Tok.And) then negation().flatMap(right => more(And(left, right)))
        else Right(left)
      negation().flatMap(more)

    /** `¬A` is `A → ⊥`, which is exactly what the programmer notation writes as
      * `A => Nothing`. Desugaring here means the engine never sees negation as
      * a connective of its own.
      */
    private def negation(): Either[ParseError, Formula] =
      if eat(Tok.Not) then negation().map(Implies(_, False)) else atom()

    private def atom(): Either[ParseError, Formula] =
      val t = peek
      t.tok match
        case Tok.Id(name) => advance(); Right(Atom(canonical(name)))
        case Tok.TrueT    => advance(); Right(True)
        case Tok.FalseT   => advance(); Right(False)

        case Tok.EitherT =>
          advance()
          if !eat(Tok.LBracket) then Left(ParseError.ExpectedBracket(peek.pos))
          else
            for
              a <- expr()
              _ <- Either.cond(eat(Tok.Comma), (), ParseError.ExpectedComma(peek.pos))
              b <- expr()
              _ <- Either.cond(eat(Tok.RBracket), (), ParseError.ExpectedCloseBracket(peek.pos))
            yield Or(a, b)

        case Tok.LParen =>
          advance()
          expr().flatMap: a =>
            if eat(Tok.Comma) then
              for
                b <- expr()
                _ <- Either.cond(eat(Tok.RParen), (), ParseError.ExpectedCloseParen(peek.pos))
              yield And(a, b)
            else if eat(Tok.RParen) then Right(a)
            else Left(ParseError.ExpectedCloseParen(peek.pos))

        case Tok.Eof => Left(ParseError.UnexpectedEnd(t.pos))
        case _       => Left(ParseError.Unexpected(src.slice(t.pos, t.pos + 1), t.pos))

  /** `p` and `P` are the same atom; the notations differ only in how they show
    * it.
    */
  private def canonical(name: String): String =
    if name.isEmpty then name else name.head.toUpper.toString + name.tail

/** The goals the Setup screen offers, in both notations.
  *
  * The last is deliberately not a theorem, so a player can meet the negative
  * ending on purpose (§4.10, and the handoff's Setup spec).
  */
object Examples:

  final case class Example(id: String, programmer: String, logician: String)

  val all: List[Example] = List(
    Example("distributivity", "(P, Either[Q, R]) => Either[(P, Q), (P, R)]", "p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)"),
    Example("commutativity", "(A, B) => (B, A)", "a ∧ b → b ∧ a"),
    Example("k", "A => (B => A)", "a → (b → a)"),
    Example("transitivity", "(A => B) => ((B => C) => (A => C))", "(a → b) → ((b → c) → (a → c))"),
    Example("excludedMiddle", "Either[A, A => Nothing]", "a ∨ ¬a")
  )
