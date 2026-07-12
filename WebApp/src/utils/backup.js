import { zip, unzip, strToU8, strFromU8 } from 'fflate';
import { db } from './db';
import { auth, firestore } from './firebase';
import { doc, setDoc } from 'firebase/firestore';

// Helper: Convert base64 data URL to Uint8Array (fallback)
function dataUrlToUint8Array(dataUrl) {
  if (!dataUrl) return new Uint8Array(0);
  const parts = dataUrl.split(',');
  if (parts.length < 2) return new Uint8Array(0);
  const base64 = parts[1];
  const binaryString = atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

// Helper: Convert Uint8Array to base64 data URL (fallback)
function uint8ArrayToDataUrl(bytes, mimeType) {
  let binary = '';
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  const base64 = btoa(binary);
  return `data:${mimeType};base64,${base64}`;
}

// Helper: Get MIME type from filename
function getMimeType(filename) {
  const ext = filename.split('.').pop().toLowerCase();
  if (ext === 'pdf') return 'application/pdf';
  if (ext === 'png') return 'image/png';
  if (ext === 'jpg' || ext === 'jpeg') return 'image/jpeg';
  if (ext === 'doc') return 'application/msword';
  if (ext === 'docx') return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  return 'application/octet-stream';
}

// Helper: Check if two application objects are identical in content
export function areApplicationsContentEqual(appA, appB) {
  if (appA.companyName !== appB.companyName) return false;
  if (appA.role !== appB.role) return false;
  if (appA.platform !== appB.platform) return false;
  if (appA.status !== appB.status) return false;
  if (appA.jobDescription !== appB.jobDescription) return false;
  if (appA.notes !== appB.notes) return false;
  if (appA.url !== appB.url) return false;
  if (appA.email !== appB.email) return false;

  // Compare statusHistory
  const histA = appA.statusHistory || [];
  const histB = appB.statusHistory || [];
  if (histA.length !== histB.length) return false;
  for (let i = 0; i < histA.length; i++) {
    if (histA[i].status !== histB[i].status) return false;
    if (histA[i].timestamp !== histB[i].timestamp) return false;
  }

  // Compare attachments (only metadata: fileName and originalName)
  const compareAttach = (attA, attB) => {
    if (!attA && !attB) return true;
    if (!attA || !attB) return false;
    return attA.fileName === attB.fileName && attA.originalName === attB.originalName;
  };

  if (!compareAttach(appA.resume, appB.resume)) return false;
  if (!compareAttach(appA.coverLetter, appB.coverLetter)) return false;
  if (!compareAttach(appA.additionalDocument, appB.additionalDocument)) return false;

  const screensA = appA.screenshots || [];
  const screensB = appB.screenshots || [];
  if (screensA.length !== screensB.length) return false;
  for (let i = 0; i < screensA.length; i++) {
    if (!compareAttach(screensA[i], screensB[i])) return false;
  }

  return true;
}

// Export function (Downloads attachment binaries from Supabase Storage and archives them into the ZIP)
export async function exportBackupToZip(apps, onSuccess, onError) {
  const userId = auth.currentUser ? auth.currentUser.uid : null;
  if (!userId) {
    onError("User is not authenticated");
    return;
  }

  try {
    const files = {};

    // 1. Serialize Applications JSON without dataUrl or url
    const serializedApps = apps.map(app => {
      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;
      return {
        uuid: app.uuid,
        companyName: app.companyName,
        role: app.role,
        platform: app.platform,
        status: app.status,
        jobDescription: app.jobDescription,
        notes: app.notes,
        url: app.url,
        email: app.email,
        createdAt: app.createdAt,
        updatedAt: app.updatedAt,
        statusHistory: app.statusHistory || [],
        resume: cleanAttachment(app.resume),
        coverLetter: cleanAttachment(app.coverLetter),
        additionalDocument: cleanAttachment(app.additionalDocument),
        screenshots: (app.screenshots || []).map(cleanAttachment)
      };
    });

    files['data.json'] = strToU8(JSON.stringify(serializedApps, null, 2));

    // 2. Build list of attachments to fetch from Supabase
    const attachmentsToFetch = [];
    apps.forEach(app => {
      const items = [
        { att: app.resume, type: 'resumes' },
        { att: app.coverLetter, type: 'cover_letters' },
        { att: app.additionalDocument, type: 'additional_documents' },
        ...(app.screenshots || []).map(scr => ({ att: scr, type: 'screenshots' }))
      ].filter(item => item.att && item.att.fileName && item.att.url);
      
      attachmentsToFetch.push(...items);
    });

    // 3. Fetch all binaries in parallel from Supabase Storage
    await Promise.all(attachmentsToFetch.map(async ({ att }) => {
      try {
        const response = await fetch(att.url);
        if (response.ok) {
          const arrayBuffer = await response.arrayBuffer();
          files[att.fileName] = new Uint8Array(arrayBuffer);
        }
      } catch (e) {
        console.error(`Failed to fetch attachment file ${att.fileName}:`, e);
      }
    }));

    // 4. Compress ZIP asynchronously
    zip(files, async (err, zipBytes) => {
      if (err) {
        onError(err.message || err);
        return;
      }

      // 5. Trigger download/save of ZIP archive
      const blob = new Blob([zipBytes], { type: 'application/zip' });
      const dateStr = new Date().toISOString().slice(0, 10);
      const defaultFilename = `applytrack_backup_${dateStr}.zip`;

      const triggerTraditionalDownload = () => {
        const downloadUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = defaultFilename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(downloadUrl);
        onSuccess();
      };

      if ('showSaveFilePicker' in window) {
        try {
          const handle = await window.showSaveFilePicker({
            suggestedName: defaultFilename,
            types: [{
              description: 'ZIP Archive',
              accept: {
                'application/zip': ['.zip']
              }
            }]
          });
          const writable = await handle.createWritable();
          await writable.write(blob);
          await writable.close();
          onSuccess();
        } catch (err) {
          if (err.name === 'AbortError') {
            onError("Save cancelled by user");
          } else {
            console.error("Save picker failed, falling back:", err);
            triggerTraditionalDownload();
          }
        }
      } else {
        triggerTraditionalDownload();
      }
    });
  } catch (e) {
    onError(e.message || e);
  }
}

// Conflict checking function
export function checkBackupConflicts(file, onResult, onError) {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const zipBytes = new Uint8Array(e.target.result);
      unzip(zipBytes, (err, unzipped) => {
        if (err) {
          onError("Invalid ZIP file or compression format.");
          return;
        }

        const dataBytes = unzipped['data.json'];
        if (!dataBytes) {
          onError("Missing data.json file inside the backup ZIP.");
          return;
        }

        const jsonStr = strFromU8(dataBytes);
        const importedApps = JSON.parse(jsonStr);
        if (!Array.isArray(importedApps)) {
          onError("Invalid data.json structure in backup.");
          return;
        }

        // Count conflicts against local database cache
        const existingApps = db.getApplications();
        let conflictsCount = 0;

        importedApps.forEach(importedApp => {
          const match = existingApps.find(a => a.uuid === importedApp.uuid);
          if (match) {
            const equal = areApplicationsContentEqual(match, importedApp);
            if (!equal) {
              conflictsCount++;
            }
          }
        });

        onResult(conflictsCount, importedApps, unzipped);
      });
    } catch (err) {
      onError(err.message || err);
    }
  };
  reader.onerror = () => onError("Could not read backup file.");
  reader.readAsArrayBuffer(file);
}

