/* panels.jsx — goal banner, scope, move presentations, tree/backtracking views.
 * Exports to window. */

// ---- resource glyph (atom crystal for vars, shape glyph otherwise) --------
function ResourceGlyph({ type, size }) {
  if (type.k === 'var') return <AtomCrystal name={type.name} size={size || 24} />;
  return <Glyph type={type} size={size || 24} />;
}

// ---- goal banner ----------------------------------------------------------
function GoalBanner({ puzzle, lang }) {
  return (
    <div className="goal">
      <div className="goal-top">
        <span className="goal-label">{t(lang === 'logic' ? 'play.goal.logic' : 'play.goal.prog')}</span>
        <span style={{ flex: 1 }} />
        <span className="goal-label" style={{ color: 'var(--accent)' }}>{t('play.yourGoal')}</span>
      </div>
      {lang === 'logic' ? (
        <div className="goal-prop">
          <TypeText type={puzzle.goal} lang="logic" />
        </div>
      ) : (
        <div className="goal-sig">
          <span className="kw">def </span>{puzzle.binder}
          <span className="punc">[</span>
          {puzzle.tyParams.map((p, i) => (
            <span key={p}><span className={'a' + p}>{p}</span>{i < puzzle.tyParams.length - 1 ? <span className="punc">, </span> : null}</span>
          ))}
          <span className="punc">]: </span>
          <TypeText type={puzzle.goal} lang="prog" />
        </div>
      )}
    </div>
  );
}

// ---- scope panel ----------------------------------------------------------
function kindLabel(type, lang) {
  if (type.k === 'var') return t('kind.var');
  return t('kind.' + type.k + '.' + (lang === 'logic' ? 'logic' : 'prog'));
}
function ScopePanel({ scope, lang }) {
  if (!scope || scope.length === 0) {
    return <div className="scope-empty">{t('play.scope.empty1')}<br />{t('play.scope.empty2')}</div>;
  }
  return (
    <div>
      {scope.map((v, i) => (
        <div className="chip" key={v.name + i} style={{ animationDelay: (i * 30) + 'ms' }}>
          <ResourceGlyph type={v.type} size={26} />
          <div style={{ minWidth: 0 }}>
            <div className="chip-name">{v.name}</div>
            <div className="chip-ty"><TypeText type={v.type} lang={lang} /></div>
          </div>
          <span className="chip-meta">{kindLabel(v.type, lang)}</span>
        </div>
      ))}
    </div>
  );
}

// ---- move label fragments -------------------------------------------------
function MoveBadge({ move }) {
  return <span className={'move-badge ' + move.kind}>{move.logic}</span>;
}

// ---- variant A: contextual menu list --------------------------------------
function MovesMenu({ moves, onPick, lang }) {
  if (!moves.length) return <div className="move empty">{t('play.noMove')}</div>;
  return (
    <div>
      {moves.map((m, i) => (
        <button className="move" key={i} onClick={() => onPick(m)} title={m.blurb}>
          <span className={'move-badge ' + m.kind}>{m.rule}</span>
          <span className="move-body">
            <span className="move-title">{moveTitle(m, lang)}<span className="move-kind">{m.kind === 'con' ? 'build' : 'use'}</span></span>
            <span className="move-code">{moveCode(m, lang)}</span>
          </span>
        </button>
      ))}
    </div>
  );
}

// ---- variant B: card tray -------------------------------------------------
function MovesTray({ moves, onPick }) {
  if (!moves.length) return <div className="move empty">{t('play.noMove')}</div>;
  return (
    <div className="tray">
      {moves.map((m, i) => (
        <button className="mcard" key={i} onClick={() => onPick(m)} title={m.blurb}>
          <span className={'move-badge ' + m.kind}>{m.rule}</span>
          <div className="move-title" style={{ marginBottom: 3 }}>{m.title}</div>
          <div className="move-code" style={{ whiteSpace: 'normal' }}>{m.prog}</div>
        </button>
      ))}
    </div>
  );
}

