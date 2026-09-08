/* Service worker — the offline half of D3.
 *
 * The game needs no network once loaded: no backend, no data fetching, and
 * state lives in the browser. So the whole app is an app shell that can be
 * cached outright.
 *
 * Two rules, and both were paid for:
 *
 * 1. **The shell is network-first**, everything else cache-first. Cache-first
 *    on `main.js` means a rebuilt game is not the game that runs — the page
 *    keeps the bundle this worker cached, and there is no error to see. That
 *    was measured, not guessed: with the worker active, a changed `main.js` on
 *    the server was still not the one the page fetched. Offline is unaffected,
 *    since a failed fetch falls back to the cache.
 * 2. **`CACHE` carries a build stamp**, written by `assembleSite` in build.sbt
 *    from the shell's own bytes. The name used to be bumped by hand, and a
 *    build that forgot left every existing browser on the old cache — the
 *    worker itself is only re-installed when *its* bytes change, which an
 *    unchanged constant guaranteed they would not.
 */

const CACHE = 'curry-howard-shell-d3ca1bd73d48';

/* The files a rebuild changes. Served network-first so a new build is picked up
   the moment it exists; the cache is the offline fallback, not the source. */
const FRESH = /\/(index\.html|js\/[^/]+\.js|styles\/.*\.css)$|\/$/;

const SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './js/main.js',
  './styles/base.css',
  './styles/fonts.css',
  './styles/tokens/colors.css',
  './styles/tokens/typography.css',
  './styles/tokens/spacing.css',
  './styles/tokens/radius.css',
  './styles/tokens/elevation.css',
  './styles/tokens/motion.css',
  './assets/icon-x.svg',
  './assets/favicon.svg',
  './assets/icon-x-disc-192.png',
  './assets/icon-x-disc-512.png',
  './assets/curry.jpeg',
  './assets/howard.jpg',
  './assets/hilbert.jpg',
  './assets/schonfinkel.jpg',
  './assets/gentzen.jpg',
  './assets/church.jpg',
  './assets/icon-morph-correspondence.gif'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((names) => Promise.all(names.filter((n) => n !== CACHE).map((n) => caches.delete(n))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  // A navigation with no network must still land on the shell.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(() => caches.match('./index.html', { ignoreSearch: true }))
    );
    return;
  }

  const url = new URL(request.url);
  const sameOrigin = url.origin === self.location.origin;

  const store = (response) => {
    if (response.ok && sameOrigin) {
      const copy = response.clone();
      caches.open(CACHE).then((cache) => cache.put(request, copy));
    }
    return response;
  };

  // The shell: network first, cache only when the network is not there.
  if (sameOrigin && FRESH.test(url.pathname)) {
    event.respondWith(
      fetch(request)
        .then(store)
        .catch(() => caches.match(request, { ignoreSearch: true }))
    );
    return;
  }

  // Everything else — images, fonts — never changes without changing its name.
  event.respondWith(
    caches.match(request, { ignoreSearch: true }).then((hit) => hit || fetch(request).then(store))
  );
});
