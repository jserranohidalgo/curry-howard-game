/* app-min.jsx — minimal app: HOME → SETUP → PLAY, plus HELP and the RESULT overlay.
 * Moves are presented only as the rules table. No hints, no move evaluation. */

const { useState, useEffect, useMemo, useRef } = React;

function mixHex(a, b, t) {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return '#' + pa.map((v, i) => Math.round(v + (pb[i] - v) * t).toString(16).padStart(2, '0')).join('');
}

// Is every legal continuation of this node already explored and closed?
// Treated as exhausted: dead ends, and nodes whose every (hole, move) pair has a
// child that is itself exhausted. A win short-circuits to false.
function nodeExhausted(game, id, seen) {
  seen = seen || new Set();
  if (seen.has(id)) return true;
  seen.add(id);
  const n = game.nodes[id];
  if (n.status === 'win') return false;
  const holes = collectHoles(n.term);
  let pairs = 0;
  holes.forEach((h) => { pairs += legalMoves(h).length; });
  if (pairs === 0) return true;                       // dead end
  if (n.childrenIds.length < pairs) return false;     // untried options remain
  return n.childrenIds.every((c) => nodeExhausted(game, c, seen));
}

function PlayScreen({ view, setView, locale, setLang, puzzle, onHome }) {
  const [game, setGame] = useState(() => newGame(puzzle));
  const [selId, setSelId] = useState(null);
  const [treeOpen, setTreeOpen] = useState(false);
  const [confirm, setConfirm] = useState(null);

  const cur = game.nodes[game.currentId];
  const term = cur.term;
  const holes = useMemo(() => collectHoles(term), [term]);
  const deadIds = useMemo(() => {
    const s = new Set();
    holes.forEach((h) => { if (legalMoves(h).length === 0) s.add(h.id); });
    return s;
  }, [holes]);

  const selected = holes.find((h) => h.id === selId) || holes[0] || null;
  useEffect(() => {
    if (holes.length && !holes.find((h) => h.id === selId)) setSelId(holes[0].id);
  }, [game.currentId, holes.length]);

  const moves = selected ? legalMoves(selected) : [];
  const lang = view === 'proof' ? 'logic' : 'prog';
  const won = cur.status === 'win';
  const lost = useMemo(() => !won && nodeExhausted(game, game.rootId), [game, won]);

  function pick(move) {
    if (!selected) return;
    setGame(applyMove(game, selected.id, move).game);
    setSelId(null);
  }
  const jump = (id) => { setGame({ ...game, currentId: id }); setSelId(null); };
  const backtrack = () => { if (cur.parentId != null) jump(cur.parentId); };
  const ctx = { selectedHoleId: selected ? selected.id : null, deadIds, onSelect: setSelId };

  const askCancel = () => setConfirm({
    title: t('dlg.cancel.t'), body: t('dlg.cancel.b'), label: t('dlg.cancel.ok'), act: onHome,
  });
  const askRestart = () => setConfirm({
    title: t('dlg.restart.t'), body: t('dlg.restart.b'), label: t('dlg.restart.ok'),
    act: () => { setGame(newGame(puzzle)); setSelId(null); },
  });

  return (
    <>
      <TopBar view={view} setView={setView} locale={locale} setLang={setLang}>
        <div style={{ height: 22, width: 1, background: 'var(--line)' }} />
        <div className="muted" style={{ fontSize: 12.5, display: 'flex', gap: 14 }}>
          <span><b style={{ color: 'var(--ink)', fontWeight: 600 }}>{holes.length}</b> {t(holes.length === 1 ? 'play.holes.one' : 'play.holes.many')}</span>
          <span><b style={{ color: 'var(--ink)', fontWeight: 600 }}>{cur.depth}</b> {t('play.moves')}</span>
        </div>
      </TopBar>

      <div className="stage">
        {/* LEFT — past & next */}
        <div className="col col-left">
          <div className="panel-head" style={{ cursor: 'pointer', userSelect: 'none' }} onClick={() => setTreeOpen(!treeOpen)}>
            <span className="panel-eyebrow">{t('play.path')}</span>
            {!treeOpen && <span className="muted" style={{ fontSize: 11, whiteSpace: 'nowrap' }}>{t(Object.keys(game.nodes).length === 1 ? 'play.nodes.one' : 'play.nodes.many', { n: Object.keys(game.nodes).length, d: cur.depth })}</span>}
            <span style={{ flex: 1 }} />
            <button className="chev"><Icon name={treeOpen ? 'chevron-down' : 'chevron-right'} size={14} /></button>
          </div>
          {treeOpen && <div className="panel-body" style={{ flex: '0 1 34%' }}>
            <GraphTree game={game} onJump={jump} />
          </div>}

          <div className="divider" style={{ margin: 0 }} />
          <div className="panel-head">
            <span className="panel-eyebrow">{t(lang === 'logic' ? 'play.rules.logic' : 'play.rules.prog')}</span>
            <span style={{ flex: 1 }} />
            <span className="muted" style={{ fontSize: 11 }}>{t('play.available', { n: moves.length })}</span>
          </div>
          <div className="panel-body" style={{ flex: '1 1 auto' }}>
            {won
              ? <div className="hint" style={{ padding: '10px 12px', border: '1px solid var(--ok)', background: 'var(--ok-soft)', color: 'var(--ink)', display: 'flex', gap: 8, alignItems: 'center' }}>
                  <Icon name="check" size={15} color="var(--ok)" /><span><b>{t('play.solved')}</b> {t('play.solved.d')}</span>
                </div>
              : <MovesTable key={selected ? selected.id : 'none'} moves={moves} onPick={pick} lang={lang} />}
          </div>

          <div style={{ padding: '12px 16px', borderTop: '1px solid var(--line)', display: 'flex', gap: 8 }}>
            <button className="ghost-btn" style={{ flex: 1, opacity: cur.parentId == null ? .42 : 1 }}
              onClick={backtrack} disabled={cur.parentId == null}><Icon name="undo-2" size={14} />{t('play.backtrack')}</button>
            <button className="ghost-btn" style={{ flex: 1 }} onClick={askRestart}><Icon name="rotate-ccw" size={14} />{t('play.restart')}</button>
            <button className="ghost-btn" onClick={askCancel}>{t('play.cancel')}</button>
          </div>
        </div>

        {/* RIGHT — present */}
        <div className="col col-center">
          <div className="canvas">
            <div className="canvas-inner">
              <GoalBanner puzzle={puzzle} lang={lang} />
              <div className="term-wrap">
                <div className="term-card" style={{ overflowX: 'auto' }}>
                  {view === 'proof' ? <ProofView term={term} ctx={ctx} /> : <ProgramView term={term} ctx={ctx} />}
                </div>
                <div className="hint" style={{ marginTop: 14, textAlign: 'center' }}>
                  {won ? null : selected
                    ? <>{t('play.selected')} <Glyph type={selected.type} size={14} ghost /> <b style={{ color: 'var(--ink)' }}><TypeText type={selected.type} lang={lang} /></b> — {deadIds.has(selected.id) ? t('play.deadHint') : t('play.pick')}</>
                    : t('play.noHoles')}
                </div>
                <div style={{ marginTop: 28 }}>
                  <div className="panel-eyebrow" style={{ marginBottom: 10 }}>{t(lang === 'logic' ? 'play.scope.logic' : 'play.scope.prog')}</div>
                  <div className="scope-row"><ScopePanel scope={selected ? selected.scope : []} lang={lang} /></div>
                </div>
              </div>
            </div>
          </div>

          {selected && deadIds.has(selected.id) && !won && !lost && (
            <div className="toast dead">
              <span style={{ width: 24, height: 24, borderRadius: '50%', background: 'var(--dead)', color: '#fff', display: 'grid', placeItems: 'center' }}><Icon name="x" size={14} color="#fff" /></span>
              <div>
                <div style={{ fontWeight: 600 }}>{t('play.dead')}</div>
                <div className="muted" style={{ fontSize: 11.5 }}>{t('play.dead.d')}</div>
              </div>
              <button className="pill-btn" onClick={backtrack}><Icon name="undo-2" size={13} />{t('play.backtrack')}</button>
            </div>
          )}
        </div>
      </div>

      {(won || lost) && <ResultOverlay kind={won ? 'win' : 'lost'} view={view} depth={cur.depth} onHome={onHome} />}
      {confirm && <ConfirmDialog title={confirm.title} body={confirm.body} confirmLabel={confirm.label}
        onCancel={() => setConfirm(null)}
        onConfirm={() => { const a = confirm.act; setConfirm(null); a(); }} />}
    </>
  );
}