// ---- variant C: draggable tiles -------------------------------------------
function MovesTiles({ moves, onPick, onDragMove }) {
  if (!moves.length) return <div className="move empty">{t('play.noMove')}</div>;
  return (
    <div className="tray">
      {moves.map((m, i) => (
        <div className="tile" key={i} draggable
          onClick={() => onPick(m)}
          onDragStart={(e) => { onDragMove(m); e.dataTransfer.effectAllowed = 'move'; try { e.dataTransfer.setData('text/plain', m.rule); } catch (_) {} }}
          onDragEnd={() => onDragMove(null)}
          title={'Drag onto the selected hole — or click. ' + m.blurb}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className={'move-badge ' + m.kind}>{m.rule}</span>
            <span className="move-title">{m.title}</span>
          </div>
          <div className="move-code" style={{ marginTop: 6 }}>{m.prog}</div>
        </div>
      ))}
    </div>
  );
}

function moveTitle(m, lang) { return lang === 'logic' && m.ltitle ? m.ltitle : m.title; }
function moveCode(m, lang) { return lang === 'logic' && m.lcode ? m.lcode : m.prog; }

// ---- variant D: full rules table -------------------------------------------
// The complete I/E table of IPL (= constructors/destructors of the ADTs).
// Always visible; cells with no applicable move are dimmed. Cells with several
// concrete instances stack and unfold in place.
const RTABLE_ROWS = [
  { key: 'fun',  mk: () => T.fun(T.v('A'), T.v('B')),  sym: { logic: '→', prog: '=>' },      I: { label: '→I', rules: ['⟶.I'] }, E: { label: '→E', rules: ['⟶.E'] } },
  { key: 'prod', mk: () => T.prod(T.v('A'), T.v('B')), sym: { logic: '∧', prog: '( , )' },   I: { label: '∧I', rules: ['∧.I'] }, E: { label: '∧E', rules: ['∧.E₁', '∧.E₂'] } },
  { key: 'sum',  mk: () => T.sum(T.v('A'), T.v('B')),  sym: { logic: '∨', prog: 'Either' },  I: { label: '∨I', rules: ['∨.I₁', '∨.I₂'] }, E: { label: '∨E', rules: ['∨.E'] } },
  { key: 'unit', mk: () => T.unit,                     sym: { logic: '⊤', prog: 'Unit' },    I: { label: '⊤I', rules: ['⊤.I'] }, E: null },
  { key: 'void', mk: () => T.void,                     sym: { logic: '⊥', prog: 'Nothing' }, I: null, E: { label: '⊥E', rules: ['⊥.E'] } },
];

