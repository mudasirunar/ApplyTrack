import { describe, it, expect } from 'vitest';
import { mergeApplicationFields } from './db';

describe('mergeApplicationFields', () => {
  it('merges non-colliding fields from both devices', () => {
    const localApp = {
      uuid: 'job-1',
      companyName: 'Acme Corp',
      role: 'Senior Full Stack Engineer', // Changed offline on Device A
      updatedAt: 1000
    };

    const remoteData = {
      uuid: 'job-1',
      companyName: 'Acme Corp',
      notes: 'Interview scheduled with VP of Engineering', // Changed online on Device B
      updatedAt: 1100
    };

    const result = mergeApplicationFields(localApp, remoteData);
    expect(result.role).toBe('Senior Full Stack Engineer');
    expect(result.notes).toBe('Interview scheduled with VP of Engineering');
    expect(result.companyName).toBe('Acme Corp');
  });

  it('resolves colliding same fields using timestamp-based Last-Write-Wins (LWW)', () => {
    // Case 1: Local is newer
    const localNewer = {
      uuid: 'job-2',
      role: 'Staff Engineer',
      updatedAt: 2000
    };
    const remoteOlder = {
      uuid: 'job-2',
      role: 'Senior Engineer',
      updatedAt: 1000
    };
    expect(mergeApplicationFields(localNewer, remoteOlder).role).toBe('Staff Engineer');

    // Case 2: Remote is newer
    const localOlder = {
      uuid: 'job-2',
      status: 'Applied',
      updatedAt: 1000
    };
    const remoteNewer = {
      uuid: 'job-2',
      status: 'Interviewing',
      updatedAt: 2000
    };
    expect(mergeApplicationFields(localOlder, remoteNewer).status).toBe('Interviewing');
  });

  it('unions statusHistory milestones chronologically without losing either device history', () => {
    const localApp = {
      uuid: 'job-3',
      status: 'Interviewing',
      updatedAt: 3000,
      statusHistory: [
        { status: 'Applied', timestamp: 1000 },
        { status: 'Interviewing', timestamp: 3000 }
      ]
    };

    const remoteData = {
      uuid: 'job-3',
      status: 'Screening',
      updatedAt: 2000,
      statusHistory: [
        { status: 'Applied', timestamp: 1000 },
        { status: 'Screening', timestamp: 2000 }
      ]
    };

    const result = mergeApplicationFields(localApp, remoteData);
    expect(result.statusHistory).toHaveLength(3);
    expect(result.statusHistory.map(h => h.status)).toEqual(['Applied', 'Screening', 'Interviewing']);
    expect(result.status).toBe('Interviewing'); // local is newer (3000 vs 2000)
  });

  it('preserves attachments from both devices without overwriting', () => {
    const localApp = {
      uuid: 'job-4',
      resume: { fileName: 'resume_v2.pdf', originalName: 'resume.pdf' },
      updatedAt: 2000
    };

    const remoteData = {
      uuid: 'job-4',
      coverLetter: { fileName: 'cover_letter.pdf', originalName: 'cover_letter.pdf' },
      updatedAt: 1000
    };

    const result = mergeApplicationFields(localApp, remoteData);
    expect(result.resume.fileName).toBe('resume_v2.pdf');
    expect(result.coverLetter.fileName).toBe('cover_letter.pdf');
  });

  it('unions screenshots by fileName from both devices', () => {
    const localApp = {
      uuid: 'job-5',
      screenshots: [{ fileName: 'scr1.png' }],
      updatedAt: 2000
    };

    const remoteData = {
      uuid: 'job-5',
      screenshots: [{ fileName: 'scr2.png' }],
      updatedAt: 1000
    };

    const result = mergeApplicationFields(localApp, remoteData);
    expect(result.screenshots).toHaveLength(2);
    expect(result.screenshots.map(s => s.fileName)).toEqual(expect.arrayContaining(['scr1.png', 'scr2.png']));
  });
});
