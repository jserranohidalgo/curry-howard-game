/* views.jsx — render a term in two faithful syntaxes:
 *   ProgramView — Scala code with interactive typed holes
 *   ProofView   — Gentzen natural-deduction derivation (same structure)
 * Shared HoleView handles selection + drag-drop targeting. Exports to window. */

let _pk = 0;
const k = () => 'v' + (_pk++);
const kw = (s) => <span key={k()} className="kw">{s}</span>;
const ty = (s) => <span key={k()} className="ty">{s}</span>;
const pn = (s) => <span key={k()} className="punc">{s}</span>;
const vr = (s) => <span key={k()} className="vr">{s}</span>;
const raw = (s) => <span key={k()}>{s}</span>;

// ---- shared hole ----------------------------------------------------------
function HoleView({ h, ctx, proof }) {
  const sel = ctx.selectedHoleId === h.id;
  const dead = ctx.deadIds && ctx.deadIds.has(h.id);
  const cls = (proof ? 'nd-hole' : 'hole') + (sel ? ' sel' : '') + (dead ? ' dead' : '');
  const drop = ctx.dropProps ? ctx.dropProps(h) : {};
  return (
    <span key={k()} className={cls} data-hole={h.id}
      onClick={(e) => { e.stopPropagation(); ctx.onSelect(h.id); }} {...drop}>
      <Glyph type={h.type} size={proof ? 15 : 16} ghost />
      <span className={proof ? '' : 'hole-ty'}>
        <span className="punc">… : </span><TypeText type={h.type} lang={proof ? 'logic' : 'prog'} />
      </span>
    </span>
  );
}

// ============================================================ PROGRAM VIEW
function ProgramView({ term, ctx }) {
  _pk = 0;
  const out = [];
  prog(term, 0, ctx, out);
  return <div className="code">{out}</div>;
}

const pad = (n) => '  '.repeat(n);

function prog(node, ind, ctx, out) {
  switch (node.k) {
    case 'hole': out.push(<HoleView key={k()} h={node} ctx={ctx} />); break;
    case 'unit': out.push(pn('()')); break;
    case 'var': out.push(vr(node.name)); break;
    case 'proj': out.push(vr(node.srcName), pn('._' + node.idx)); break;
    case 'absurd': out.push(vr(node.srcName), raw(' '), kw('match'), raw(' '), pn('{ }')); break;
    case 'inl':
      out.push(ty('Left'), pn('(')); prog(node.arg, ind, ctx, out); out.push(pn(')')); break;
    case 'inr':
      out.push(ty('Right'), pn('(')); prog(node.arg, ind, ctx, out); out.push(pn(')')); break;
    case 'pair':
      out.push(pn('(')); prog(node.fst, ind, ctx, out); out.push(pn(', ')); prog(node.snd, ind, ctx, out); out.push(pn(')')); break;
    case 'app':
      out.push(vr(node.fnName), pn('(')); prog(node.arg, ind, ctx, out); out.push(pn(')')); break;
    case 'lam': {
      out.push(pn('('), vr(node.param.name), pn(': '));
      out.push(<span key={k()} className="ty"><TypeText type={node.param.type} lang="prog" /></span>);
      out.push(pn(') '), kw('=>'), raw('\n' + pad(ind + 1)));
      prog(node.body, ind + 1, ctx, out);
      break;
    }
    case 'let': {
      out.push(kw('val '), vr(node.binding.name), pn(': '));
      out.push(<span key={k()} className="ty"><TypeText type={node.binding.type} lang="prog" /></span>);
      out.push(pn(' = '));
      prog(node.value, ind, ctx, out);
      out.push(raw('\n' + pad(ind)));
      prog(node.body, ind, ctx, out);
      break;
    }
    case 'match': {
      const ll = `Left(${node.lvar.name})`;
      const rl = `Right(${node.rvar.name})`;
      const w = Math.max(ll.length, rl.length);
      out.push(vr(node.scrutName), raw(' '), kw('match'), raw(' '), pn('{'));
      out.push(raw('\n' + pad(ind + 1)), kw('case '), ty('Left'), pn('('), vr(node.lvar.name), pn(')'),
        raw(' '.repeat(w - ll.length) + ' '), kw('=> '));
      prog(node.lbody, ind + 1, ctx, out);
      out.push(raw('\n' + pad(ind + 1)), kw('case '), ty('Right'), pn('('), vr(node.rvar.name), pn(')'),
        raw(' '.repeat(w - rl.length) + ' '), kw('=> '));
      prog(node.rbody, ind + 1, ctx, out);
      out.push(raw('\n' + pad(ind)), pn('}'));
      break;
    }
    default: break;
  }
}

