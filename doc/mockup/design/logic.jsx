/* logic.jsx — the Curry–Howard game engine.
 * Type model, term/hole model, scope, legal-move generation, move application,
 * and a persistent game tree (for backtracking + the search-tree view).
 * Pure data + functions; no React. Exports onto window at the end. */

// ----------------------------------------------------------------------------
// Types
// ----------------------------------------------------------------------------
const T = {
  v: (name) => ({ k: 'var', name }),
  unit: { k: 'unit' },
  void: { k: 'void' },
  prod: (a, b) => ({ k: 'prod', a, b }),
  sum: (a, b) => ({ k: 'sum', a, b }),
  fun: (a, b) => ({ k: 'fun', a, b }),
};

function typeEq(a, b) {
  if (a.k !== b.k) return false;
  switch (a.k) {
    case 'var': return a.name === b.name;
    case 'unit': case 'void': return true;
    default: return typeEq(a.a, b.a) && typeEq(a.b, b.b);
  }
}

// Print a type in 'prog' (Scala) or 'logic' (propositional) syntax.
// prec governs parenthesization: atoms 4, prod 3, sum 2, fun 1.
function printType(t, lang, parentPrec) {
  parentPrec = parentPrec || 0;
  const lower = lang === 'logic';
  let prec, s;
  switch (t.k) {
    case 'var': return lower ? t.name.toLowerCase() : t.name;
    case 'unit': return lang === 'logic' ? '⊤' : 'Unit';
    case 'void': return lang === 'logic' ? '⊥' : 'Nothing';
    case 'prod': {
      prec = 3;
      if (lang === 'logic') s = `${printType(t.a, lang, prec)} ∧ ${printType(t.b, lang, prec)}`;
      else s = `(${printType(t.a, lang, 0)}, ${printType(t.b, lang, 0)})`;
      break;
    }
    case 'sum': {
      prec = 3;
      if (lang === 'logic') s = `${printType(t.a, lang, prec)} ∨ ${printType(t.b, lang, prec)}`;
      else s = `Either[${printType(t.a, lang, 0)}, ${printType(t.b, lang, 0)}]`;
      break;
    }
    case 'fun': {
      prec = 1;
      if (lang === 'logic') {
        s = `${printType(t.a, lang, 2)} → ${printType(t.b, lang, 1)}`;
      } else {
        // Scala: a tuple/function domain needs an extra wrap.
        let dom = printType(t.a, lang, 0);
        if (t.a.k === 'fun') dom = `(${dom})`;
        s = `${dom} => ${printType(t.b, lang, 0)}`;
      }
      break;
    }
  }
  // For prog, tuples/Either already bracket themselves; only logic uses prec wraps.
  if (lang === 'logic' && prec <= parentPrec) return `(${s})`;
  return s;
}

// short id for atoms inside a type (for auto-naming bound vars)
function leafAtoms(t, acc) {
  acc = acc || [];
  if (t.k === 'var') acc.push(t.name);
  else if (t.a) { leafAtoms(t.a, acc); leafAtoms(t.b, acc); }
  return acc;
}

// ----------------------------------------------------------------------------
// Naming — pick a fresh variable name given a type and the names already used.
// Mirrors the spec's playthrough naming where possible (x for the tuple,
// q/r for Either[Q,R] branches, qr for the bound disjunction, z for Nothing).
// ----------------------------------------------------------------------------
function freshName(type, used) {
  let base;
  if (type.k === 'var') base = type.name.toLowerCase();
  else if (type.k === 'void') base = 'z';
  else if (type.k === 'unit') base = 'u';
  else if (type.k === 'fun') base = 'f';
  else {
    const atoms = leafAtoms(type).map((a) => a.toLowerCase());
    base = atoms.length >= 1 && atoms.length <= 3 ? atoms.join('') : 'x';
  }
  if (!used.has(base)) return base;
  let i = 2;
  while (used.has(base + i)) i++;
  return base + i;
}

// ----------------------------------------------------------------------------
// Terms / holes
// ----------------------------------------------------------------------------
let _uid = 0;
function nid() { return 'n' + (++_uid); }

function hole(type, scope) { return { k: 'hole', id: nid(), type, scope: scope || [] }; }

function usedNames(scope) { const s = new Set(); scope.forEach((v) => s.add(v.name)); return s; }

