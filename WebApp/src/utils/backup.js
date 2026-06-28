import { zip, unzip, strToU8, strFromU8 } from 'fflate';
import { db } from './db';

// Helper: Convert base64 data URL to Uint8Array
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

// Helper: Convert Uint8Array to base64 data URL
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

// Helper: Strip dataUrls from attachment before serializing to JSON
function serializeAttachment(att) {
  if (!att) return null;
  return {
    fileName: att.fileName,
    originalName: att.originalName
  };
}

// Export function
export function exportBackupToZip(apps, onSuccess, onError) {
  try {
    const files = {};

    // 1. Serialize Applications JSON without dataUrl
    const serializedApps = apps.map(app => {
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
        resume: serializeAttachment(app.resume),
        coverLetter: serializeAttachment(app.coverLetter),
        additionalDocument: serializeAttachment(app.additionalDocument),
        screenshots: (app.screenshots || []).map(serializeAttachment)
      };
    });

    files['data.json'] = strToU8(JSON.stringify(serializedApps, null, 2));

    // 2. Add raw attachments files to ZIP root
    apps.forEach(app => {
      const attachments = [
        app.resume,
        app.coverLetter,
        app.additionalDocument,
        ...(app.screenshots || [])
      ].filter(Boolean);

      attachments.forEach(att => {
        if (att.fileName && att.dataUrl) {
          const bytes = dataUrlToUint8Array(att.dataUrl);
          files[att.fileName] = bytes;
        }
      });
    });

    // 3. Compress ZIP asynchronously
    zip(files, (err, zipBytes) => {
      if (err) {
        onError(err.message || err);
        return;
      }

      // 4. Download file
      const blob = new Blob([zipBytes], { type: 'application/zip' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const dateStr = new Date().toISOString().slice(0, 10);
      a.download = `applytrack_backup_${dateStr}.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      onSuccess();
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

        // Count conflicts
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

// Import execution function
export function importBackup(importedApps, unzipped, overwrite, onProgress, onSuccess, onError) {
  try {
    onProgress("Restoring records...");
    const localApps = db.getApplications();
    let importedCount = 0;
    let updatedCount = 0;
    let ignoredCount = 0;

    // Helper: restore dataUrl from unzip
    const restoreAttachment = (att, localMatchAtt) => {
      if (!att) return null;
      // Look up in ZIP
      const fileBytes = unzipped[att.fileName];
      if (fileBytes) {
        const mimeType = getMimeType(att.fileName);
        return {
          ...att,
          dataUrl: uint8ArrayToDataUrl(fileBytes, mimeType)
        };
      }
      // Fallback to local copy if files match and local copy has dataUrl
      if (localMatchAtt && localMatchAtt.fileName === att.fileName && localMatchAtt.dataUrl) {
        return {
          ...att,
          dataUrl: localMatchAtt.dataUrl
        };
      }
      return {
        ...att,
        dataUrl: ''
      };
    };

    importedApps.forEach(importedApp => {
      const matchIndex = localApps.findIndex(a => a.uuid === importedApp.uuid);
      const match = matchIndex !== -1 ? localApps[matchIndex] : null;

      // Prepare attachment dataUrls
      const resume = restoreAttachment(importedApp.resume, match?.resume);
      const coverLetter = restoreAttachment(importedApp.coverLetter, match?.coverLetter);
      const additionalDocument = restoreAttachment(importedApp.additionalDocument, match?.additionalDocument);
      const screenshots = (importedApp.screenshots || []).map((scr, idx) => {
        const localMatchScr = match?.screenshots?.[idx];
        return restoreAttachment(scr, localMatchScr);
      });

      const processedApp = {
        ...importedApp,
        resume,
        coverLetter,
        additionalDocument,
        screenshots
      };

      if (!match) {
        // New application record -> calculate ID and push
        const nextId = localApps.length > 0 ? Math.max(...localApps.map(a => a.id)) + 1 : 1;
        localApps.push({
          ...processedApp,
          id: nextId,
          updatedAt: Date.now()
        });
        importedCount++;
      } else {
        // Match exists by UUID -> check duplicate content
        const isIdentical = areApplicationsContentEqual(match, processedApp);
        if (isIdentical) {
          ignoredCount++;
        } else {
          // Conflict
          if (overwrite) {
            // Overwrite and keep the same local ID
            localApps[matchIndex] = {
              ...processedApp,
              id: match.id,
              updatedAt: Date.now()
            };
            updatedCount++;
          } else {
            // Ignore / Keep current version
            ignoredCount++;
          }
        }
      }
    });

    // Save applications list and trigger data changes
    db.saveApplications(localApps);
    onSuccess(importedCount, updatedCount, ignoredCount);
  } catch (err) {
    onError(err.message || err);
  }
}
