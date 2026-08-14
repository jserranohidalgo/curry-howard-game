/* parser.jsx — parse a goal typed by the user, in EITHER notation.
 * Accepts:  A => B | A -> B | A → B     implication (right assoc)
 *           (A, B) | A ∧ B | A & B      conjunction
 *           Either[A, B] | A ∨ B | A | B  disjunction
 *           Unit | ⊤    Nothing | ⊥     units
 *           parentheses, identifiers
 * Returns { ok:true, type } or { ok:false, error, pos }. */

function tokenize(src) {
  const ts = [];
  let i = 0;
  const three = { '/\\': 'and', '\\/': 'or' };
  while (i < src.length) {
    const c = src[i];
    if (/\s/.test(c)) { i++; continue; }
    const two = src.slice(i, i + 2);
    if (two === '=>' || two === '->') { ts.push({ t: 'arrow', pos: i }); i += 2; continue; }
    if (three[two]) { ts.push({ t: three[two], pos: i }); i += 2; continue; }
    if (c === '→' || c === '⇒') { ts.push({ t: 'arrow', pos: i }); i++; continue; }
    if (c === '∧' || c === '&' || c === '×') { ts.push({ t: 'and', pos: i }); i++; continue; }
    if (c === '∨' || c === '|' || c === '+') { ts.push({ t: 'or', pos: i }); i++; continue; }
    if (c === '¬' || c === '~' || c === '!') { ts.push({ t: 'not', pos: i }); i++; continue; }
    if (c === '⊤') { ts.push({ t: 'unit', pos: i }); i++; continue; }
    if (c === '⊥') { ts.push({ t: 'void', pos: i }); i++; continue; }
    if (c === '(') { ts.push({ t: '(', pos: i }); i++; continue; }
    if (c === ')') { ts.push({ t: ')', pos: i }); i++; continue; }
    if (c === '[') { ts.push({ t: '[', pos: i }); i++; continue; }
    if (c === ']') { ts.push({ t: ']', pos: i }); i++; continue; }
    if (c === ',') { ts.push({ t: ',', pos: i }); i++; continue; }
    if (/[A-Za-z]/.test(c)) {
      let j = i; while (j < src.length && /[A-Za-z0-9_']/.test(src[j])) j++;
      const w = src.slice(i, j);
      const lw = w.toLowerCase();
      if (lw === 'unit' || lw === 'true' || lw === 'top') ts.push({ t: 'unit', pos: i });
      else if (lw === 'not') ts.push({ t: 'not', pos: i });
      else if (lw === 'nothing' || lw === 'void' || lw === 'false' || lw === 'bottom') ts.push({ t: 'void', pos: i });
      else if (lw === 'either' || lw === 'or') ts.push({ t: 'either', pos: i });
      else if (lw === 'and') ts.push({ t: 'and', pos: i });
      else ts.push({ t: 'id', v: w, pos: i });
      i = j; continue;
    }
    return { error: t('err.char', { c }), pos: i };
  }
  ts.push({ t: 'eof', pos: src.length });
  return { tokens: ts };
}

function parseGoal(src) {
  if (!src || !src.trim()) return { ok: false, error: t('err.empty'), pos: 0 };
  const tk = tokenize(src);
  if (tk.error) return { ok: false, error: tk.error, pos: tk.pos };
  const ts = tk.tokens;
  let p = 0;
  const peek = () => ts[p];
  const eat = (t) => (ts[p].t === t ? ts[p++] : null);
  let err = null;
  const fail = (msg, pos) => { if (!err) err = { error: msg, pos }; return null; };

  function pExpr() { // arrow, right associative
    const l = pOr();
    if (!l) return null;
    if (peek().t === 'arrow') { p++; const r = pExpr(); if (!r) return null; return T.fun(l, r); }
    return l;
  }
  function pOr() {
    let l = pAnd(); if (!l) return null;
    while (peek().t === 'or') { p++; const r = pAnd(); if (!r) return null; l = T.sum(l, r); }
    return l;
  }
  function pAnd() {
    let l = pNot(); if (!l) return null;
    while (peek().t === 'and') { p++; const r = pNot(); if (!r) return null; l = T.prod(l, r); }
    return l;
  }
  // ¬A desugars to A → ⊥, exactly as the programmer notation writes A => Nothing
  function pNot() {
    if (peek().t === 'not') { p++; const a = pNot(); if (!a) return null; return T.fun(a, T.void); }
    return pAtom();
  }
  function pAtom() {
    const tok = peek();
    if (tok.t === 'id') { p++; return T.v(tok.v.length > 1 ? tok.v[0].toUpperCase() + tok.v.slice(1) : tok.v.toUpperCase()); }
    if (tok.t === 'unit') { p++; return T.unit; }
    if (tok.t === 'void') { p++; return T.void; }
    if (tok.t === 'either') {
      p++;
      if (!eat('[')) return fail(t('err.eitherOpen'), peek().pos);
      const a = pExpr(); if (!a) return null;
      if (!eat(',')) return fail(t('err.eitherComma'), peek().pos);
      const b = pExpr(); if (!b) return null;
      if (!eat(']')) return fail(t('err.eitherClose'), peek().pos);
      return T.sum(a, b);
    }
    if (tok.t === '(') {
      p++;
      const a = pExpr(); if (!a) return null;
      if (eat(',')) {
        const b = pExpr(); if (!b) return null;
        if (!eat(')')) return fail(t('err.pairClose'), peek().pos);
        return T.prod(a, b);
      }
      if (!eat(')')) return fail(t('err.parenClose'), peek().pos);
      return a;
    }
    if (tok.t === 'eof') return fail(t('err.eof'), tok.pos);
    return fail(t('err.unexpected', { c: src.slice(tok.pos, tok.pos + 1) }), tok.pos);
  }

  const ty = pExpr();
  if (!ty) return { ok: false, ...(err || { error: t('err.parse'), pos: 0 }) };
  if (peek().t !== 'eof') return { ok: false, error: t('err.trailing'), pos: peek().pos };
  return { ok: true, type: ty };
}

// Build a puzzle object (what newGame expects) from a parsed goal.
function puzzleFromType(type, label) {
  const params = [];
  leafAtoms(type).forEach((a) => { if (!params.includes(a)) params.push(a); });
  return {
    id: 'custom', name: label || 'Your goal', binder: 'solution',
    tyParams: params, goal: type, init: [],
  };
}

const EXAMPLES = [
  { prog: '(P, Either[Q, R]) => Either[(P, Q), (P, R)]', logic: 'p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)', note: 'ex.dist' },
  { prog: '(A, B) => (B, A)', logic: 'a ∧ b → b ∧ a', note: 'ex.comm' },
  { prog: 'A => (B => A)', logic: 'a → (b → a)', note: 'ex.k' },
  { prog: '(A => B) => ((B => C) => (A => C))', logic: '(a → b) → ((b → c) → (a → c))', note: 'ex.trans' },
  { prog: 'Either[A, A => Nothing]', logic: 'a ∨ ¬a', note: 'ex.em' },
];

Object.assign(window, { parseGoal, puzzleFromType, EXAMPLES });
