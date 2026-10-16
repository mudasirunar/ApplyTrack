import { onAuthStateChanged, signOut } from 'firebase/auth';
import { auth, firestore } from './firebase';
import { collection, onSnapshot, doc, setDoc, deleteDoc, writeBatch, getDocs, getDoc } from 'firebase/firestore';
import { stageOfflineAttachments, getStagedOfflineAttachments, clearStagedOfflineAttachments } from './offlineQueueDb';

const KEYS = {
  USER: 'applytrack_user',
  APPLICATIONS: 'applytrack_applications',
  THEME: 'applytrack_theme',
  DELETED_TEMP: 'applytrack_deleted_temp'
};

let unsubscribeFirestore = null;
let isInitialLoad = true;
let currentUserId = (() => {
  try {
    const raw = typeof localStorage !== 'undefined' ? localStorage.getItem(KEYS.USER) : null;
    return raw ? JSON.parse(raw).uid : null;
  } catch {
    return null;
  }
})();
let isAuthInitialized = false;
let lastLocalWriteTime = 0;
let currentSyncState = 'IDLE';

function getEffectiveUserId() {
  if (currentUserId) return currentUserId;
  try {
    const raw = typeof localStorage !== 'undefined' ? localStorage.getItem(KEYS.USER) : null;
    const uid = raw ? JSON.parse(raw).uid : null;
    if (uid) currentUserId = uid;
    return uid;
  } catch {
    return null;
  }
}

function markLocalWrite() {
  lastLocalWriteTime = Date.now();
}

/**
 * Generates a standard RFC4122 v4 UUID (identical to java.util.UUID.randomUUID() on Android)
 */
export function generateUuid() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// Helper to trigger sync status custom events
let syncStateResetTimeout = null;
function triggerSyncState(state, message = '') {
  currentSyncState = state;
  window.dispatchEvent(new CustomEvent('applytrack_sync_state', {
    detail: { state, message }
  }));

  if (syncStateResetTimeout) clearTimeout(syncStateResetTimeout);
  if (state === 'SUCCESS' || state === 'ERROR') {
    syncStateResetTimeout = setTimeout(() => {
      currentSyncState = 'IDLE';
    }, 2500);
  }
}

// Resolve remote attachment to absolute public Supabase URLs
function resolveRemoteAttachment(att, userId, type) {
  if (!att || !att.fileName) return null;
  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
  return {
    ...att,
    url: `${supabaseUrl}/storage/v1/object/public/ApplyTrack/users/${userId}/${type}/${att.fileName}`
  };
}

// Supabase REST operations
function dataUrlToBlob(dataUrl) {
  try {
    const parts = dataUrl.split(',');
    if (parts.length < 2) return null;
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
    const bstr = atob(parts[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new Blob([u8arr], { type: mime });
  } catch {
    return null;
  }
}

async function uploadFileToSupabase(userId, type, fileName, dataUrl) {
  if (!dataUrl) return true;

  try {
    let blob = dataUrlToBlob(dataUrl);
    if (!blob) {
      const response = await fetch(dataUrl);
      blob = await response.blob();
    }
    
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const uploadUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack/users/${userId}/${type}/${fileName}`;

    const uploadResponse = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${anonKey}`,
        'apikey': anonKey,
        'Content-Type': blob.type || 'application/octet-stream',
        'x-upsert': 'true'
      },
      body: blob
    });

    if (!uploadResponse.ok && uploadResponse.status !== 409) {
      throw new Error(`Upload to Supabase failed (${uploadResponse.status}): ${uploadResponse.statusText}`);
    }

    return true;
  } catch (e) {
    console.error('Supabase upload failed:', e);
    throw e;
  }
}

async function deleteFileFromSupabase(userId, type, fileName) {
  try {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const fullPath = `users/${userId}/${type}/${fileName}`;

    // Standard Supabase Storage REST endpoint for file deletion (with JSON prefixes payload)
    const deleteUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack`;
    console.log('[Supabase Delete] Requesting deletion of:', fullPath);
    const deleteResponse = await fetch(deleteUrl, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${anonKey}`,
        'apikey': anonKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ prefixes: [fullPath] })
    });

    const result = await deleteResponse.json();
    console.log('[Supabase Delete] Response:', deleteResponse.status, result);
    return deleteResponse.ok;
  } catch (e) {
    console.error('Supabase delete failed:', e);
    return false;
  }
}

async function deleteApplicationAttachments(userId, app) {
  if (!app || !userId) return;
  const deleteTasks = [];
  if (app.resume && app.resume.fileName) {
    deleteTasks.push(deleteFileFromSupabase(userId, 'resumes', app.resume.fileName));
  }
  if (app.coverLetter && app.coverLetter.fileName) {
    deleteTasks.push(deleteFileFromSupabase(userId, 'cover_letters', app.coverLetter.fileName));
  }
  if (app.additionalDocument && app.additionalDocument.fileName) {
    deleteTasks.push(deleteFileFromSupabase(userId, 'additional_documents', app.additionalDocument.fileName));
  }
  if (app.screenshots && app.screenshots.length > 0) {
    for (let j = 0; j < app.screenshots.length; j++) {
      const scr = app.screenshots[j];
      if (scr && scr.fileName) {
        deleteTasks.push(deleteFileFromSupabase(userId, 'screenshots', scr.fileName));
      }
    }
  }
  if (deleteTasks.length > 0) {
    try {
      await Promise.all(deleteTasks);
    } catch (e) {
      console.error('Failed to delete application files from Supabase:', e);
    }
  }
}

async function wipeAllUserSupabaseStorage(userId) {
  if (!userId) return;
  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
  const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
  const folders = ['resumes', 'cover_letters', 'additional_documents', 'screenshots', 'documents'];

  const allFilesToDelete = [];
  for (const folder of folders) {
    try {
      const listRes = await fetch(`${supabaseUrl}/storage/v1/object/list/ApplyTrack`, {
        method: 'POST',
        headers: {
          'apikey': anonKey,
          'Authorization': `Bearer ${anonKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ prefix: `users/${userId}/${folder}`, limit: 500 })
      });
      if (listRes.ok) {
        const items = await listRes.json();
        for (const item of items) {
          if (item && item.name && item.id) {
            allFilesToDelete.push(`users/${userId}/${folder}/${item.name}`);
          }
        }
      }
    } catch (e) {
      console.warn(`[Wipe All] Could not list ${folder}:`, e);
    }
  }

  if (allFilesToDelete.length > 0) {
    try {
      await fetch(`${supabaseUrl}/storage/v1/object/ApplyTrack`, {
        method: 'DELETE',
        headers: {
          'apikey': anonKey,
          'Authorization': `Bearer ${anonKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ prefixes: allFilesToDelete })
      });
      console.log(`[Wipe All] Purged ${allFilesToDelete.length} files from Supabase Storage for user: ${userId}`);
    } catch (e) {
      console.error('[Wipe All] Error deleting storage files:', e);
    }
  }
}

