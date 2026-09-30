import { describe, it, expect, vi } from 'vitest';

vi.mock('./db', () => ({
  db: {
    getApplications: vi.fn(),
    saveApplications: vi.fn()
  }
}));

vi.mock('./firebase', () => ({
  auth: { currentUser: null },
  firestore: {}
}));

import { areApplicationsContentEqual, getMimeType } from './backup';

describe('backup utility functions', () => {
  describe('getMimeType', () => {
    it('returns application/pdf for .pdf files', () => {
      expect(getMimeType('resume.pdf')).toBe('application/pdf');
      expect(getMimeType('my.document.PDF')).toBe('application/pdf');
    });

    it('returns image/png for .png files', () => {
      expect(getMimeType('screenshot.png')).toBe('image/png');
      expect(getMimeType('IMAGE.PNG')).toBe('image/png');
    });

    it('returns image/jpeg for .jpg and .jpeg files', () => {
      expect(getMimeType('photo.jpg')).toBe('image/jpeg');
      expect(getMimeType('photo.jpeg')).toBe('image/jpeg');
      expect(getMimeType('PHOTO.JPEG')).toBe('image/jpeg');
    });

    it('returns word document mime types for .doc and .docx', () => {
      expect(getMimeType('cv.doc')).toBe('application/msword');
      expect(getMimeType('cv.docx')).toBe(
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      );
    });

    it('returns application/octet-stream for unknown extensions or files without extension', () => {
      expect(getMimeType('data.xyz')).toBe('application/octet-stream');
      expect(getMimeType('README')).toBe('application/octet-stream');
    });
  });

  describe('areApplicationsContentEqual', () => {
    const baseApp = {
      companyName: 'Acme Corp',
      role: 'Staff Engineer',
      platform: 'LinkedIn',
      status: 'Applied',
      jobDescription: 'Great role description',
      notes: 'Applied with referral',
      url: 'https://acme.com/jobs/123',
      email: 'recruiter@acme.com',
      statusHistory: [
        { status: 'Applied', timestamp: 1700000000000 }
      ],
      resume: { fileName: 'resume_1.pdf', originalName: 'MyResume.pdf' },
      coverLetter: { fileName: 'cover_1.pdf', originalName: 'CoverLetter.pdf' },
      additionalDocument: null,
      screenshots: [
        { fileName: 'screen_1.png', originalName: 'JobPost.png' }
      ]
    };

    it('returns true when comparing identical application objects', () => {
      const clone = JSON.parse(JSON.stringify(baseApp));
      expect(areApplicationsContentEqual(baseApp, clone)).toBe(true);
    });

    it('returns false when core fields differ', () => {
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, companyName: 'Other Corp' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, role: 'Junior Engineer' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, platform: 'Indeed' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, status: 'Interview' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, jobDescription: 'Changed' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, notes: 'Changed notes' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, url: 'https://other.com' })).toBe(false);
      expect(areApplicationsContentEqual(baseApp, { ...baseApp, email: 'other@acme.com' })).toBe(false);
    });

    it('handles statusHistory length and item differences', () => {
      const shorterHistory = { ...baseApp, statusHistory: [] };
      expect(areApplicationsContentEqual(baseApp, shorterHistory)).toBe(false);

      const differentStatus = {
        ...baseApp,
        statusHistory: [{ status: 'Interview', timestamp: 1700000000000 }]
      };
      expect(areApplicationsContentEqual(baseApp, differentStatus)).toBe(false);

      const differentTimestamp = {
        ...baseApp,
        statusHistory: [{ status: 'Applied', timestamp: 1700009999999 }]
      };
      expect(areApplicationsContentEqual(baseApp, differentTimestamp)).toBe(false);
    });

    it('handles attachment equality and mismatches', () => {
      // Resume mismatch
      const diffResume = {
        ...baseApp,
        resume: { fileName: 'resume_2.pdf', originalName: 'MyResume.pdf' }
      };
      expect(areApplicationsContentEqual(baseApp, diffResume)).toBe(false);

      // Missing cover letter
      const noCoverLetter = { ...baseApp, coverLetter: null };
      expect(areApplicationsContentEqual(baseApp, noCoverLetter)).toBe(false);

      // Additional document mismatch
      const withExtraDoc = {
        ...baseApp,
        additionalDocument: { fileName: 'extra.pdf', originalName: 'Cert.pdf' }
      };
      expect(areApplicationsContentEqual(baseApp, withExtraDoc)).toBe(false);

      // Screenshots length mismatch
      const noScreenshots = { ...baseApp, screenshots: [] };
      expect(areApplicationsContentEqual(baseApp, noScreenshots)).toBe(false);

      // Screenshots content mismatch
      const diffScreenshots = {
        ...baseApp,
        screenshots: [{ fileName: 'screen_2.png', originalName: 'JobPost.png' }]
      };
      expect(areApplicationsContentEqual(baseApp, diffScreenshots)).toBe(false);
    });

    it('handles null and undefined statusHistory or screenshots gracefully', () => {
      const app1 = { ...baseApp, statusHistory: null, screenshots: null };
      const app2 = { ...baseApp, statusHistory: undefined, screenshots: undefined };
      expect(areApplicationsContentEqual(app1, app2)).toBe(true);
    });
  });
});
