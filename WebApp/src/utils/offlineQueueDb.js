/**
 * Native IndexedDB staging helper for offline attachment binaries.
 * Prevents localStorage 5MB quota exhaustion by storing large files (PDFs/images)
 * in IndexedDB until online sync uploads them to Supabase Storage.
 */

const DB_NAME = 'applytrack_offline_db';
const DB_VERSION = 1;
const STORE_NAME = 'pending_attachments';

function openDb() {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      return resolve(null);
    }
    const request = window.indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = (e) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'uuid' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => {
      console.warn('IndexedDB open error:', request.error);
      resolve(null);
    };
  });
}

export async function stageOfflineAttachments(uuid, attachments) {
  if (!uuid || !attachments) return false;
  try {
    const db = await openDb();
    if (!db) return false;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      store.put({
        uuid,
        attachments,
        timestamp: Date.now()
      });
      tx.oncomplete = () => resolve(true);
      tx.onerror = () => {
        console.warn('Failed to stage offline attachments:', tx.error);
        resolve(false);
      };
    });
  } catch (err) {
    console.warn('stageOfflineAttachments error:', err);
    return false;
  }
}

export async function getStagedOfflineAttachments(uuid) {
  if (!uuid) return null;
  try {
    const db = await openDb();
    if (!db) return null;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const request = store.get(uuid);
      request.onsuccess = () => resolve(request.result ? request.result.attachments : null);
      request.onerror = () => resolve(null);
    });
  } catch {
    return null;
  }
}

export async function clearStagedOfflineAttachments(uuid) {
  if (!uuid) return false;
  try {
    const db = await openDb();
    if (!db) return false;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      store.delete(uuid);
      tx.oncomplete = () => resolve(true);
      tx.onerror = () => resolve(false);
    });
  } catch {
    return false;
  }
}
