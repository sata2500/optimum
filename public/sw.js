const CACHE_NAME = 'optimum-cache-v1';
const PRE_CACHE_ASSETS = [
  '/',
  '/index.html',
  '/logo.jpg',
  'https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Outfit:wght@300;400;500;600;700;800&display=swap'
];

// Install: Cache critical static shell assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(PRE_CACHE_ASSETS);
    }).then(() => self.skipWaiting())
  );
});

// Activate: Clean up old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Fetch: Network First, Fallback to Cache
// Dynamically cache other CSS/JS assets as they are requested
self.addEventListener('fetch', event => {
  // Only handle HTTP/HTTPS (skip chrome-extension, etc.)
  if (!event.request.url.startsWith(self.location.origin) && !event.request.url.startsWith('https://fonts.')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(response => {
        // If valid response, clone and cache it
        if (response && response.status === 200 && response.type === 'basic') {
          const responseToCache = response.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(event.request, responseToCache);
          });
        }
        return response;
      })
      .catch(() => {
        // Offline: serve from cache
        return caches.match(event.request).then(cachedResponse => {
          if (cachedResponse) {
            return cachedResponse;
          }
          // If offline and request is document, return cached root/index.html (perfect for SPA)
          if (event.request.mode === 'navigate') {
            return caches.match('/index.html');
          }
        });
      })
  );
});

// Notification Click handler: focus or open the app window
self.addEventListener('notificationclick', event => {
  event.notification.close();

  // Find all open clients (windows) of this origin
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      // If there is already an open window, focus it
      for (const client of clientList) {
        if (client.url && 'focus' in client) {
          // Pass the slot and date if they exist in event.notification.data
          if (event.notification.data) {
            client.postMessage({
              type: 'optimum-notification-clicked',
              slot: event.notification.data.slot,
              date: event.notification.data.date
            });
          }
          return client.focus();
        }
      }
      // If no window is open, open a new one
      if (self.clients.openWindow) {
        let url = '/';
        if (event.notification.data) {
          url = `/?slot=${event.notification.data.slot}&date=${event.notification.data.date}`;
        }
        return self.clients.openWindow(url);
      }
    })
  );
});
