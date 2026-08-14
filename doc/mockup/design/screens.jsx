/* screens.jsx — HOME, SETUP, HELP and the RESULT overlay + confirm dialog. */

// ---------------------------------------------------------------- shared bits
function ViewSwitch({ view, onChange }) {
  return (
    <div className="seg accent">
      <button className={view === 'program' ? 'on' : ''} onClick={() => onChange('program')}>{t('view.prog')}</button>
      <button className={view === 'proof' ? 'on' : ''} onClick={() => onChange('proof')}>{t('view.logic')}</button>
    </div>
  );
}

function TopBar({ view, setView, locale, setLang, children }) {
  return (
    <div className="topbar">
      <div className="brand">
        <img className="brand-lockup" src="urjc/assets/logo-etsii.svg" alt="Universidad Rey Juan Carlos — Escuela Técnica Superior de Ingeniería Informática" />
      </div>
      {children}
      <div className="topbar-spacer" />
      <ViewSwitch view={view} onChange={setView} />
      <div className="seg lang">
        <button className={locale === 'en' ? 'on' : ''} onClick={() => setLang('en')}>EN</button>
        <button className={locale === 'es' ? 'on' : ''} onClick={() => setLang('es')}>ES</button>
      </div>
    </div>
  );
}

function BrandMark({ size, dark }) {
  const s = size || 30;
  return <img src={dark ? 'urjc/assets/logo-urjc-white.svg' : 'urjc/assets/logo-urjc.svg'} width={s} height={s} alt="Universidad Rey Juan Carlos" style={{ display: 'block' }} />;
}

// ---------------------------------------------------------------- HOME
// The systems on offer. Each pairs a logic with the language its proofs are
// programs in (Curry–Howard). Only IPL ≡ STLC+ADTs is playable for now; the
// rest are listed on a par so the roadmap is legible.
const SYSTEMS = [
  { id: 'sys.ipl', active: true, glyphs: () => <>
      <Glyph type={T.fun(T.v('A'), T.v('B'))} size={22} />
      <Glyph type={T.prod(T.v('A'), T.v('B'))} size={22} />
      <Glyph type={T.sum(T.v('A'), T.v('B'))} size={22} /></> },
  { id: 'sys.cpl', active: false, glyphs: () => <>
      <Glyph type={T.fun(T.v('A'), T.v('B'))} size={22} />
      <Glyph type={T.void} size={22} /></> },
  { id: 'sys.foil', active: false, glyphs: () => <span className="start-sym">∀∃</span> },
  { id: 'sys.focl', active: false, glyphs: () => <span className="start-sym">∀¬</span> },
  { id: 'sys.mll', active: false, glyphs: () => <span className="start-sym">⊗⊸</span> },
];

