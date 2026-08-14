/* shapes.jsx — "a type has a shape" visual language.
 * SVG glyphs per type-former, atom crystals (colour-coded P/Q/R),
 * and colour-aware type text rendering. Exports to window. */

const ATOM_CLASS = { P: 'aP', Q: 'aQ', R: 'aR' };
function atomClass(name) { return ATOM_CLASS[name] || ''; }

// top-level shape key for a type
function shapeOf(type) {
  switch (type.k) {
    case 'fun': return 'fun';
    case 'prod': return 'prod';
    case 'sum': return 'sum';
    case 'unit': return 'unit';
    case 'void': return 'void';
    case 'var': return 'var';
    default: return 'var';
  }
}

// ---- SVG glyph for a type-former -------------------------------------------
// ghost=true → empty "socket" rendering (dashed, unfilled) for holes.
function Glyph({ type, size, ghost }) {
  size = size || 22;
  const k = shapeOf(type);
  const stroke = ghost ? 'var(--accent)' : 'var(--shape-ink)';
  const fill = ghost ? 'none' : 'var(--shape-fill)';
  const sw = 1.6;
  const dash = ghost ? '3 2.6' : 'none';
  const common = { fill, stroke, strokeWidth: sw, strokeDasharray: dash, strokeLinejoin: 'round' };

  let inner = null;
  if (k === 'fun') {
    // portal frame + arrow through it
    inner = (
      <g>
        <rect x="2.5" y="3.5" width="19" height="17" rx="4" {...common} />
        <path d="M7 12 H15.5" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeDasharray={dash} />
        <path d="M13 8.5 L17 12 L13 15.5" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" strokeDasharray={dash} />
      </g>
    );
  } else if (k === 'prod') {
    // two fused cells
    inner = (
      <g>
        <rect x="2.5" y="4" width="19" height="16" rx="4" {...common} />
        <path d="M12 4.6 V19.4" fill="none" stroke={stroke} strokeWidth={sw} strokeDasharray={dash} />
      </g>
    );
  } else if (k === 'sum') {
    // fork — diagonal split (either/or)
    inner = (
      <g>
        <rect x="2.5" y="4" width="19" height="16" rx="4" {...common} />
        <path d="M5 19 L19 5" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeDasharray={dash} />
      </g>
    );
  } else if (k === 'unit') {
    // solid complete token
    inner = ghost
      ? <circle cx="12" cy="12" r="8.5" {...common} />
      : <circle cx="12" cy="12" r="8.5" fill="var(--shape-ink)" stroke="none" />;
  } else if (k === 'void') {
    // empty void with slash
    inner = (
      <g>
        <circle cx="12" cy="12" r="8.5" fill="none" stroke={ghost ? stroke : 'var(--dead)'} strokeWidth={sw} strokeDasharray={dash} />
        <path d="M6.5 17.5 L17.5 6.5" stroke={ghost ? stroke : 'var(--dead)'} strokeWidth={sw} strokeLinecap="round" />
      </g>
    );
  } else { // var — crystal
    const name = type.name || 'A';
    const ac = atomClass(name);
    inner = (
      <g>
        <path d="M12 2.6 L20.5 8 V16 L12 21.4 L3.5 16 V8 Z"
          fill={`var(--atom-${name}-soft, var(--shape-fill))`}
          stroke={`var(--atom-${name}, var(--shape-ink))`} strokeWidth={sw} strokeLinejoin="round"
          strokeDasharray={ghost ? dash : 'none'} className={ac} />
      </g>
    );
  }
  return (
    <svg className="glyph" width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      {inner}
    </svg>
  );
}

// ---- colour-coded atom crystal token (with letter) -------------------------
function AtomCrystal({ name, size }) {
  size = size || 22;
  return (
    <svg className="glyph" width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 2.6 L20.5 8 V16 L12 21.4 L3.5 16 V8 Z"
        fill={`var(--atom-${name}-soft, var(--shape-fill))`}
        stroke={`var(--atom-${name}, var(--shape-ink))`} strokeWidth="1.6" strokeLinejoin="round" />
      <text x="12" y="13.2" textAnchor="middle" dominantBaseline="middle"
        fontFamily="var(--mono)" fontSize="9.5" fontWeight="600"
        fill={`var(--atom-${name}, var(--ink))`}>{name}</text>
    </svg>
  );
}

// ---- colour-aware type text (atoms tinted) ---------------------------------
// Returns an array of React nodes. lang: 'prog' | 'logic'.
function TypeText({ type, lang }) {
  return <span>{renderTy(type, lang, 0)}</span>;
}
let _tk = 0;
function renderTy(t, lang, parentPrec) {
  const key = 'tk' + (_tk++);
  const lower = lang === 'logic';
  if (t.k === 'var') {
    const nm = lower ? t.name.toLowerCase() : t.name;
    return <span key={key} className={atomClass(t.name)}>{nm}</span>;
  }
  if (t.k === 'unit') return <span key={key} className="ty">{lang === 'logic' ? '⊤' : 'Unit'}</span>;
  if (t.k === 'void') return <span key={key} style={{ color: 'var(--dead)' }}>{lang === 'logic' ? '⊥' : 'Nothing'}</span>;

  let prec, parts;
  if (t.k === 'prod') {
    prec = 3;
    parts = lang === 'logic'
      ? [renderTy(t.a, lang, prec), <span key={key+'o'} className="punc"> ∧ </span>, renderTy(t.b, lang, prec)]
      : [<span key={key+'l'} className="punc">(</span>, renderTy(t.a, lang, 0), <span key={key+'c'} className="punc">, </span>, renderTy(t.b, lang, 0), <span key={key+'r'} className="punc">)</span>];
  } else if (t.k === 'sum') {
    prec = 3;
    parts = lang === 'logic'
      ? [renderTy(t.a, lang, prec), <span key={key+'o'} className="punc"> ∨ </span>, renderTy(t.b, lang, prec)]
      : [<span key={key+'e'} className="ty">Either</span>, <span key={key+'l'} className="punc">[</span>, renderTy(t.a, lang, 0), <span key={key+'c'} className="punc">, </span>, renderTy(t.b, lang, 0), <span key={key+'r'} className="punc">]</span>];
  } else if (t.k === 'fun') {
    prec = 1;
    if (lang === 'logic') {
      parts = [renderTy(t.a, lang, 2), <span key={key+'o'} className="punc"> → </span>, renderTy(t.b, lang, 1)];
    } else {
      const domWrap = (t.a.k === 'fun');
      const dom = domWrap
        ? [<span key={key+'dl'} className="punc">(</span>, renderTy(t.a, lang, 0), <span key={key+'dr'} className="punc">)</span>]
        : [renderTy(t.a, lang, 0)];
      parts = [...dom, <span key={key+'o'} className="punc"> =&gt; </span>, renderTy(t.b, lang, 0)];
    }
  }
  if (lang === 'logic' && prec <= parentPrec) {
    return <span key={key}><span className="punc">(</span>{parts}<span className="punc">)</span></span>;
  }
  return <span key={key}>{parts}</span>;
}

Object.assign(window, { Glyph, AtomCrystal, TypeText, shapeOf, atomClass });
