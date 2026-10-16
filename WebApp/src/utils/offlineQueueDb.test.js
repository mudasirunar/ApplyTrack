import { describe, it, expect } from 'vitest';
import { stageOfflineAttachments, getStagedOfflineAttachments, clearStagedOfflineAttachments } from './offlineQueueDb';

describe('offlineQueueDb', () => {
  it('gracefully handles missing parameters', async () => {
    expect(await stageOfflineAttachments(null, null)).toBe(false);
    expect(await getStagedOfflineAttachments(null)).toBe(null);
    expect(await clearStagedOfflineAttachments(null)).toBe(false);
  });

  it('safely handles non-browser / no-indexedDB environments without throwing', async () => {
    expect(await stageOfflineAttachments('test-uuid', { resume: { fileName: 'test.pdf' } })).toBe(false);
    expect(await getStagedOfflineAttachments('test-uuid')).toBe(null);
    expect(await clearStagedOfflineAttachments('test-uuid')).toBe(false);
  });
});