// ============================================================ PROOF VIEW
function ProofView({ term, ctx }) {
  _pk = 0;
  return <div style={{ minWidth: 'max-content', margin: '0 auto' }}>
    <div className="nd">{nd(term, {}, ctx)}</div>
  </div>;
}

function Deriv({ premises, rule, concl }) {
  return (
    <div key={k()} className="nd-stack">
      <div className="nd-prem">{premises.length ? premises : <span style={{ height: 2 }} />}</div>
      <div className="nd-bar"><span className="nd-rule">{rule}</span></div>
      <div className="nd-concl">{concl}</div>
    </div>
  );
}

function assumptionLeaf(name, type) {
  return (
    <div key={k()} className="nd-stack">
      <span className="nd-leaf">
        <span className="nd-disch">[</span><TypeText type={type} lang="logic" /><span className="nd-disch">]</span>
        <sup className="nd-disch" style={{ fontFamily: 'var(--mono)' }}>{name}</sup>
      </span>
    </div>
  );
}

// derivation for a referenced source (let-bound → expand its value; else assumption)
function srcDeriv(name, type, env, ctx) {
  if (env[name]) return nd(env[name], env, ctx);
  return assumptionLeaf(name, type);
}

function conclTy(t) { return <TypeText type={t} lang="logic" />; }

function nd(node, env, ctx) {
  switch (node.k) {
    case 'hole':
      return <div key={k()} className="nd-stack"><HoleView h={node} ctx={ctx} proof /></div>;
    case 'var':
      return srcDeriv(node.name, node.type, env, ctx);
    case 'unit':
      return <Deriv key={k()} premises={[]} rule="⊤I" concl={conclTy(node.type)} />;
    case 'proj':
      return <Deriv key={k()} premises={[srcDeriv(node.srcName, node.srcType, env, ctx)]}
        rule={node.idx === 1 ? '∧E₁' : '∧E₂'} concl={conclTy(node.type)} />;
    case 'app':
      return <Deriv key={k()} premises={[srcDeriv(node.fnName, node.fnType, env, ctx), nd(node.arg, env, ctx)]}
        rule="→E" concl={conclTy(node.type)} />;
    case 'absurd':
      return <Deriv key={k()} premises={[srcDeriv(node.srcName, node.srcType, env, ctx)]}
        rule="⊥E" concl={conclTy(node.type)} />;
    case 'lam':
      return <Deriv key={k()} premises={[nd(node.body, env, ctx)]}
        rule={<span>→I<sup style={{ fontFamily: 'var(--mono)' }}>{node.param.name}</sup></span>} concl={conclTy(node.type)} />;
    case 'pair':
      return <Deriv key={k()} premises={[nd(node.fst, env, ctx), nd(node.snd, env, ctx)]}
        rule="∧I" concl={conclTy(node.type)} />;
    case 'inl':
      return <Deriv key={k()} premises={[nd(node.arg, env, ctx)]} rule="∨I₁" concl={conclTy(node.type)} />;
    case 'inr':
      return <Deriv key={k()} premises={[nd(node.arg, env, ctx)]} rule="∨I₂" concl={conclTy(node.type)} />;
    case 'let':
      return nd(node.body, { ...env, [node.binding.name]: node.value }, ctx);
    case 'match':
      return <Deriv key={k()}
        premises={[srcDeriv(node.scrutName, node.scrutType, env, ctx), nd(node.lbody, env, ctx), nd(node.rbody, env, ctx)]}
        rule={<span>∨E<sup style={{ fontFamily: 'var(--mono)' }}>{node.lvar.name},{node.rvar.name}</sup></span>}
        concl={conclTy(node.type)} />;
    default:
      return <span key={k()} />;
  }
}

Object.assign(window, { ProgramView, ProofView, HoleView });