// Recursively collect every open hole, each annotated with its scope.
function collectHoles(node, acc) {
  acc = acc || [];
  if (!node) return acc;
  if (node.k === 'hole') { acc.push(node); return acc; }
  switch (node.k) {
    case 'lam': collectHoles(node.body, acc); break;
    case 'pair': collectHoles(node.fst, acc); collectHoles(node.snd, acc); break;
    case 'inl': case 'inr': collectHoles(node.arg, acc); break;
    case 'app': collectHoles(node.arg, acc); break;
    case 'let': collectHoles(node.value, acc); collectHoles(node.body, acc); break;
    case 'match': collectHoles(node.lbody, acc); collectHoles(node.rbody, acc); break;
    default: break; // var, unit, proj, absurd — leaves
  }
  return acc;
}

function findHole(node, id) {
  return collectHoles(node).find((h) => h.id === id) || null;
}

// Replace the hole with the given id by `repl`, returning a new tree.
function replaceHole(node, id, repl) {
  if (!node) return node;
  if (node.k === 'hole') return node.id === id ? repl : node;
  switch (node.k) {
    case 'lam': return { ...node, body: replaceHole(node.body, id, repl) };
    case 'pair': return { ...node, fst: replaceHole(node.fst, id, repl), snd: replaceHole(node.snd, id, repl) };
    case 'inl': case 'inr': return { ...node, arg: replaceHole(node.arg, id, repl) };
    case 'app': return { ...node, arg: replaceHole(node.arg, id, repl) };
    case 'let': return { ...node, value: replaceHole(node.value, id, repl), body: replaceHole(node.body, id, repl) };
    case 'match': return { ...node, lbody: replaceHole(node.lbody, id, repl), rbody: replaceHole(node.rbody, id, repl) };
    default: return node;
  }
}