function App() {
  const [screen, setScreen] = useState('home');   // home | setup | help | play
  const [view, setView] = useState('program');
  const [locale, setLocaleState] = useState('en');
  const [puzzle, setPuzzle] = useState(null);

  setLocale(locale);                      // before children render
  const setLang = (l) => { setLocale(l); setLocaleState(l); };
  useEffect(() => { document.documentElement.setAttribute('data-theme', 'light'); }, []);
  useEffect(() => { document.documentElement.lang = locale; }, [locale]);
  const vars = {
    '--accent': '#E90129', '--accent-ink': '#ffffff',
    '--accent-soft': '#fdeaee', '--accent-line': '#f9c3cd',
  };

  const begin = (type) => { setPuzzle(puzzleFromType(type)); setScreen('play'); };

  return (
    <div className="app" style={vars}>
      {screen === 'play' && puzzle ? (
        <PlayScreen view={view} setView={setView} locale={locale} setLang={setLang}
          puzzle={puzzle} onHome={() => setScreen('home')} />
      ) : (
        <>
          <TopBar view={view} setView={setView} locale={locale} setLang={setLang} />
          {screen === 'home' && <HomeScreen view={view} onStart={() => setScreen('setup')} onHelp={() => setScreen('help')} />}
          {screen === 'setup' && <SetupScreen view={view} onBack={() => setScreen('home')} onBegin={begin} />}
          {screen === 'help' && <HelpScreen view={view} onBack={() => setScreen('home')} />}
        </>
      )}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