// Import execution function (Uploads unzipped files to Supabase and writes metadata to Firestore)
export async function importBackup(importedApps, unzipped, overwrite, onProgress, onSuccess, onError) {
  const userId = auth.currentUser ? auth.currentUser.uid : null;
  if (!userId) {
    onError("User is not authenticated");
    return;
  }

  try {
    onProgress("Importing applications and uploading attachments...");
    
    const existingApps = db.getApplications();
    let importedCount = 0;
    let updatedCount = 0;
    let ignoredCount = 0;

    const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;

    // Helper: Upload file bytes to Supabase Storage
    async function uploadBytesToSupabase(type, fileName, bytes) {
      const uploadUrl = `${supabaseUrl}/storage/v1/object/ApplyTrack/users/${userId}/${type}/${fileName}`;
      
      // Perform HEAD check to skip if already uploaded
      try {
        const checkRes = await fetch(uploadUrl, {
          method: 'HEAD',
          headers: {
            'Authorization': `Bearer ${anonKey}`,
            'apikey': anonKey
          }
        });
        if (checkRes.ok) return true;
      } catch (e) {}

      const mimeType = getMimeType(fileName);
      const blob = new Blob([bytes], { type: mimeType });
      
      const res = await fetch(uploadUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${anonKey}`,
          'apikey': anonKey,
          'Content-Type': mimeType
        },
        body: blob
      });
      return res.ok;
    }

    // Process imported applications one by one
    for (let i = 0; i < importedApps.length; i++) {
      const importedApp = importedApps[i];
      const matchIndex = existingApps.findIndex(a => a.uuid === importedApp.uuid);
      const match = matchIndex !== -1 ? existingApps[matchIndex] : null;

      const isIdentical = match ? areApplicationsContentEqual(match, importedApp) : false;

      if (match && isIdentical) {
        ignoredCount++;
        continue;
      }

      if (match && !overwrite) {
        ignoredCount++;
        continue;
      }

      onProgress(`Processing ${importedApp.companyName || 'Application'}...`);

      // 1. Upload files from unzipped ZIP bytes if present
      const attachmentsToUpload = [
        { att: importedApp.resume, type: 'resumes' },
        { att: importedApp.coverLetter, type: 'cover_letters' },
        { att: importedApp.additionalDocument, type: 'additional_documents' },
        ...(importedApp.screenshots || []).map(scr => ({ att: scr, type: 'screenshots' }))
      ].filter(item => item.att && item.att.fileName);

      for (let j = 0; j < attachmentsToUpload.length; j++) {
        const { att, type } = attachmentsToUpload[j];
        const bytes = unzipped[att.fileName];
        if (bytes) {
          await uploadBytesToSupabase(type, att.fileName, bytes);
        }
      }

      // 2. Prepare cleaned Firestore metadata object
      const cleanAttachment = (att) => att ? { fileName: att.fileName, originalName: att.originalName } : null;

      const serializedApp = {
        id: match ? match.id : (existingApps.length > 0 ? Math.max(...existingApps.map(a => a.id)) + 1 : 1),
        uuid: importedApp.uuid,
        companyName: importedApp.companyName || null,
        role: importedApp.role || null,
        platform: importedApp.platform || 'Direct',
        status: importedApp.status || 'Applied',
        jobDescription: importedApp.jobDescription || '',
        notes: importedApp.notes || '',
        url: importedApp.url || '',
        email: importedApp.email || '',
        createdAt: importedApp.createdAt || Date.now(),
        updatedAt: Date.now(),
        statusHistory: importedApp.statusHistory || [
          { status: importedApp.status || 'Applied', timestamp: importedApp.createdAt || Date.now() }
        ],
        resume: cleanAttachment(importedApp.resume),
        coverLetter: cleanAttachment(importedApp.coverLetter),
        additionalDocument: cleanAttachment(importedApp.additionalDocument),
        screenshots: (importedApp.screenshots || []).map(cleanAttachment)
      };

      // 3. Save directly to Firestore
      const userDocRef = doc(firestore, 'users', userId, 'job_applications', importedApp.uuid);
      await setDoc(userDocRef, serializedApp);

      if (match) {
        updatedCount++;
      } else {
        importedCount++;
      }
    }

    onSuccess(importedCount, updatedCount, ignoredCount);
  } catch (err) {
    console.error('Import backup failed:', err);
    onError(err.message || err);
  }
}