// ----------------------------------------------------------------------------
// Legal moves for a given hole
// A move = { rule, kind:'con'|'des', via (scope var name|null),
//            prog, logic (short syntax labels), title, blurb, opens, build() }
// ----------------------------------------------------------------------------
function legalMoves(h) {
  const moves = [];
  const sc = h.scope;
  const used = usedNames(sc);
  const tt = h.type;

  // ---- Constructors (build the hole's shape) ----
  if (tt.k === 'fun') {
    const pname = freshName(tt.a, used);
    moves.push({
      rule: '⟶.I', kind: 'con', via: null, builds: tt,
      prog: `(${pname}: …) => …`, logic: '→I',
      title: t('mv.funI'), blurb: t('mv.funI.b', { p: pname, t: printType(tt.a, 'prog') }),
      ltitle: t('mv.funI.l'), lcode: t('mv.funI.lc', { p: pname, t: printType(tt.a, 'logic') }),
      opens: ['result'],
      build: () => {
        const param = { name: pname, type: tt.a };
        return { k: 'lam', id: nid(), param, type: tt, body: hole(tt.b, [...sc, param]) };
      },
    });
  } else if (tt.k === 'prod') {
    moves.push({
      rule: '∧.I', kind: 'con', via: null, builds: tt,
      prog: '( … , … )', logic: '∧I',
      title: t('mv.prodI'), blurb: t('mv.prodI.b'), opens: ['left', 'right'],
      ltitle: t('mv.prodI.l'), lcode: t('mv.prodI.lc'),
      build: () => ({ k: 'pair', id: nid(), type: tt, fst: hole(tt.a, sc), snd: hole(tt.b, sc) }),
    });
  } else if (tt.k === 'sum') {
    moves.push({
      rule: '∨.I₁', kind: 'con', via: null, builds: tt,
      prog: 'Left( … )', logic: '∨I₁',
      title: t('mv.sumI1'), blurb: t('mv.sumI1.b', { t: printType(tt.a, 'prog') }), opens: ['arg'],
      ltitle: t('mv.sumI1.l'), lcode: printType(tt.a, 'logic'),
      build: () => ({ k: 'inl', id: nid(), type: tt, arg: hole(tt.a, sc) }),
    });
    moves.push({
      rule: '∨.I₂', kind: 'con', via: null, builds: tt,
      prog: 'Right( … )', logic: '∨I₂',
      title: t('mv.sumI2'), blurb: t('mv.sumI2.b', { t: printType(tt.b, 'prog') }), opens: ['arg'],
      ltitle: t('mv.sumI2.l'), lcode: printType(tt.b, 'logic'),
      build: () => ({ k: 'inr', id: nid(), type: tt, arg: hole(tt.b, sc) }),
    });
  } else if (tt.k === 'unit') {
    moves.push({
      rule: '⊤.I', kind: 'con', via: null, builds: tt,
      prog: '( )', logic: '⊤I',
      title: t('mv.unitI'), blurb: t('mv.unitI.b'), opens: [],
      ltitle: t('mv.unitI.l'), lcode: '⊤',
      build: () => ({ k: 'unit', id: nid(), type: tt }),
    });
  }
  // var & void: no constructor.

  // ---- Destructors (consume a variable in scope) ----
  sc.forEach((v) => {
    // Axiom — close directly with a matching variable.
    if (typeEq(v.type, tt)) {
      moves.push({
        rule: 'Ax', kind: 'des', via: v.name, builds: tt,
        prog: v.name, logic: 'Ax',
        title: t('mv.ax', { v: v.name }), blurb: t('mv.ax.b', { v: v.name }), opens: [],
        ltitle: t('mv.ax.l', { v: v.name }), lcode: `${v.name} : ${printType(tt, 'logic')}`,
        build: () => ({ k: 'var', id: nid(), name: v.name, type: tt }),
      });
    }
    if (v.type.k === 'prod') {
      // backward projections (only if the component type matches the hole)
      if (typeEq(v.type.a, tt)) moves.push(projBack(v, 1, tt));
      if (typeEq(v.type.b, tt)) moves.push(projBack(v, 2, tt));
      // forward projections (bind a component as a new resource)
      moves.push(projFwd(v, 1, h, used));
      moves.push(projFwd(v, 2, h, used));
    }
    if (v.type.k === 'fun') {
      // backward application (fill the hole if it is the codomain)
      if (typeEq(v.type.b, tt)) {
        moves.push({
          rule: '⟶.E', kind: 'des', via: v.name, builds: tt,
          prog: `${v.name}( … )`, logic: '→E',
          title: t('mv.appB', { v: v.name }), blurb: t('mv.appB.b', { v: v.name, t: printType(v.type.a, 'prog') }), opens: ['arg'],
        ltitle: t('mv.appB.l', { v: v.name }), lcode: t('mv.appB.lc', { t: printType(v.type.a, 'logic') }),
          build: () => ({ k: 'app', id: nid(), fnName: v.name, fnType: v.type, type: tt, arg: hole(v.type.a, sc) }),
        });
      }
      // forward application (bind the result)
      const bn = freshName(v.type.b, used);
      moves.push({
        rule: '⟶.E', kind: 'des', via: v.name, builds: null, forward: true,
        prog: `val ${bn} = ${v.name}( … )`, logic: '→E',
        title: t('mv.appF', { v: v.name }), blurb: t('mv.appF.b', { v: v.name, n: bn, t: printType(v.type.b, 'prog') }), opens: ['arg'],
        ltitle: t('mv.appF.l', { v: v.name, t: printType(v.type.b, 'logic') }), lcode: t('mv.appB.lc', { t: printType(v.type.a, 'logic') }),
        build: () => {
          const binding = { name: bn, type: v.type.b };
          return {
            k: 'let', id: nid(), binding, type: tt,
            value: { k: 'app', id: nid(), fnName: v.name, fnType: v.type, type: v.type.b, arg: hole(v.type.a, sc) },
            body: hole(tt, [...sc, binding]),
          };
        },
      });
    }
    if (v.type.k === 'sum') {
      // case analysis — fills any-typed hole, opens a branch per side.
      const ln = freshName(v.type.a, used);
      const rn = freshName(v.type.b, new Set([...used, ln]));
      moves.push({
        rule: '∨.E', kind: 'des', via: v.name, builds: tt,
        prog: `${v.name} match { Left | Right }`, logic: '∨E',
        title: t('mv.sumE', { v: v.name }), blurb: t('mv.sumE.b', { v: v.name, l: ln, r: rn }), opens: ['Left', 'Right'],
        ltitle: t('mv.sumE.l', { v: v.name }), lcode: t('mv.sumE.lc', { a: printType(v.type.a, 'logic'), b: printType(v.type.b, 'logic') }),
        build: () => {
          const lvar = { name: ln, type: v.type.a };
          const rvar = { name: rn, type: v.type.b };
          return {
            k: 'match', id: nid(), scrutName: v.name, scrutType: v.type, type: tt,
            lvar, lbody: hole(tt, [...sc, lvar]),
            rvar, rbody: hole(tt, [...sc, rvar]),
          };
        },
      });
    }
    if (v.type.k === 'void') {
      moves.push({
        rule: '⊥.E', kind: 'des', via: v.name, builds: tt,
        prog: `${v.name} match { }`, logic: '⊥E',
        title: t('mv.voidE', { v: v.name }), blurb: t('mv.voidE.b', { v: v.name }), opens: [],
        ltitle: t('mv.voidE.l', { v: v.name }), lcode: t('mv.voidE.lc'),
        build: () => ({ k: 'absurd', id: nid(), srcName: v.name, srcType: v.type, type: tt }),
      });
    }
  });

  return moves;
}