function HomeScreen({ view, onStart, onHelp }) {
  const prog = view === 'program';
  return (
    <div className="screen">
      <div className="sheet sheet-narrow">
        <div className="hero-mark"><BrandMark size={54} /></div>
        <h1 className="hero-title">{t('home.title')}</h1>
        <p className="hero-tagline">{t('home.tagline')}</p>
        <p className="hero-lede">{t(prog ? 'home.lede.prog' : 'home.lede.logic')}</p>

        <div className="start-list">
          <div className="panel-eyebrow" style={{ marginBottom: 10 }}>{t('home.choose')}</div>
          {SYSTEMS.map((sys) => (
            <button key={sys.id} className={'start-btn' + (sys.active ? '' : ' soon')}
              disabled={!sys.active} onClick={sys.active ? onStart : undefined}>
              <span className="start-glyphs">{sys.glyphs()}</span>
              <span className="start-body">
                <span className="start-title">{t(sys.id)}</span>
                <span className="start-sub">{t(sys.id + '.lang')}</span>
              </span>
              {sys.active
                ? <span className="start-go"><Icon name="arrow-right" size={18} /></span>
                : <span className="start-tag">{t('home.soon.tag')}</span>}
            </button>
          ))}
        </div>

        <div className="home-foot">
          <button className="ghost-btn" onClick={onHelp}>{t('home.help')}</button>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------- SETUP
function SetupScreen({ view, onBack, onBegin }) {
  const [src, setSrc] = React.useState('');
  const prog = view === 'program';
  const res = React.useMemo(() => parseGoal(src), [src]);
  const touched = src.trim().length > 0;
  const ok = res.ok;

  React.useEffect(() => {
    const h = (e) => { if (e.key === 'Enter' && ok) onBegin(res.type); };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [ok, res.type]);

  return (
    <div className="screen">
      <div className="sheet sheet-narrow">
        <button className="ghost-btn back" onClick={onBack}><Icon name="arrow-left" size={14} />{t('nav.back')}</button>
        <h2 className="setup-title">{t(prog ? 'setup.title.prog' : 'setup.title.logic')}</h2>
        <p className="setup-lede muted">{t(prog ? 'setup.lede.prog' : 'setup.lede.logic')}</p>

        <div className={'goal-input' + (touched && !ok ? ' bad' : '') + (ok ? ' good' : '')}>
          <input autoFocus value={src} onChange={(e) => setSrc(e.target.value)} spellCheck="false"
            placeholder={prog ? '(P, Either[Q, R]) => Either[(P, Q), (P, R)]' : 'p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)'} />
          {ok && <span className="goal-shape"><Glyph type={res.type} size={22} /></span>}
        </div>

        <div className="parse-out">
          {!touched && <div className="syntax-hint">
            <span><span className="kbd">=&gt;</span> <span className="kbd">→</span> {t('setup.hint.impl')}</span>
            <span><span className="kbd">(A, B)</span> <span className="kbd">∧</span> {t('setup.hint.conj')}</span>
            <span><span className="kbd">Either[A, B]</span> <span className="kbd">∨</span> {t('setup.hint.disj')}</span>
            <span><span className="kbd">¬A</span> <span className="kbd">A =&gt; Nothing</span> {t('setup.hint.neg')}</span>
            <span><span className="kbd">Unit</span> <span className="kbd">Nothing</span></span>
          </div>}
          {touched && !ok && <div className="parse-err">
            <Icon name="circle-alert" size={16} />{res.error}
            {typeof res.pos === 'number' && <span className="muted"> ({t('setup.at', { n: res.pos + 1 })})</span>}
          </div>}
          {ok && <div className="parse-ok">
            <div className="parse-row">
              <span className="parse-tag">{t('view.prog')}</span>
              <span className="code" style={{ fontSize: 13.5 }}><TypeText type={res.type} lang="prog" /></span>
            </div>
            <div className="parse-row">
              <span className="parse-tag">{t('view.logic')}</span>
              <span style={{ fontFamily: 'var(--serif)', fontSize: 16 }}><TypeText type={res.type} lang="logic" /></span>
            </div>
          </div>}
        </div>

        <button className="primary-btn" disabled={!ok} onClick={() => ok && onBegin(res.type)}>
          {t('setup.begin')}
        </button>

        <div className="examples">
          <div className="panel-eyebrow" style={{ marginBottom: 9 }}>{t('setup.examples')}</div>
          {EXAMPLES.map((ex, i) => (
            <button className="ex-btn" key={i} onClick={() => setSrc(prog ? ex.prog : ex.logic)}>
              <span className="ex-code">{prog ? ex.prog : ex.logic}</span>
              <span className="ex-note">{t(ex.note)}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------- HELP
const HELP_STEPS = [
  { n: 1, title: 'help.s1.title', prog: 'help.s1.prog', logic: 'help.s1.logic',
    art: (lang) => <span className={lang === 'logic' ? 'art-prop' : 'art-goal'}><TypeText type={T.fun(T.prod(T.v('P'), T.v('Q')), T.v('P'))} lang={lang} /></span> },
  { n: 2, title: 'help.s2.title', prog: 'help.s2.prog', logic: 'help.s2.logic',
    art: (lang) => <span className="hole sel" style={{ pointerEvents: 'none' }}><Glyph type={T.v('P')} size={16} ghost /><span className="hole-ty"><span className="punc">… : </span><TypeText type={T.v('P')} lang={lang} /></span></span> },
  { n: 3, title: 'help.s3.title', prog: 'help.s3.prog', logic: 'help.s3.logic',
    art: (lang) => <span className="chip" style={{ margin: 0, minWidth: 190 }}><AtomCrystal name="P" size={24} /><span><span className="chip-name">pq</span><span className="chip-ty" style={{ display: 'block' }}><TypeText type={T.prod(T.v('P'), T.v('Q'))} lang={lang} /></span></span></span> },
  { n: 4, title: 'help.s4.title', prog: 'help.s4.prog', logic: 'help.s4.logic',
    art: (lang) => <span style={{ display: 'flex', gap: 6 }}>
      <span className="tcell on" style={{ width: 104, pointerEvents: 'none' }}><span className="tcell-rule">∧I</span><span className="tcell-title">{t('help.applies')}</span></span>
      <span className="tcell off" style={{ width: 104, pointerEvents: 'none' }}><span className="tcell-rule">∨E</span><span className="tcell-title">{t('help.notHere')}</span></span>
      <span className="rt-row-h" style={{ paddingLeft: 6 }}><Glyph type={T.prod(T.v('A'), T.v('B'))} size={19} /><span>{lang === 'logic' ? '∧' : '( , )'}</span></span>
    </span> },
  { n: 5, title: 'help.s5.title', prog: 'help.s5.prog', logic: 'help.s5.logic',
    art: () => <span style={{ display: 'flex', gap: 5, alignItems: 'center' }}>
      <span className="crumb">⟶.I</span><span className="crumb-sep"><Icon name="chevron-right" size={12} /></span>
      <span className="crumb branch"><Icon name="git-branch" size={12} /> ∨.E</span><span className="crumb-sep"><Icon name="chevron-right" size={12} /></span>
      <span className="crumb on">∧.I</span>
    </span> },
  { n: 6, title: 'help.s6.title', prog: null, logic: null,
    endings: [
      { k: 'win', t: 'help.end.win', d: 'help.end.win.d', icon: 'check' },
      { k: 'dead', t: 'help.end.dead', d: 'help.end.dead.d', icon: 'undo-2' },
      { k: 'lost', t: 'help.end.lost', d: 'help.end.lost.d', icon: 'x' },
    ] },
];

const GLOSSARY = [
  ['type', 'proposition'], ['program', 'proof'], ['hole', 'open goal'],
  ['parameter / binding', 'hypothesis'], ['constructor', 'introduction rule'],
  ['destructor', 'elimination rule'], ['(A, B)', 'A ∧ B'], ['Either[A, B]', 'A ∨ B'],
  ['A => B', 'A → B'], ['Unit', '⊤'], ['Nothing', '⊥'],
];

function HelpScreen({ view, onBack }) {
  const prog = view === 'program';
  return (
    <div className="screen scroll">
      <div className="sheet sheet-wide">
        <button className="ghost-btn back" onClick={onBack}><Icon name="arrow-left" size={14} />{t('nav.back')}</button>
        <h2 className="setup-title">{t('help.title')}</h2>
        <p className="setup-lede muted">{t('help.lede')}</p>

        <div className="steps">
          {HELP_STEPS.map((s) => (
            <div className="step" key={s.n}>
              <div className="step-n">{s.n}</div>
              <div className="step-body">
                <div className="step-title">{t(s.title)}</div>
                {s.prog !== null && <p className="step-text">{t(prog ? s.prog : s.logic)}</p>}
                {s.endings && (
                  <div className="endings">
                    {s.endings.map((e) => (
                      <div className={'ending ' + e.k} key={e.k}>
                        <span className="ending-dot"><Icon name={e.icon} size={13} /></span>
                        <div><b>{t(e.t)}</b><div className="step-text" style={{ margin: '2px 0 0' }}>{t(e.d)}</div></div>
                      </div>
                    ))}
                  </div>
                )}
                {s.art && <div className="step-art">{s.art(prog ? 'prog' : 'logic')}</div>}
              </div>
            </div>
          ))}
        </div>

        <div className="glossary">
          <div className="panel-eyebrow" style={{ marginBottom: 10 }}>{t('help.gloss')}</div>
          <div className="gloss-grid">
            <div className="gloss-h">{t('view.prog')}</div><div className="gloss-h">{t('view.logic')}</div>
            {GLOSSARY.map(([a, b], i) => (
              <React.Fragment key={i}>
                <div className="gloss-c code">{a}</div>
                <div className="gloss-c" style={{ fontFamily: 'var(--serif)' }}>{b}</div>
              </React.Fragment>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------- RESULT overlay
function ResultOverlay({ kind, view, depth, onHome }) {
  const prog = view === 'program';
  const won = kind === 'win';
  return (
    <div className="modal-veil">
      <div className={'result-card ' + kind}>
        <div className="result-icon"><Icon name={won ? 'check' : 'x'} size={24} color="#fff" /></div>
        <div className="result-title">
          {won ? t(prog ? 'res.win.prog' : 'res.win.logic') : t('res.lost')}
        </div>
        <p className="result-text">{won ? t('res.win.d', { n: depth }) : t('res.lost.d')}</p>
        <button className="primary-btn" onClick={onHome}>{t('res.new')}</button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------- confirm dialog
function ConfirmDialog({ title, body, confirmLabel, onConfirm, onCancel }) {
  React.useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onCancel(); };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, []);
  return (
    <div className="modal-veil" onClick={onCancel}>
      <div className="confirm-card" onClick={(e) => e.stopPropagation()}>
        <div className="confirm-title">{title}</div>
        <p className="result-text" style={{ marginBottom: 20 }}>{body}</p>
        <div className="confirm-actions">
          <button className="ghost-btn" onClick={onCancel}>{t('dlg.keep')}</button>
          <button className="danger-btn" onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  BrandMark, TopBar, ViewSwitch,
  HomeScreen, SetupScreen, HelpScreen, ResultOverlay, ConfirmDialog,
});
