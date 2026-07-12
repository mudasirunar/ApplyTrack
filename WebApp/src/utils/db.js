import { onAuthStateChanged, signOut } from 'firebase/auth';
import { auth, firestore } from './firebase';
import { collection, onSnapshot, doc, setDoc, deleteDoc, writeBatch } from 'firebase/firestore';

const KEYS = {
  USER: 'applytrack_user',
  APPLICATIONS: 'applytrack_applications',
  THEME: 'applytrack_theme',
  DELETED_TEMP: 'applytrack_deleted_temp'
};

let unsubscribeFirestore = null;
let isInitialLoad = true;
let currentUserId = null;
let isAuthInitialized = false;
let lastLocalWriteTime = 0;
let currentSyncState = 'IDLE';

function markLocalWrite() {
  lastLocalWriteTime = Date.now();
}

// Helper to trigger sync status custom events
function triggerSyncState(state, message = '') {
  currentSyncState = state;
  window.dispatchEvent(new CustomEvent('applytrack_sync_state', {
    detail: { state, message }
  }));
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
async function checkFileExistsOnSupabase(userId, type, fileName) {
  try {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const checkUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack/users/${userId}/${type}/${fileName}`;

    const checkResponse = await fetch(checkUrl, {
      method: 'HEAD',
      headers: {
        'Authorization': `Bearer ${anonKey}`,
        'apikey': anonKey
      }
    });
    return checkResponse.ok;
  } catch (e) {
    return false;
  }
}

async function uploadFileToSupabase(userId, type, fileName, dataUrl) {
  if (!dataUrl) return false;
  
  // Check if file already exists first (avoid duplicate uploads)
  const exists = await checkFileExistsOnSupabase(userId, type, fileName);
  if (exists) return true;

  try {
    const response = await fetch(dataUrl);
    const blob = await response.blob();
    
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const uploadUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack/users/${userId}/${type}/${fileName}`;

    const uploadResponse = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${anonKey}`,
        'apikey': anonKey,
        'Content-Type': blob.type || 'application/octet-stream'
      },
      body: blob
    });

    return uploadResponse.ok;
  } catch (e) {
    console.error('Supabase upload failed:', e);
    return false;
  }
}

async function deleteFileFromSupabase(userId, type, fileName) {
  try {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const deleteUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack/users/${userId}/${type}/${fileName}`;

    const deleteResponse = await fetch(deleteUrl, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${anonKey}`,
        'apikey': anonKey
      }
    });
    return deleteResponse.ok;
  } catch (e) {
    console.error('Supabase delete failed:', e);
    return false;
  }
}

function startFirestoreListener(userId) {
  if (unsubscribeFirestore) unsubscribeFirestore();
  
  const colRef = collection(firestore, 'users', userId, 'job_applications');
  
  // Trigger syncing status during initial load so UI shows loading state
  if (isInitialLoad) {
    triggerSyncState('SYNCING');
  }
  
  unsubscribeFirestore = onSnapshot(colRef, (snapshot) => {
    const apps = [];
    snapshot.forEach((doc) => {
      const data = doc.data();
      apps.push({
        ...data,
        id: data.id || doc.id,
        resume: resolveRemoteAttachment(data.resume, userId, 'resumes'),
        coverLetter: resolveRemoteAttachment(data.coverLetter, userId, 'cover_letters'),
        additionalDocument: resolveRemoteAttachment(data.additionalDocument, userId, 'additional_documents'),
        screenshots: (data.screenshots || []).map(scr => resolveRemoteAttachment(scr, userId, 'screenshots'))
      });
    });
    
    // Sort applications by ID sequence
    apps.sort((a, b) => (a.id || 0) - (b.id || 0));
    
    // Update local cache
    localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(apps));
    
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
}

function stopFirestoreListener() {
  if (unsubscribeFirestore) {
    unsubscribeFirestore();
    unsubscribeFirestore = null;
  }
}

// Firebase Auth Observer Integration
onAuthStateChanged(auth, (firebaseUser) => {
  isAuthInitialized = true;
  if (firebaseUser) {
    const formattedUser = {
      uid: firebaseUser.uid,
      displayName: firebaseUser.displayName || 'Google User',
      email: firebaseUser.email,
      photoURL: firebaseUser.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(firebaseUser.displayName || 'User')}&background=2F3A4A&color=fff&bold=true`
    };
    
    localStorage.setItem(KEYS.USER, JSON.stringify(formattedUser));
    currentUserId = firebaseUser.uid;
    
    startFirestoreListener(firebaseUser.uid);
    window.dispatchEvent(new Event('applytrack_auth_change'));
  } else {
    stopFirestoreListener();
    localStorage.removeItem(KEYS.USER);
    localStorage.removeItem(KEYS.APPLICATIONS);
    currentUserId = null;
    isInitialLoad = true;
    
    window.dispatchEvent(new Event('applytrack_auth_change'));
  }
});