function projBack(v, idx, tt) {
  return {
    rule: idx === 1 ? '∧.E₁' : '∧.E₂', kind: 'des', via: v.name, builds: tt,
    prog: `${v.name}._${idx}`, logic: idx === 1 ? '∧E₁' : '∧E₂',
    title: t('mv.projB', { v: v.name, i: idx }), blurb: t('mv.projB.b', { ord: t('ord.' + idx) }), opens: [],
    ltitle: t('mv.projB.l', { v: v.name, side: t('side.' + idx) }), lcode: printType(tt, 'logic'),
    build: () => ({ k: 'proj', id: nid(), srcName: v.name, srcType: v.type, idx, type: tt }),
  };
}
function projFwd(v, idx, h, used) {
  const comp = idx === 1 ? v.type.a : v.type.b;
  const nm = freshName(comp, used);
  const tt = h.type;
  return {
    rule: idx === 1 ? '∧.E₁' : '∧.E₂', kind: 'des', via: v.name, builds: null, forward: true,
    prog: `val ${nm} = ${v.name}._${idx}`, logic: idx === 1 ? '∧E₁' : '∧E₂',
    title: t('mv.projF', { v: v.name, i: idx }), blurb: t('mv.projF.b', { ord: t('ord.' + idx), n: nm, t: printType(comp, 'prog') }), opens: ['continue'],
    ltitle: t('mv.projF.l', { v: v.name, side: t('side.' + idx) }), lcode: printType(comp, 'logic'),
    build: () => {
      const binding = { name: nm, type: comp };
      return {
        k: 'let', id: nid(), binding, type: tt,
        value: { k: 'proj', id: nid(), srcName: v.name, srcType: v.type, idx, type: comp },
        body: hole(tt, [...h.scope, binding]),
      };
    },
  };
}

// ----------------------------------------------------------------------------
// State helpers
// ----------------------------------------------------------------------------
function termStatus(term) {
  const holes = collectHoles(term);
  if (holes.length === 0) return 'win';
  // dead if any open hole has no legal move at all
  for (const hh of holes) if (legalMoves(hh).length === 0) return 'dead';
  return 'open';
}

// ----------------------------------------------------------------------------
// Puzzles
// ----------------------------------------------------------------------------
const PUZZLES = {
  distributivity: {
    id: 'distributivity',
    name: 'Distributivity',
    binder: 'program',
    tyParams: ['P', 'Q', 'R'],
    goal: T.fun(T.prod(T.v('P'), T.sum(T.v('Q'), T.v('R'))),
                T.sum(T.prod(T.v('P'), T.v('Q')), T.prod(T.v('P'), T.v('R')))),
    propLatex: 'p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)',
    init: [],
  },
};

function makeRoot(puzzle) {
  _uid = 0;
  const root = hole(puzzle.goal, puzzle.init.slice());
  return root;
}

// ----------------------------------------------------------------------------
// Game tree (exploration / backtracking)
// ----------------------------------------------------------------------------
function newGame(puzzle) {
  const rootTerm = makeRoot(puzzle);
  const rootId = 'g0';
  let counter = 0;
  const nodes = {
    [rootId]: { id: rootId, parentId: null, term: rootTerm, move: null, actedHoleId: null,
                status: termStatus(rootTerm), childrenIds: [], depth: 0 },
  };
  return { puzzle, nodes, rootId, currentId: rootId, _counter: () => ++counter };
}

// Apply a move at a hole from the current node; returns {game, newId}.
function applyMove(game, holeId, move) {
  const cur = game.nodes[game.currentId];
  const repl = move.build();
  const newTerm = replaceHole(cur.term, holeId, repl);
  const newId = 'g' + game._counter();
  const node = {
    id: newId, parentId: cur.id, term: newTerm,
    move: { rule: move.rule, kind: move.kind, prog: move.prog, logic: move.logic, via: move.via, title: move.title },
    actedHoleId: holeId, status: termStatus(newTerm),
    childrenIds: [], depth: cur.depth + 1,
  };
  const nodes = { ...game.nodes, [newId]: node,
    [cur.id]: { ...cur, childrenIds: [...cur.childrenIds, newId] } };
  return { game: { ...game, nodes, currentId: newId }, newId };
}

Object.assign(window, {
  T, typeEq, printType, leafAtoms, freshName,
  hole, collectHoles, findHole, replaceHole, legalMoves, termStatus,
  PUZZLES, makeRoot, newGame, applyMove,
});