/**
 * Merges local offline edits with remote data using field-level resolution:
 * 1. Array/Timeline fields (statusHistory): Merged and sorted chronologically without losing milestones.
 * 2. Attachments (resume, coverLetter, additionalDocument): Preserved if updated on either device.
 * 3. Screenshots: Deduplicated union by fileName.
 * 4. Colliding scalar fields (changed on both devices): Timestamp-based Last-Write-Wins (LWW).
 * 5. Non-colliding fields: Kept from whichever device modified them.
 */
export function mergeApplicationFields(localApp, remoteData) {
  if (!remoteData) return { ...localApp };
  if (!localApp) return { ...remoteData };

  const localUpdated = Number(localApp.updatedAt) || 0;
  const remoteUpdated = Number(remoteData.updatedAt) || 0;
  const localIsNewer = localUpdated >= remoteUpdated;

  // Start with remote data as the base
  const merged = { ...remoteData };

  // Scalar and basic fields in ApplyTrack
  const scalarFields = [
    'companyName', 'role', 'platform', 'status', 'jobDescription', 'notes', 'url', 'email'
  ];

  for (const field of scalarFields) {
    const localVal = localApp[field];
    const remoteVal = remoteData[field];

    if (localVal === undefined && remoteVal === undefined) continue;

    if (localVal !== undefined && remoteVal === undefined) {
      merged[field] = localVal;
    } else if (localVal === undefined && remoteVal !== undefined) {
      merged[field] = remoteVal;
    } else if (localVal === remoteVal) {
      merged[field] = localVal;
    } else {
      // Both sides modified the same field:
      // Timestamp-based Last-Write-Wins (LWW)
      merged[field] = localIsNewer ? localVal : remoteVal;
    }
  }

  // 1. Status History: Union merge & chronological sort (never lose milestones)
  const localHistory = Array.isArray(localApp.statusHistory) ? localApp.statusHistory : [];
  const remoteHistory = Array.isArray(remoteData.statusHistory) ? remoteData.statusHistory : [];
  const historyMap = new Map();

  for (const item of [...remoteHistory, ...localHistory]) {
    if (!item || !item.status) continue;
    const key = `${item.status}_${Math.floor((item.timestamp || 0) / 1000)}`;
    if (!historyMap.has(key)) {
      historyMap.set(key, item);
    }
  }
  merged.statusHistory = Array.from(historyMap.values()).sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));

  // If status was decided by LWW above, ensure the latest statusHistory reflects the final status
  if (merged.status && merged.statusHistory.length > 0) {
    const lastHistory = merged.statusHistory[merged.statusHistory.length - 1];
    if (lastHistory.status !== merged.status) {
      merged.statusHistory.push({
        status: merged.status,
        timestamp: Math.max(localUpdated, remoteUpdated, Date.now())
      });
    }
  }

  // 2. Attachments: preserve if added on either device; if both updated, LWW
  for (const attKey of ['resume', 'coverLetter', 'additionalDocument']) {
    const localAtt = localApp[attKey];
    const remoteAtt = remoteData[attKey];
    if (localAtt && !remoteAtt) {
      merged[attKey] = localAtt;
    } else if (!localAtt && remoteAtt) {
      merged[attKey] = remoteAtt;
    } else if (localAtt && remoteAtt) {
      merged[attKey] = localIsNewer ? localAtt : remoteAtt;
    }
  }

  // 3. Screenshots: Union by fileName
  const localScreenshots = Array.isArray(localApp.screenshots) ? localApp.screenshots : [];
  const remoteScreenshots = Array.isArray(remoteData.screenshots) ? remoteData.screenshots : [];
  const screenshotMap = new Map();
  for (const scr of [...remoteScreenshots, ...localScreenshots]) {
    if (scr && scr.fileName && !screenshotMap.has(scr.fileName)) {
      screenshotMap.set(scr.fileName, scr);
    }
  }
  merged.screenshots = Array.from(screenshotMap.values());

  // 4. Update timestamps and identifiers
  merged.id = localApp.id || remoteData.id;
  merged.uuid = localApp.uuid || remoteData.uuid;
  merged.createdAt = remoteData.createdAt || localApp.createdAt;
  merged.updatedAt = Math.max(localUpdated, remoteUpdated, Date.now());

  // Preserve local flags if present
  if (localApp.isPendingSync !== undefined) {
    merged.isPendingSync = localApp.isPendingSync;
  }
  if (localApp.isPendingDelete !== undefined) {
    merged.isPendingDelete = localApp.isPendingDelete;
  }

  return merged;
}

