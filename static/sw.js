/* Service worker — the offline half of D3.
 *
 * The game needs no network once loaded: no backend, no data fetching, and
 * state lives in the browser. So the whole app is an app shell that can be
 * cached outright, and the strategy is cache-first with a network fallback.
 *
 * Bump CACHE whenever the shell changes; activate deletes every older cache.
 */

const CACHE = 'curry-howard-shell-v1';

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
  './assets/logo-urjc.svg',
  './assets/logo-urjc-white.svg',
  './assets/logo-etsii.svg',
  './assets/logo-etsii-white.svg'
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

  event.respondWith(
    caches.match(request, { ignoreSearch: true }).then(
      (hit) =>
        hit ||
        fetch(request).then((response) => {
          // Cache same-origin successes so a first visit warms the rest.
          if (response.ok && new URL(request.url).origin === self.location.origin) {
            const copy = response.clone();
            caches.open(CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        })
    )
  );
});
