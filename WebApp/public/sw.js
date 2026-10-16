const CACHE_NAME = 'applytrack-cache-v1';
const SHELL_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/app_icon.png',
  '/app_icon.avif'
];

// 1. Install: Pre-cache core shell
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(SHELL_URLS);
    }).then(() => self.skipWaiting())
  );
});

// 2. Activate: Take control immediately
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      );
    }).then(() => self.clients.claim())
  );
});

// 3. Fetch: Handle offline reloads and asset requests
self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);

  // Bypass third-party services (Firebase, Supabase, Google APIs)
  if (
    url.origin !== self.location.origin ||
    url.hostname.includes('googleapis.com') ||
    url.hostname.includes('firebaseio.com') ||
    url.hostname.includes('supabase.co')
  ) {
    return;
  }

  // A. Navigation requests (Reloading /applications, /applications/new, etc.)
  // Network-First: try network; if offline, return cached index.html shell so React can boot
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then((networkRes) => {
          if (networkRes && networkRes.status === 200) {
            const clone = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return networkRes;
        })
        .catch(async () => {
          // Offline fallback to cached SPA shell
          const cached = await caches.match(req);
          if (cached) return cached;
          const shell = await caches.match('/index.html');
          if (shell) return shell;
          return caches.match('/');
        })
    );
    return;
  }

  // B. Static Assets (JS chunks, CSS, images, fonts, Vite modules)
  // Stale-While-Revalidate: Serve from cache immediately, update in background when online
  event.respondWith(
    caches.match(req).then((cachedRes) => {
      const fetchPromise = fetch(req)
        .then((networkRes) => {
          if (networkRes && networkRes.status === 200) {
            const clone = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return networkRes;
        })
        .catch(() => null);

      return cachedRes || fetchPromise;
    })
  );
});