function startFirestoreListener(userId) {
  if (unsubscribeFirestore) unsubscribeFirestore();
  
  const colRef = collection(firestore, 'users', userId, 'job_applications');
  
  // Trigger syncing status during initial load so UI shows loading state
  if (isInitialLoad) {
    triggerSyncState('SYNCING');
  }
  
  unsubscribeFirestore = onSnapshot(colRef, (snapshot) => {
    // If the snapshot comes from local cache / offline, do NOT overwrite localStorage!
    // localStorage is the persistent source of truth while offline.
    if (snapshot.metadata.fromCache) {
      return;
    }

    const apps = [];
    snapshot.forEach((doc) => {
      const data = doc.data();
      const appUuid = data.uuid || doc.id;
      apps.push({
        ...data,
        id: appUuid,
        uuid: appUuid,
        resume: resolveRemoteAttachment(data.resume, userId, 'resumes'),
        coverLetter: resolveRemoteAttachment(data.coverLetter, userId, 'cover_letters'),
        additionalDocument: resolveRemoteAttachment(data.additionalDocument, userId, 'additional_documents'),
        screenshots: (data.screenshots || []).map(scr => resolveRemoteAttachment(scr, userId, 'screenshots'))
      });
    });
    
    // Preserve any locally pending apps that haven't synced yet
    const existingRaw = localStorage.getItem(KEYS.APPLICATIONS);
    let existingApps = [];
    try {
      existingApps = existingRaw ? JSON.parse(existingRaw) : [];
    } catch {
      existingApps = [];
    }
    const pendingLocally = existingApps.filter(a => a.isPendingSync);

    const mergedMap = new Map();
    apps.forEach(a => mergedMap.set(a.uuid, a));
    pendingLocally.forEach(a => {
      if (!mergedMap.has(a.uuid)) {
        mergedMap.set(a.uuid, a);
      } else {
        // Merge offline pending edit with remote data so local un-synced edits are not clobbered
        const remoteApp = mergedMap.get(a.uuid);
        mergedMap.set(a.uuid, mergeApplicationFields(a, remoteApp));
      }
    });
    const finalApps = Array.from(mergedMap.values());
    finalApps.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));

    // Update local cache
    localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(finalApps));
    
    // Dispatch data change
    window.dispatchEvent(new Event('applytrack_data_change'));
    
    if (isInitialLoad) {
      isInitialLoad = false;
      // Show success on initial load only if there is data
      if (snapshot.docs.length > 0) {
        triggerSyncState('SUCCESS');
      } else {
        triggerSyncState('IDLE');
      }
    } else {
      // For subsequent snapshots, only show SUCCESS if it came from remote and there are actual changes
      const isEchoOfLocalWrite = (Date.now() - lastLocalWriteTime) < 1500;
      if (!snapshot.metadata.hasPendingWrites && snapshot.docChanges().length > 0 && !isEchoOfLocalWrite) {
        triggerSyncState('SUCCESS');
      }
    }
  }, (error) => {
    console.error('Firestore listener error:', error);
    triggerSyncState('ERROR', error.message || 'Sync failed');
  });

  // Drain any pending items when listener starts
  processPendingSyncQueue();
}

let isDrainingQueue = false;
export async function processPendingSyncQueue() {
  if (isDrainingQueue || (typeof navigator !== 'undefined' && !navigator.onLine)) return;
  const userId = getEffectiveUserId();
  if (!userId) return;

  const raw = localStorage.getItem(KEYS.APPLICATIONS);
  let apps = [];
  try {
    apps = raw ? JSON.parse(raw) : [];
  } catch {
    return;
  }

  const pendingDeletes = apps.filter(a => a.isPendingDelete);
  const pendingApps = apps.filter(a => a.isPendingSync && !a.isPendingDelete);
  if (pendingDeletes.length === 0 && pendingApps.length === 0) return;

  isDrainingQueue = true;
  triggerSyncState('SYNCING');

  try {
    // 1. Process Pending Deletions
    for (const app of pendingDeletes) {
      try {
        const userDocRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        await deleteDoc(userDocRef);
        await deleteApplicationAttachments(userId, app);
        await clearStagedOfflineAttachments(app.uuid);
      } catch (delErr) {
        console.warn('[Offline Sync] Failed to delete remote app during queue drain:', delErr);
      }

      // Remove from localStorage
      const currentRaw = localStorage.getItem(KEYS.APPLICATIONS);
      const currentApps = currentRaw ? JSON.parse(currentRaw) : [];
      const updated = currentApps.filter(a => a.uuid !== app.uuid);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));
    }

    // 2. Process Pending Add/Edits
    for (const app of pendingApps) {
      // 1. Upload any staged offline attachments
      const staged = await getStagedOfflineAttachments(app.uuid);
      if (staged) {
        const uploadTasks = [];
        if (staged.resume?.dataUrl) {
          uploadTasks.push(uploadFileToSupabase(userId, 'resumes', staged.resume.fileName, staged.resume.dataUrl));
        }
        if (staged.coverLetter?.dataUrl) {
          uploadTasks.push(uploadFileToSupabase(userId, 'cover_letters', staged.coverLetter.fileName, staged.coverLetter.dataUrl));
        }
        if (staged.additionalDocument?.dataUrl) {
          uploadTasks.push(uploadFileToSupabase(userId, 'additional_documents', staged.additionalDocument.fileName, staged.additionalDocument.dataUrl));
        }
        if (staged.screenshots && Array.isArray(staged.screenshots)) {
          for (const scr of staged.screenshots) {
            if (scr.dataUrl) {
              uploadTasks.push(uploadFileToSupabase(userId, 'screenshots', scr.fileName, scr.dataUrl));
            }
          }
        }
        if (uploadTasks.length > 0) {
          await Promise.all(uploadTasks);
        }
        await clearStagedOfflineAttachments(app.uuid);
      }

      // 2. Check if this document was deleted remotely while offline (DELETE WINS)
      const userDocRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
      let remoteData = null;
      if (!app.isNewOfflineApp) {
        try {
          const docSnap = await getDoc(userDocRef);
          if (!docSnap.exists()) {
            // Document was deleted on the server while offline: Delete Wins!
            console.warn(`[Offline Sync] Discarding offline update for '${app.uuid}' because it was deleted remotely.`);
            await clearStagedOfflineAttachments(app.uuid);

            const currentRaw = localStorage.getItem(KEYS.APPLICATIONS);
            const currentApps = currentRaw ? JSON.parse(currentRaw) : [];
            const updated = currentApps.filter(a => a.uuid !== app.uuid);
            localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
            window.dispatchEvent(new Event('applytrack_data_change'));
            continue;
          } else {
            remoteData = docSnap.data();
          }
        } catch (checkErr) {
          console.warn('[Offline Sync] Remote doc check failed, proceeding with sync:', checkErr);
        }
      }

      // 3. Field-Level Merge with remote document if available
      let mergedApp = { ...app };
      if (remoteData) {
        mergedApp = mergeApplicationFields(app, remoteData);
      }

      const appToSync = { ...mergedApp };
      delete appToSync.isPendingSync;
      delete appToSync.isNewOfflineApp;
      delete appToSync.isPendingDelete;
      if (appToSync.resume?.url) delete appToSync.resume.url;
      if (appToSync.coverLetter?.url) delete appToSync.coverLetter.url;
      if (appToSync.additionalDocument?.url) delete appToSync.additionalDocument.url;
      if (appToSync.screenshots) {
        appToSync.screenshots = appToSync.screenshots.map(s => {
          const c = { ...s };
          delete c.url;
          return c;
        });
      }

      await setDoc(userDocRef, appToSync);

      // 4. Update local storage with the merged application and clear isPendingSync
      const currentRaw = localStorage.getItem(KEYS.APPLICATIONS);
      const currentApps = currentRaw ? JSON.parse(currentRaw) : [];
      const updated = currentApps.map(a => a.uuid === app.uuid ? { ...mergedApp, isPendingSync: false } : a);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));
    }

    triggerSyncState('SUCCESS');
    window.dispatchEvent(new CustomEvent('applytrack_toast', {
      detail: {
        message: 'Synced offline changes with cloud',
        type: 'success'
      }
    }));
  } catch (err) {
    console.error('[Offline Sync] Failed to drain pending sync queue:', err);
    triggerSyncState('ERROR', 'Offline sync will retry when connected');
  } finally {
    isDrainingQueue = false;
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => {
    processPendingSyncQueue();
  });
}

