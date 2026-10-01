import { describe, it, expect } from 'vitest';

describe('Job Analytics & Funnel Calculations', () => {
  const sampleApplications = [
    {
      id: 'app-1',
      companyName: 'Google',
      jobTitle: 'Frontend Engineer',
      platform: 'LinkedIn',
      status: 'Interview',
      notes: 'Passed initial phone screen'
    },
    {
      id: 'app-2',
      companyName: 'Stripe',
      jobTitle: 'React Developer',
      platform: 'LinkedIn',
      status: 'Offer',
      notes: 'Offer letter received'
    },
    {
      id: 'app-3',
      companyName: 'Amazon',
      jobTitle: 'Software Development Engineer',
      platform: 'Indeed',
      status: 'Rejected',
      notes: 'Applied on website'
    },
    {
      id: 'app-4',
      companyName: 'Microsoft',
      jobTitle: 'Full Stack Engineer',
      platform: 'Company Website',
      status: 'Applied',
      notes: 'Referral submitted'
    },
    {
      id: 'app-5',
      companyName: 'Meta',
      jobTitle: 'UI Engineer',
      platform: 'LinkedIn',
      status: 'Saved',
      notes: 'Need to review job description'
    }
  ];

  it('calculates status distribution accurately', () => {
    const distribution = sampleApplications.reduce((acc, job) => {
      acc[job.status] = (acc[job.status] || 0) + 1;
      return acc;
    }, {});

    expect(distribution['Interview']).toBe(1);
    expect(distribution['Offer']).toBe(1);
    expect(distribution['Rejected']).toBe(1);
    expect(distribution['Applied']).toBe(1);
    expect(distribution['Saved']).toBe(1);
  });

  it('computes callback and conversion rates correctly', () => {
    const activeJobs = sampleApplications.filter((j) => j.status !== 'Saved');
    const totalActive = activeJobs.length; // 4
    const interviewsOrOffers = activeJobs.filter(
      (j) => j.status === 'Interview' || j.status === 'Offer'
    ).length; // 2
    const offers = activeJobs.filter((j) => j.status === 'Offer').length; // 1

    const callbackRate = totalActive > 0 ? (interviewsOrOffers / totalActive) * 100 : 0;
    const offerConversionRate = interviewsOrOffers > 0 ? (offers / interviewsOrOffers) * 100 : 0;

    expect(callbackRate).toBe(50);
    expect(offerConversionRate).toBe(50);
  });

  it('handles empty job lists safely without NaN or division by zero', () => {
    const total = 0;
    const count = 0;
    const rate = total > 0 ? (count / total) * 100 : 0;

    expect(rate).toBe(0);
    expect(Number.isNaN(rate)).toBe(false);
  });

  it('filters by search term case-insensitively', () => {
    const query = 'stripe';
    const matches = sampleApplications.filter((job) =>
      job.companyName.toLowerCase().includes(query.toLowerCase()) ||
      job.jobTitle.toLowerCase().includes(query.toLowerCase())
    );

    expect(matches).toHaveLength(1);
    expect(matches[0].companyName).toBe('Stripe');
  });

  it('calculates platform performance ranking', () => {
    const activeJobs = sampleApplications.filter((j) => j.status !== 'Saved');
    const platformBreakdown = activeJobs.reduce((acc, job) => {
      if (!acc[job.platform]) {
        acc[job.platform] = { total: 0, responses: 0 };
      }
      acc[job.platform].total += 1;
      if (job.status === 'Interview' || job.status === 'Offer') {
        acc[job.platform].responses += 1;
      }
      return acc;
    }, {});

    expect(platformBreakdown['LinkedIn'].total).toBe(2);
    expect(platformBreakdown['LinkedIn'].responses).toBe(2);
    expect((platformBreakdown['LinkedIn'].responses / platformBreakdown['LinkedIn'].total) * 100).toBe(100);

    expect(platformBreakdown['Indeed'].total).toBe(1);
    expect(platformBreakdown['Indeed'].responses).toBe(0);
  });
});