export const db = {
  getSyncState() {
    return currentSyncState;
  },
  isAuthReady() {
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
    const apps = this.getApplications();
    return apps.find(a => 
      String(a.id) === String(id) || 
      a.uuid === String(id)
    ) || null;
  },

  async addApplication(appData) {
    const userId = currentUserId;
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const uuid = crypto.randomUUID ? crypto.randomUUID() : 'uuid-' + Math.random().toString(36).substring(2, 11);
      const apps = this.getApplications();
      const nextId = apps.length > 0 ? Math.max(...apps.map(a => a.id)) + 1 : 1;

      // 1. Upload files to Supabase Storage if they contain new binary contents (dataUrl)
      if (appData.resume && appData.resume.dataUrl) {
        await uploadFileToSupabase(userId, 'resumes', appData.resume.fileName, appData.resume.dataUrl);
      }
      if (appData.coverLetter && appData.coverLetter.dataUrl) {
        await uploadFileToSupabase(userId, 'cover_letters', appData.coverLetter.fileName, appData.coverLetter.dataUrl);
      }
      if (appData.additionalDocument && appData.additionalDocument.dataUrl) {
        await uploadFileToSupabase(userId, 'additional_documents', appData.additionalDocument.fileName, appData.additionalDocument.dataUrl);
      }
      if (appData.screenshots && appData.screenshots.length > 0) {
        for (let i = 0; i < appData.screenshots.length; i++) {
          const scr = appData.screenshots[i];
          if (scr.dataUrl) {
            await uploadFileToSupabase(userId, 'screenshots', scr.fileName, scr.dataUrl);
          }
        }
      }

      // 2. Create the Firestore model mapping (strictly metadata filenames, no dataUrl)
      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;

      const serializedApp = {
        id: nextId,
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
    const userId = currentUserId;
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const originalApp = this.getApplicationById(id);
      if (!originalApp) throw new Error('Application not found');

      // 1. Upload new attachments if they have dataUrl and don't match the original file
      if (updatedData.resume && updatedData.resume.dataUrl && (!originalApp.resume || originalApp.resume.fileName !== updatedData.resume.fileName)) {
        await uploadFileToSupabase(userId, 'resumes', updatedData.resume.fileName, updatedData.resume.dataUrl);
      }
      if (updatedData.coverLetter && updatedData.coverLetter.dataUrl && (!originalApp.coverLetter || originalApp.coverLetter.fileName !== updatedData.coverLetter.fileName)) {
        await uploadFileToSupabase(userId, 'cover_letters', updatedData.coverLetter.fileName, updatedData.coverLetter.dataUrl);
      }
      if (updatedData.additionalDocument && updatedData.additionalDocument.dataUrl && (!originalApp.additionalDocument || originalApp.additionalDocument.fileName !== updatedData.additionalDocument.fileName)) {
        await uploadFileToSupabase(userId, 'additional_documents', updatedData.additionalDocument.fileName, updatedData.additionalDocument.dataUrl);
      }
      if (updatedData.screenshots && updatedData.screenshots.length > 0) {
        for (let i = 0; i < updatedData.screenshots.length; i++) {
          const scr = updatedData.screenshots[i];
          if (scr.dataUrl) {
            const hasMatch = originalApp.screenshots && originalApp.screenshots.some(s => s.fileName === scr.fileName);
            if (!hasMatch) {
              await uploadFileToSupabase(userId, 'screenshots', scr.fileName, scr.dataUrl);
            }
          }
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
    const userId = currentUserId;
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const app = this.getApplicationById(id);
      if (!app) throw new Error('Application not found');

      // Save to local cache temporary deleted list for undo
      localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify([app]));

      // Delete the Firestore record
      const userDocRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
      await deleteDoc(userDocRef);

      return true;
    } catch (e) {
      console.error('Delete application failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to delete application');
      throw e;
    }
  },

  async deleteMultipleApplications(ids) {
    const userId = currentUserId;
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const apps = this.getApplications();
      const idStrings = ids.map(String);
      const appsToDelete = apps.filter(a => 
        idStrings.includes(String(a.id)) || 
        idStrings.includes(String(a.uuid))
      );
      if (appsToDelete.length === 0) return false;

      // Save metadata to temp cache for undo
      localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify(appsToDelete));

      // Batch delete from Firestore
      const batch = writeBatch(firestore);
      appsToDelete.forEach(app => {
        const docRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        batch.delete(docRef);
      });
      await batch.commit();

      return true;
    } catch (e) {
      console.error('Multiple delete failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to delete applications');
      throw e;
    }
  },

  async undoDelete() {
    const userId = currentUserId;
    if (!userId) throw new Error('User is not authenticated');

    markLocalWrite();
    try {
      const temp = localStorage.getItem(KEYS.DELETED_TEMP);
      if (!temp) return false;

      const appsToRestore = JSON.parse(temp);
      
      const batch = writeBatch(firestore);
      appsToRestore.forEach(app => {
        // Strip URLs to match pure Firestore format
        const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;
        
        const serialized = {
          ...app,
          resume: cleanAttachment(app.resume),
          coverLetter: cleanAttachment(app.coverLetter),
          additionalDocument: cleanAttachment(app.additionalDocument),
          screenshots: (app.screenshots || []).map(cleanAttachment)
        };
        
        const docRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        batch.set(docRef, serialized);
      });
      await batch.commit();

      localStorage.removeItem(KEYS.DELETED_TEMP);
      return true;
    } catch (e) {
      console.error('Undo delete failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to restore applications');
      throw e;
    }
  },

  async clearUndoCache() {
    const userId = currentUserId;
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
    const userId = currentUserId;
    if (!userId) return;

    markLocalWrite();
    try {
      const apps = this.getApplications();
      
      // 1. Delete all attachments in Supabase Storage
      for (let i = 0; i < apps.length; i++) {
        const app = apps[i];
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

      // 2. Delete all Firestore records
      const batch = writeBatch(firestore);
      apps.forEach(app => {
        const docRef = doc(firestore, 'users', userId, 'job_applications', app.uuid);
        batch.delete(docRef);
      });
      await batch.commit();

      localStorage.removeItem(KEYS.APPLICATIONS);
    } catch (e) {
      console.error('Reset database failed:', e);
      triggerSyncState('ERROR', e.message || 'Failed to wipe database');
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

  importData(jsonString) {
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

    const now = Date.now();
    const oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000;
    const oneMonthAgo = now - 30 * 24 * 60 * 60 * 1000;
    
    const applicationsThisWeek = apps.filter(a => a.createdAt >= oneWeekAgo && a.status !== 'Saved').length;
    const applicationsThisMonth = apps.filter(a => a.createdAt >= oneMonthAgo && a.status !== 'Saved').length;

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
      if (a.status === 'Saved') return;
      const plat = a.platform ? a.platform.trim() : '';
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
      if (a.status === 'Saved') return;
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
  }
};