function RTableCell({ ck, spec, moves, onPick, open, setOpen, lang }) {
  if (!spec) return <div className="tcell none">—</div>;
  const ms = moves.filter((m) => spec.rules.includes(m.rule));
  const n = ms.length;
  const isOpen = open === ck;
  return (
    <div className="tcell-wrap">
      <button className={'tcell' + (n ? ' on' : ' off') + (n > 1 ? ' stack' : '')}
        disabled={!n}
        onClick={() => { if (n === 1) onPick(ms[0]); else if (n > 1) setOpen(isOpen ? null : ck); }}
        title={n === 1 ? ms[0].blurb : n > 1 ? t('play.cell.n', { n }) : t('play.cell.no')}>
        <span className="tcell-rule">{spec.label}</span>
        {n > 1 && <span className="tcell-count">{n}</span>}
      </button>
      {isOpen && (
        <div className="tcell-menu">
          {ms.map((m, i) => (
            <button key={i} className="tcell-item" onClick={() => { setOpen(null); onPick(m); }} title={m.blurb}>
              <span style={{ fontSize: 11, fontWeight: 600, display: 'block', marginBottom: 1 }}>{moveTitle(m, lang)}</span>
              <span className="move-code">{moveCode(m, lang)}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function MovesTable({ moves, onPick, lang }) {
  const [open, setOpen] = React.useState(null);
  const axSpec = { label: 'Ax', rules: ['Ax'] };
  return (
    <div>
      {!moves.length && <div className="move empty" style={{ marginBottom: 10 }}>{t('play.noMove')}</div>}
      <div className="rtable">
        <div className="rt-h"></div>
        <div className="rt-h">{t(lang === 'logic' ? 'play.col.con.logic' : 'play.col.con.prog')}</div>
        <div className="rt-h">{t(lang === 'logic' ? 'play.col.des.logic' : 'play.col.des.prog')}</div>
        {RTABLE_ROWS.map((r) => (
          <React.Fragment key={r.key}>
            <div className="rt-row-h"><Glyph type={r.mk()} size={19} /><span>{r.sym[lang === 'logic' ? 'logic' : 'prog']}</span></div>
            <RTableCell ck={r.key + 'I'} spec={r.I} moves={moves} onPick={onPick} open={open} setOpen={setOpen} lang={lang} />
            <RTableCell ck={r.key + 'E'} spec={r.E} moves={moves} onPick={onPick} open={open} setOpen={setOpen} lang={lang} />
          </React.Fragment>
        ))}
        <div className="rt-row-h" title={t('play.hyp.tip')}><span style={{ fontFamily: 'var(--mono)', fontSize: 12 }}>{t('play.hyp')}</span></div>
        <div className="tcell none">—</div>
        <RTableCell ck="ax" spec={axSpec} moves={moves} onPick={onPick} open={open} setOpen={setOpen} lang={lang} />
      </div>
    </div>
  );
}

// ---- breadcrumb (path root → current) -------------------------------------
function pathTo(game, id) {
  const path = []; let cur = id;
  while (cur != null) { path.unshift(cur); cur = game.nodes[cur].parentId; }
  return path;
}
function Breadcrumb({ game, onJump }) {
  const path = pathTo(game, game.currentId);
  return (
    <div className="crumbs">
      {path.map((id, i) => {
        const n = game.nodes[id];
        const isBranch = n.childrenIds.length > 1;
        const label = n.move ? n.move.rule : 'start';
        return (
          <span key={id} style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            {i > 0 && <span className="crumb-sep"><Icon name="chevron-right" size={12} /></span>}
            <button className={'crumb' + (id === game.currentId ? ' on' : '') + (isBranch ? ' branch' : '')}
              onClick={() => onJump(id)} title={isBranch ? 'Choice point — ' + n.childrenIds.length + ' branches explored' : (n.move ? n.move.title : 'Initial state')}>
              {isBranch && <Icon name="git-branch" size={11} />}{label}
            </button>
          </span>
        );
      })}
    </div>
  );
}

// ---- node-graph tree ------------------------------------------------------
function GraphTree({ game, onJump }) {
  const rows = [];
  function walk(id, depth) {
    const n = game.nodes[id];
    const holes = collectHoles(n.term).length;
    rows.push(
      <div key={id} className="grow" style={{ marginLeft: depth * 16 }}>
        {depth > 0 && <span className="grail" />}
        <button className={'gnode' + (id === game.currentId ? ' on' : '') + (n.status === 'win' ? ' win' : '') + (n.status === 'dead' ? ' dead' : '')}
          onClick={() => onJump(id)} style={{ flex: 1 }}
          title={n.status === 'dead' ? 'Dead end' : n.status === 'win' ? 'Solved' : holes + ' holes open'}>
          <span className="gnode-dot" />
          <span>{n.move ? n.move.rule : 'start'}</span>
          <span style={{ marginLeft: 'auto', color: 'var(--muted)', fontSize: 10, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            {n.status === 'win' ? <><Icon name="check" size={11} color="var(--ok)" />solved</>
              : n.status === 'dead' ? <><Icon name="x" size={11} color="var(--dead)" />dead</>
              : holes + 'h'}
          </span>
        </button>
      </div>
    );
    n.childrenIds.forEach((c) => walk(c, depth + 1));
  }
  walk(game.rootId, 0);
  return <div className="gtree">{rows}</div>;
}

Object.assign(window, {
  ResourceGlyph, GoalBanner, ScopePanel,
  MovesMenu, MovesTray, MovesTiles, MovesTable, Breadcrumb, GraphTree, moveTitle, moveCode, kindLabel,
});