function stopFirestoreListener() {
  if (unsubscribeFirestore) {
    unsubscribeFirestore();
    unsubscribeFirestore = null;
  }
  currentSyncState = 'IDLE';
}

// Firebase Auth Observer Integration
onAuthStateChanged(auth, (firebaseUser) => {
  isAuthInitialized = true;
  if (firebaseUser) {
    const googleProvider = firebaseUser.providerData?.find(p => p.providerId === 'google.com') || firebaseUser.providerData?.[0];
    const displayName = firebaseUser.displayName || googleProvider?.displayName || firebaseUser.email?.split('@')[0] || 'Google User';
    const email = firebaseUser.email || googleProvider?.email || '';
    
    let photoURL = firebaseUser.photoURL || googleProvider?.photoURL;
    if (photoURL && photoURL.includes('googleusercontent.com')) {
      // Upgrade from low-res 96px thumbnail to crisp 256px resolution for Retina displays
      photoURL = photoURL.replace(/=s\d+(-c)?$/, '=s256-c');
    }
    if (!photoURL) {
      photoURL = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=2F3A4A&color=fff&bold=true`;
    }

    const formattedUser = {
      uid: firebaseUser.uid,
      displayName,
      email,
      photoURL
    };
    
    localStorage.setItem(KEYS.USER, JSON.stringify(formattedUser));
    currentUserId = firebaseUser.uid;
    
    startFirestoreListener(firebaseUser.uid);
    window.dispatchEvent(new Event('applytrack_auth_change'));
  } else {
    // When firebaseUser is null:
    // If we have a cached user in localStorage, KEEP IT! The user did not log out.
    // They are either offline, or Firebase Auth hasn't finished handshake yet.
    const cachedUser = typeof localStorage !== 'undefined' ? localStorage.getItem(KEYS.USER) : null;
    if (cachedUser) {
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('applytrack_auth_change'));
      return;
    }

    // Only if there is NO cached user at all:
    stopFirestoreListener();
    currentUserId = null;
    isInitialLoad = true;
    if (typeof window !== 'undefined') window.dispatchEvent(new Event('applytrack_auth_change'));
  }
});

export const db = {
  getSyncState() {
    return currentSyncState;
  },
  isAuthReady() {
    if (isAuthInitialized) return true;
    if (typeof navigator !== 'undefined' && !navigator.onLine && this.getCurrentUser()) {
      return true;
    }
    return isAuthInitialized;
  },

  // --- AUTH OPERATIONS ---
  getCurrentUser() {
    const user = localStorage.getItem(KEYS.USER);
    return user ? JSON.parse(user) : null;
  },

  setCurrentUser(user) {
    if (user) {
      localStorage.setItem(KEYS.USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(KEYS.USER);
    }
    window.dispatchEvent(new Event('applytrack_auth_change'));
  },

  logout() {
    stopFirestoreListener();
    localStorage.removeItem(KEYS.USER);
    localStorage.removeItem(KEYS.APPLICATIONS);
    currentUserId = null;
    isInitialLoad = true;
    window.dispatchEvent(new Event('applytrack_auth_change'));
    signOut(auth).catch((err) => {
      console.error('Logout error:', err);
    });
  },

  // --- JOB APPLICATIONS CRUD (Firestore Online + Local Cache Sync) ---
  getApplications() {
    const appsStr = localStorage.getItem(KEYS.APPLICATIONS);
    return appsStr ? JSON.parse(appsStr) : [];
  },

  saveApplications(apps) {
    // Only used to temporarily push cache (e.g. backup checks)
    localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(apps));
    window.dispatchEvent(new Event('applytrack_data_change'));
  },

  getApplicationById(id) {
    if (!id) return null;
    const apps = this.getApplications();
    const strId = String(id).toLowerCase();
    return apps.find(a => 
      String(a.id) === String(id) || 
      String(a.uuid) === String(id) ||
      String(a.id).toLowerCase() === strId ||
      String(a.uuid).toLowerCase() === strId
    ) || null;
  },

  async addApplication(appData) {
    const userId = getEffectiveUserId();
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();

    const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;
    if (isOffline) {
      const uuid = appData.uuid || generateUuid();
      const apps = this.getApplications();

      // Stage offline file binaries in IndexedDB
      const filesToStage = {};
      if (appData.resume?.dataUrl) filesToStage.resume = appData.resume;
      if (appData.coverLetter?.dataUrl) filesToStage.coverLetter = appData.coverLetter;
      if (appData.additionalDocument?.dataUrl) filesToStage.additionalDocument = appData.additionalDocument;
      if (appData.screenshots && appData.screenshots.some(s => s.dataUrl)) filesToStage.screenshots = appData.screenshots;
      if (Object.keys(filesToStage).length > 0) {
        await stageOfflineAttachments(uuid, filesToStage);
      }

      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;
      const serializedApp = {
        id: uuid,
        uuid: uuid,
        companyName: appData.companyName || null,
        role: appData.role || null,
        platform: appData.platform || 'Direct',
        status: appData.status || 'Applied',
        jobDescription: appData.jobDescription || '',
        notes: appData.notes || '',
        url: appData.url || '',
        email: appData.email || '',
        createdAt: appData.createdAt || Date.now(),
        updatedAt: Date.now(),
        statusHistory: appData.statusHistory || [
          { status: appData.status || 'Applied', timestamp: appData.createdAt || Date.now() }
        ],
        resume: cleanAttachment(appData.resume),
        coverLetter: cleanAttachment(appData.coverLetter),
        additionalDocument: cleanAttachment(appData.additionalDocument),
        screenshots: (appData.screenshots || []).map(cleanAttachment),
        isPendingSync: true,
        isNewOfflineApp: true
      };

      const updatedApps = [...apps, serializedApp];
      updatedApps.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updatedApps));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return serializedApp;
    }

    try {
      const uuid = appData.uuid || generateUuid();

      // 1. Upload files to Supabase Storage concurrently in parallel if they contain new binary contents (dataUrl)
      const uploadTasks = [];
      if (appData.resume && appData.resume.dataUrl) {
        uploadTasks.push(uploadFileToSupabase(userId, 'resumes', appData.resume.fileName, appData.resume.dataUrl));
      }
      if (appData.coverLetter && appData.coverLetter.dataUrl) {
        uploadTasks.push(uploadFileToSupabase(userId, 'cover_letters', appData.coverLetter.fileName, appData.coverLetter.dataUrl));
      }
      if (appData.additionalDocument && appData.additionalDocument.dataUrl) {
        uploadTasks.push(uploadFileToSupabase(userId, 'additional_documents', appData.additionalDocument.fileName, appData.additionalDocument.dataUrl));
      }
      if (appData.screenshots && appData.screenshots.length > 0) {
        for (let i = 0; i < appData.screenshots.length; i++) {
          const scr = appData.screenshots[i];
          if (scr.dataUrl) {
            uploadTasks.push(uploadFileToSupabase(userId, 'screenshots', scr.fileName, scr.dataUrl));
          }
        }
      }
      if (uploadTasks.length > 0) {
        await Promise.all(uploadTasks);
      }

      // 2. Create the Firestore model mapping (strictly metadata filenames, no dataUrl)
      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;

      const serializedApp = {
        id: uuid,
        uuid: uuid,
        companyName: appData.companyName || null,
        role: appData.role || null,
        platform: appData.platform || 'Direct',
        status: appData.status || 'Applied',
        jobDescription: appData.jobDescription || '',
        notes: appData.notes || '',
        url: appData.url || '',
        email: appData.email || '',
        createdAt: appData.createdAt || Date.now(),
        updatedAt: Date.now(),
        statusHistory: appData.statusHistory || [
          { status: appData.status || 'Applied', timestamp: appData.createdAt || Date.now() }
        ],
        resume: cleanAttachment(appData.resume),
        coverLetter: cleanAttachment(appData.coverLetter),
        additionalDocument: cleanAttachment(appData.additionalDocument),
        screenshots: (appData.screenshots || []).map(cleanAttachment)
      };

      // 3. Write document to Firestore
      const userDocRef = doc(firestore, 'users', userId, 'job_applications', uuid);
      await setDoc(userDocRef, serializedApp);

      return serializedApp;
    } catch (e) {
      console.error('Add application failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to save application');
      throw e;
    }
  },

  async updateApplication(id, updatedData) {
    const userId = getEffectiveUserId();
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();

    const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;
    if (isOffline) {
      const originalApp = this.getApplicationById(id);
      if (!originalApp) throw new Error('Application not found');

      // Stage any new offline file binaries in IndexedDB
      const filesToStage = {};
      if (updatedData.resume?.dataUrl && (!originalApp.resume || originalApp.resume.fileName !== updatedData.resume.fileName)) {
        filesToStage.resume = updatedData.resume;
      }
      if (updatedData.coverLetter?.dataUrl && (!originalApp.coverLetter || originalApp.coverLetter.fileName !== updatedData.coverLetter.fileName)) {
        filesToStage.coverLetter = updatedData.coverLetter;
      }
      if (updatedData.additionalDocument?.dataUrl && (!originalApp.additionalDocument || originalApp.additionalDocument.fileName !== updatedData.additionalDocument.fileName)) {
        filesToStage.additionalDocument = updatedData.additionalDocument;
      }
      if (updatedData.screenshots && updatedData.screenshots.length > 0) {
        const newScreenshots = updatedData.screenshots.filter(s => s.dataUrl);
        if (newScreenshots.length > 0) {
          filesToStage.screenshots = newScreenshots;
        }
      }
      if (Object.keys(filesToStage).length > 0) {
        await stageOfflineAttachments(originalApp.uuid, filesToStage);
      }

      // Compute status history changes
      let oldHistory = originalApp.statusHistory || [];
      let newHistory = [];
      const timeApplied = updatedData.createdAt || Date.now();

      if (originalApp.status !== updatedData.status) {
        newHistory = [...oldHistory, { status: updatedData.status, timestamp: timeApplied }];
      } else {
        if (oldHistory.length > 0) {
          const updatedLast = { ...oldHistory[oldHistory.length - 1], timestamp: timeApplied };
          newHistory = [...oldHistory.slice(0, -1), updatedLast];
        } else {
          newHistory = [{ status: updatedData.status, timestamp: timeApplied }];
        }
      }

      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;
      const updatedApp = {
        ...originalApp,
        ...updatedData,
        id: originalApp.id,
        uuid: originalApp.uuid,
        updatedAt: Date.now(),
        statusHistory: newHistory,
        resume: cleanAttachment(updatedData.resume) || originalApp.resume,
        coverLetter: cleanAttachment(updatedData.coverLetter) || originalApp.coverLetter,
        additionalDocument: cleanAttachment(updatedData.additionalDocument) || originalApp.additionalDocument,
        screenshots: updatedData.screenshots ? updatedData.screenshots.map(cleanAttachment) : originalApp.screenshots,
        isPendingSync: true
      };

      const apps = this.getApplications();
      const updatedApps = apps.map(a => a.id === originalApp.id ? updatedApp : a);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updatedApps));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return updatedApp;
    }
    try {
      const originalApp = this.getApplicationById(id);
      if (!originalApp) throw new Error('Application not found');

      // 1. Upload new attachments concurrently in parallel if they have dataUrl and don't match the original file
      const uploadTasks = [];
      if (updatedData.resume && updatedData.resume.dataUrl && (!originalApp.resume || originalApp.resume.fileName !== updatedData.resume.fileName)) {
        uploadTasks.push(uploadFileToSupabase(userId, 'resumes', updatedData.resume.fileName, updatedData.resume.dataUrl));
      }
      if (updatedData.coverLetter && updatedData.coverLetter.dataUrl && (!originalApp.coverLetter || originalApp.coverLetter.fileName !== updatedData.coverLetter.fileName)) {
        uploadTasks.push(uploadFileToSupabase(userId, 'cover_letters', updatedData.coverLetter.fileName, updatedData.coverLetter.dataUrl));
      }
      if (updatedData.additionalDocument && updatedData.additionalDocument.dataUrl && (!originalApp.additionalDocument || originalApp.additionalDocument.fileName !== updatedData.additionalDocument.fileName)) {
        uploadTasks.push(uploadFileToSupabase(userId, 'additional_documents', updatedData.additionalDocument.fileName, updatedData.additionalDocument.dataUrl));
      }
      if (updatedData.screenshots && updatedData.screenshots.length > 0) {
        for (let i = 0; i < updatedData.screenshots.length; i++) {
          const scr = updatedData.screenshots[i];
          if (scr.dataUrl) {
            const hasMatch = originalApp.screenshots && originalApp.screenshots.some(s => s.fileName === scr.fileName);
            if (!hasMatch) {
              uploadTasks.push(uploadFileToSupabase(userId, 'screenshots', scr.fileName, scr.dataUrl));
            }
          }
        }
      }
      if (uploadTasks.length > 0) {
        await Promise.all(uploadTasks);
      }

      // 1b. Delete removed or replaced attachments from Supabase Storage
      const deleteTasks = [];
      if (originalApp.resume && originalApp.resume.fileName && (!updatedData.resume || updatedData.resume.fileName !== originalApp.resume.fileName)) {
        deleteTasks.push(deleteFileFromSupabase(userId, 'resumes', originalApp.resume.fileName));
      }
      if (originalApp.coverLetter && originalApp.coverLetter.fileName && (!updatedData.coverLetter || updatedData.coverLetter.fileName !== originalApp.coverLetter.fileName)) {
        deleteTasks.push(deleteFileFromSupabase(userId, 'cover_letters', originalApp.coverLetter.fileName));
      }
      if (originalApp.additionalDocument && originalApp.additionalDocument.fileName && (!updatedData.additionalDocument || updatedData.additionalDocument.fileName !== originalApp.additionalDocument.fileName)) {
        deleteTasks.push(deleteFileFromSupabase(userId, 'additional_documents', originalApp.additionalDocument.fileName));
      }
      if (originalApp.screenshots && originalApp.screenshots.length > 0) {
        const newScreenshots = updatedData.screenshots || [];
        for (let sIdx = 0; sIdx < originalApp.screenshots.length; sIdx++) {
          const oldScr = originalApp.screenshots[sIdx];
          const stillExists = newScreenshots.some(s => s.fileName === oldScr.fileName);
          if (!stillExists && oldScr.fileName) {
            deleteTasks.push(deleteFileFromSupabase(userId, 'screenshots', oldScr.fileName));
          }
        }
      }
      if (deleteTasks.length > 0) {
        try {
          await Promise.all(deleteTasks);
        } catch (err) {
          console.error('Supabase old attachment cleanup failed:', err);
        }
      }

      // 2. Compute status history changes
      let oldHistory = originalApp.statusHistory || [];
      let newHistory = [];
      const timeApplied = updatedData.createdAt || Date.now();

      if (originalApp.status !== updatedData.status) {
        newHistory = [...oldHistory, { status: updatedData.status, timestamp: timeApplied }];
      } else {
        if (oldHistory.length > 0) {
          const updatedLast = { ...oldHistory[oldHistory.length - 1], timestamp: timeApplied };
          newHistory = [...oldHistory.slice(0, -1), updatedLast];
        } else {
          newHistory = [{ status: updatedData.status, timestamp: timeApplied }];
        }
      }

      // 3. Update Firestore metadata (no binary content)
      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;

      const serializedApp = {
        ...originalApp,
        companyName: updatedData.companyName || null,
        role: updatedData.role || null,
        platform: updatedData.platform || 'Direct',
        status: updatedData.status || 'Applied',
        jobDescription: updatedData.jobDescription || '',
        notes: updatedData.notes || '',
        url: updatedData.url || '',
        email: updatedData.email || '',
        statusHistory: newHistory,
        resume: cleanAttachment(updatedData.resume),
        coverLetter: cleanAttachment(updatedData.coverLetter),
        additionalDocument: cleanAttachment(updatedData.additionalDocument),
        screenshots: (updatedData.screenshots || []).map(cleanAttachment),
        updatedAt: Date.now()
      };

      const userDocRef = doc(firestore, 'users', userId, 'job_applications', originalApp.uuid);
      await setDoc(userDocRef, serializedApp);

      return serializedApp;
    } catch (e) {
      console.error('Update application failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to update application');
      throw e;
    }
  },

  async deleteApplication(id) {
    const userId = getEffectiveUserId();
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    const app = this.getApplicationById(id);
    if (!app) throw new Error('Application not found');

    // Save to local cache temporary deleted list for undo
    localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify([app]));

    const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;
    if (isOffline) {
      const apps = this.getApplications();
      // If the app was ONLY created offline and never synced to cloud, delete it immediately
      if (app.isPendingSync && !app.updatedAtCloud) {
        await clearStagedOfflineAttachments(app.uuid);
        const updated = apps.filter(a => a.id !== app.id && a.uuid !== app.uuid);
        localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
        window.dispatchEvent(new Event('applytrack_data_change'));
        return { success: true, pendingDelete: false };
      }

      // If it exists in the cloud, tag it as pending deletion so it can sync deletion when online
      const updated = apps.map(a => (a.id === app.id || a.uuid === app.uuid) ? { ...a, isPendingDelete: true } : a);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return { success: true, pendingDelete: true };
    }

    try {
      // Delete the Firestore record
      const userDocRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
      await deleteDoc(userDocRef);

      // Delete attachments from Supabase Storage
      await deleteApplicationAttachments(userId, app);

      // Remove from localStorage
      const apps = this.getApplications();
      const updated = apps.filter(a => a.id !== app.id && a.uuid !== app.uuid);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));

      return { success: true, pendingDelete: false };
    } catch (e) {
      console.error('Delete application failed, marking pending delete:', e);
      const apps = this.getApplications();
      const updated = apps.map(a => (a.id === app.id || a.uuid === app.uuid) ? { ...a, isPendingDelete: true } : a);
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return { success: true, pendingDelete: true };
    }
  },

  async deleteMultipleApplications(ids) {
    const userId = getEffectiveUserId();
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    const apps = this.getApplications();
    const idStrings = ids.map(String);
    const appsToDelete = apps.filter(a => 
      idStrings.includes(String(a.id)) || 
      idStrings.includes(String(a.uuid))
    );
    if (appsToDelete.length === 0) return { success: false, pendingDelete: false };

    // Save metadata to temp cache for undo
    localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify(appsToDelete));

    const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;
    if (isOffline) {
      let updatedApps = [...apps];
      let hasPendingDelete = false;
      for (const app of appsToDelete) {
        if (app.isPendingSync && !app.updatedAtCloud) {
          await clearStagedOfflineAttachments(app.uuid);
          updatedApps = updatedApps.filter(a => a.id !== app.id && a.uuid !== app.uuid);
        } else {
          hasPendingDelete = true;
          updatedApps = updatedApps.map(a => (a.id === app.id || a.uuid === app.uuid) ? { ...a, isPendingDelete: true } : a);
        }
      }
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updatedApps));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return { success: true, pendingDelete: hasPendingDelete };
    }

    try {
      // Batch delete from Firestore
      const batch = writeBatch(firestore);
      appsToDelete.forEach(app => {
        const docRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        batch.delete(docRef);
      });
      await batch.commit();

      // Delete attachments from Supabase Storage for all deleted apps
      await Promise.all(appsToDelete.map(app => deleteApplicationAttachments(userId, app)));

      // Remove from localStorage
      const updated = apps.filter(a => !appsToDelete.some(d => d.id === a.id || d.uuid === a.uuid));
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updated));
      window.dispatchEvent(new Event('applytrack_data_change'));

      return { success: true, pendingDelete: false };
    } catch (e) {
      console.error('Multiple delete failed, marking pending delete:', e);
      let updatedApps = [...apps];
      for (const app of appsToDelete) {
        updatedApps = updatedApps.map(a => (a.id === app.id || a.uuid === app.uuid) ? { ...a, isPendingDelete: true } : a);
      }
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(updatedApps));
      window.dispatchEvent(new Event('applytrack_data_change'));
      return { success: true, pendingDelete: true };
    }
  },

  async undoDelete() {
    const userId = getEffectiveUserId();
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const temp = localStorage.getItem(KEYS.DELETED_TEMP);
      if (!temp) return false;

      const appsToRestore = JSON.parse(temp);
      const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;

      if (isOffline) {
        const apps = this.getApplications();
        const mergedMap = new Map();
        apps.forEach(a => mergedMap.set(a.uuid, a));
        appsToRestore.forEach(a => {
          const restored = { ...a };
          delete restored.isPendingDelete;
          mergedMap.set(a.uuid, restored);
        });
        const finalApps = Array.from(mergedMap.values());
        finalApps.sort((a, b) => (a.id || 0) - (b.id || 0));
        localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(finalApps));
        localStorage.removeItem(KEYS.DELETED_TEMP);
        window.dispatchEvent(new Event('applytrack_data_change'));
        return true;
      }
      
      const batch = writeBatch(firestore);
      appsToRestore.forEach(app => {
        const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;
        const serialized = {
          ...app,
          resume: cleanAttachment(app.resume),
          coverLetter: cleanAttachment(app.coverLetter),
          additionalDocument: cleanAttachment(app.additionalDocument),
          screenshots: (app.screenshots || []).map(cleanAttachment)
        };
        delete serialized.isPendingDelete;
        const docRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        batch.set(docRef, serialized);
      });
      await batch.commit();

      const apps = this.getApplications();
      const mergedMap = new Map();
      apps.forEach(a => mergedMap.set(a.uuid, a));
      appsToRestore.forEach(a => {
        const restored = { ...a };
        delete restored.isPendingDelete;
        mergedMap.set(a.uuid, restored);
      });
      const finalApps = Array.from(mergedMap.values());
      finalApps.sort((a, b) => (a.id || 0) - (b.id || 0));
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(finalApps));

      localStorage.removeItem(KEYS.DELETED_TEMP);
      window.dispatchEvent(new Event('applytrack_data_change'));
      return true;
    } catch (e) {
      console.error('Undo delete failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to restore applications');
      throw e;
    }
  },

  async clearUndoCache() {
    const userId = getEffectiveUserId();
    if (!userId) return;

    const temp = localStorage.getItem(KEYS.DELETED_TEMP);
    if (!temp) return;

    try {
      const appsToDelete = JSON.parse(temp);
      for (let i = 0; i < appsToDelete.length; i++) {
        const app = appsToDelete[i];
        
        // Remove storage files since the deletion toast expired and undo is no longer possible
        if (app.resume && app.resume.fileName) {
          await deleteFileFromSupabase(userId, 'resumes', app.resume.fileName);
        }
        if (app.coverLetter && app.coverLetter.fileName) {
          await deleteFileFromSupabase(userId, 'cover_letters', app.coverLetter.fileName);
        }
        if (app.additionalDocument && app.additionalDocument.fileName) {
          await deleteFileFromSupabase(userId, 'additional_documents', app.additionalDocument.fileName);
        }
        if (app.screenshots && app.screenshots.length > 0) {
          for (let j = 0; j < app.screenshots.length; j++) {
            const scr = app.screenshots[j];
            await deleteFileFromSupabase(userId, 'screenshots', scr.fileName);
          }
        }
      }
    } catch (e) {
      console.error('Supabase cleanup on sync check failed:', e);
    } finally {
      localStorage.removeItem(KEYS.DELETED_TEMP);
    }
  },

  // --- THEME OPERATIONS ---
  getTheme() {
    return localStorage.getItem(KEYS.THEME) || 'system';
  },

  setTheme(theme) {
    localStorage.setItem(KEYS.THEME, theme);
    window.dispatchEvent(new Event('applytrack_theme_change'));
  },

  // --- DATA MANAGEMENT ---
  async resetDatabase() {
    const userId = getEffectiveUserId();
    if (!userId) return;

    markLocalWrite();
    try {
      const apps = this.getApplications();
      
      // 1. Delete all attachments in Supabase Storage for known applications
      await Promise.all(apps.map(app => deleteApplicationAttachments(userId, app)));

      // 1b. Deep purge all remaining/orphaned files in Supabase Storage for this user
      await wipeAllUserSupabaseStorage(userId);

      // 2. Fetch and delete all Firestore records under users/{userId}/job_applications
      const querySnapshot = await getDocs(collection(firestore, 'users', userId, 'job_applications'));
      const batch = writeBatch(firestore);
      querySnapshot.forEach(docSnap => {
        batch.delete(docSnap.ref);
      });
      // Also delete user parent doc if present
      batch.delete(doc(firestore, 'users', userId));
      await batch.commit();

      // 3. Clear all local application caches and staged offline files
      localStorage.removeItem(KEYS.APPLICATIONS);
      localStorage.removeItem(KEYS.DELETED_TEMP);
      for (const app of apps) {
        await clearStagedOfflineAttachments(app.uuid);
      }
      
      // Dispatch data change to update UI
      window.dispatchEvent(new Event('applytrack_data_change'));
      triggerSyncState('SUCCESS');
    } catch (e) {
      console.error('Reset database failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to wipe database');
      throw e;
    }
  },

  exportData() {
    // Standard JSON export helper
    const apps = this.getApplications();
    const dataStr = JSON.stringify(apps, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `applytrack_backup_${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },

  importData(_jsonString) {
    // Keep signature for JSON backups (used in settings checks)
    return { success: false, error: 'ZIP backup matches native format' };
  },

  // --- ANALYTICS ENGINE (Computes calculations locally from the synchronized applications) ---
  getAnalytics() {
    const apps = this.getApplications();
    const total = apps.length;

    if (total === 0) {
      return {
        total: 0,
        applied: 0,
        saved: 0,
        interviews: 0,
        offers: 0,
        rejected: 0,
        responses: 0,
        successRate: 0,
        rejectionRate: 0,
        interviewRate: 0,
        responseRate: 0,
        applicationsThisWeek: 0,
        applicationsThisMonth: 0,
        statusDistribution: [],
        platforms: [],
        resumeStats: [],
        monthlyActivity: {}
      };
    }

    const applied = apps.filter(a => a.status === 'Applied').length;
    const saved = apps.filter(a => a.status === 'Saved').length;
    const interviews = apps.filter(a => a.status === 'Interview').length;
    const offers = apps.filter(a => a.status === 'Offer').length;
    const rejected = apps.filter(a => a.status === 'Rejected').length;
    const responses = interviews + offers + rejected;

    const activeTotal = total - saved;
    const successRate = activeTotal > 0 ? Math.round((offers / activeTotal) * 100) : 0;
    const rejectionRate = activeTotal > 0 ? Math.round((rejected / activeTotal) * 100) : 0;
    const interviewRate = activeTotal > 0 ? Math.round((interviews / activeTotal) * 100) : 0;
    const responseRate = activeTotal > 0 ? Math.round(((interviews + offers + rejected) / activeTotal) * 100) : 0;

    const now = new Date();
    
    // Start of current week (Monday)
    const startOfWeek = new Date(now);
    const day = startOfWeek.getDay();
    const diff = startOfWeek.getDate() - day + (day === 0 ? -6 : 1);
    startOfWeek.setDate(diff);
    startOfWeek.setHours(0, 0, 0, 0);
    
    // End of current week (Sunday)
    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 6);
    endOfWeek.setHours(23, 59, 59, 999);

    // Start of current month (1st)
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);

    // End of current month (last day)
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
    
    const applicationsThisWeek = apps.filter(a => a.createdAt >= startOfWeek.getTime() && a.createdAt <= endOfWeek.getTime() && a.status !== 'Saved').length;
    const applicationsThisMonth = apps.filter(a => a.createdAt >= startOfMonth.getTime() && a.createdAt <= endOfMonth.getTime() && a.status !== 'Saved').length;

    const statusDistribution = [
      { name: 'Applied', count: applied, color: '#FFB300' },
      { name: 'Saved', count: saved, color: '#78909C' },
      { name: 'Interview', count: interviews, color: '#4CAF50' },
      { name: 'Offer', count: offers, color: '#1E88E5' },
      { name: 'Rejected', count: rejected, color: '#E53935' }
    ].filter(s => s.count > 0);

    const standardPlatforms = ['LinkedIn', 'Indeed', 'Email', 'Website'];
    const platformMap = {};
    apps.forEach(a => {
      const plat = a.platform ? a.platform.trim() : '';
      if (!plat) return;
      const matched = standardPlatforms.find(sp => sp.toLowerCase() === plat.toLowerCase()) || 'Other';
      platformMap[matched] = (platformMap[matched] || 0) + 1;
    });
    const platforms = Object.entries(platformMap)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);

    const resumeMap = {};
    apps.forEach(a => {
      if (!a.resume || !a.resume.originalName) return;
      const resName = a.resume.originalName;
      if (!resumeMap[resName]) {
        resumeMap[resName] = { 
          resumeName: resName, 
          totalUsed: 0, 
          interviewCount: 0, 
          offerCount: 0, 
          rejectedCount: 0 
        };
      }
      resumeMap[resName].totalUsed += 1;
      if (a.status === 'Interview') {
        resumeMap[resName].interviewCount += 1;
      } else if (a.status === 'Offer') {
        resumeMap[resName].offerCount += 1;
      } else if (a.status === 'Rejected') {
        resumeMap[resName].rejectedCount += 1;
      }
    });

    const resumeStats = Object.values(resumeMap)
      .sort((a, b) => b.totalUsed - a.totalUsed);

    const monthlyActivity = {};
    apps.forEach(a => {
      const date = new Date(a.createdAt);
      const year = date.getFullYear();
      const month = date.toLocaleString('default', { month: 'short' });
      
      if (!monthlyActivity[year]) {
        monthlyActivity[year] = {};
      }
      monthlyActivity[year][month] = (monthlyActivity[year][month] || 0) + 1;
    });

    return {
      total,
      applied,
      saved,
      interviews,
      offers,
      rejected,
      responses,
      successRate,
      rejectionRate,
      interviewRate,
      responseRate,
      applicationsThisWeek,
      applicationsThisMonth,
      statusDistribution,
      platforms,
      resumeStats,
      monthlyActivity
    };
  },

  isOnline() {
    return typeof navigator !== 'undefined' ? navigator.onLine : true;
  },

  generateUuid,
  mergeApplicationFields,
  processPendingSyncQueue
};
