/* icons.jsx — Lucide glyphs vendored inline (the URJC system's substituted icon
 * set: 24×24 grid, 2px stroke, round caps, outline only, inherits currentColor).
 * Inlined rather than loaded as remote CSS masks so no network is required.
 * No emoji, no Unicode characters used as icons. */

const LUCIDE_PATHS = {
  sun: ['circle:12,12,4', 'M12 2v2', 'M12 20v2', 'm4.93 4.93 1.41 1.41', 'm17.66 17.66 1.41 1.41',
        'M2 12h2', 'M20 12h2', 'm6.34 17.66-1.41 1.41', 'm19.07 4.93-1.41 1.41'],
  moon: ['M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z'],
  'arrow-right': ['M5 12h14', 'm12 5 7 7-7 7'],
  'arrow-left': ['m12 19-7-7 7-7', 'M19 12H5'],
  'circle-alert': ['circle:12,12,10', 'M12 8v4', 'M12 16h.01'],
  'chevron-down': ['m6 9 6 6 6-6'],
  'chevron-right': ['m9 18 6-6-6-6'],
  'git-branch': ['M6 3v12', 'circle:18,6,3', 'circle:6,18,3', 'M18 9a9 9 0 0 1-9 9'],
  check: ['M20 6 9 17l-5-5'],
  x: ['M18 6 6 18', 'm6 6 12 12'],
  'undo-2': ['M9 14 4 9l5-5', 'M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5 5.5 5.5 0 0 1-5.5 5.5H11'],
  'rotate-ccw': ['M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8', 'M3 3v5h5'],
};

function Icon({ name, size, color, label, style }) {
  const parts = LUCIDE_PATHS[name] || [];
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size || 18} height={size || 18}
      viewBox="0 0 24 24" fill="none" stroke={color || 'currentColor'} strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"
      role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : true}
      style={{ display: 'inline-block', flex: '0 0 auto', verticalAlign: 'middle', ...style }}>
      {parts.map((p, i) => p.startsWith('circle:')
        ? (() => { const [cx, cy, r] = p.slice(7).split(','); return <circle key={i} cx={cx} cy={cy} r={r} />; })()
        : <path key={i} d={p} />)}
    </svg>
  );
}

Object.assign(window, { Icon });
