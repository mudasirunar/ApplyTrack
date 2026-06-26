import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { AppIcon } from '../components/Icons';

export default function Dashboard({ setActiveTab, setFilters }) {
  const [analytics, setAnalytics] = useState(db.getAnalytics());
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear().toString());
  const [user, setUser] = useState(db.getCurrentUser());

  useEffect(() => {
    const handleDataChange = () => {
      setAnalytics(db.getAnalytics());
    };
    window.addEventListener('applytrack_data_change', handleDataChange);
    return () => {
      window.removeEventListener('applytrack_data_change', handleDataChange);
    };
  }, []);

  const handleNavigateToApps = (filterType, value) => {
    // Reset all filters first, then apply the selected one
    const newFilters = {
      searchQuery: '',
      statusFilter: 'All',
      selectedResume: 'All',
      selectedPlatform: 'All',
      dateFilterMode: 'All',
      dateMonth: '',
      dateYear: ''
    };

    if (filterType === 'status') {
      newFilters.statusFilter = value;
    } else if (filterType === 'platform') {
      newFilters.statusFilter = 'Platform';
      newFilters.selectedPlatform = value;
    } else if (filterType === 'resume') {
      newFilters.statusFilter = 'Resume';
      newFilters.selectedResume = value;
    } else if (filterType === 'month') {
      newFilters.statusFilter = 'Date';
      newFilters.dateFilterMode = 'Month';
      newFilters.dateMonth = value.month;
      newFilters.dateYear = value.year;
    }

    setFilters(newFilters);
    setActiveTab('applications');
  };

  // Funnel calculations
  const totalActive = analytics.total - analytics.saved;
  const interviewRate = totalActive > 0 ? Math.round((analytics.interviews / totalActive) * 100) : 0;
  const offerRate = analytics.interviews > 0 ? Math.round((analytics.offers / analytics.interviews) * 100) : 0;
  const overallYield = totalActive > 0 ? Math.round((analytics.offers / totalActive) * 100) : 0;

  // Monthly Chart Math
  const availableYears = Object.keys(analytics.monthlyActivity || {}).sort((a, b) => b - a);
  const chartYear = selectedYear || availableYears[0] || new Date().getFullYear().toString();
  const monthsList = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthlyDataForYear = analytics.monthlyActivity[chartYear] || {};
  const maxApplicationsInMonth = Math.max(...monthsList.map(m => monthlyDataForYear[m] || 0), 1);

  // SVG Donut Chart segment calculations
  const renderDonutChart = () => {
    const slices = analytics.statusDistribution || [];
    if (slices.length === 0) return null;

    let accumulatedPercentage = 0;
    return (
      <svg viewBox="0 0 36 36" className="donut-chart-svg">
        {/* Background circle */}
        <circle cx="18" cy="18" r="15.915" fill="none" stroke="var(--bg-surface-variant)" strokeWidth="3" />
        
        {slices.map((slice, idx) => {
          const percent = (slice.count / analytics.total) * 100;
          const strokeDash = `${percent} ${100 - percent}`;
          const strokeOffset = 100 - accumulatedPercentage + 25; // start at top (12 o'clock)
          accumulatedPercentage += percent;

          return (
            <circle
              key={idx}
              cx="18"
              cy="18"
              r="15.915"
              fill="none"
              stroke={slice.color}
              strokeWidth="3"
              strokeDasharray={strokeDash}
              strokeDashoffset={strokeOffset}
            />
          );
        })}
      </svg>
    );
  };

  return (
    <div className="content-container animate-fade-in">
      {/* Dashboard Header */}
      <div className="dashboard-header">
        <h2 className="dashboard-greeting">Hello, {user?.displayName || 'Developer'}</h2>
        <p className="dashboard-subtext">
          {analytics.total > 0 
            ? `You have tracked ${analytics.total} job applications so far. Keep pushing!` 
            : 'Get started by tracking your very first job application.'}
        </p>
      </div>

      {/* Overview Stats Card */}
      <div 
        className="overview-card card-base card-interactive"
        onClick={() => handleNavigateToApps('status', 'All')}
      >
        <div className="overview-top">
          <div>
            <span className="overview-title">Total Applications</span>
            <div className="overview-count">{analytics.total}</div>
          </div>
          <div className="overview-badges">
            <div className="activity-badge">
              <span>This Week</span>
              <span className="activity-badge-count">{analytics.applicationsThisWeek}</span>
            </div>
            <div className="activity-badge">
              <span>This Month</span>
              <span className="activity-badge-count">{analytics.applicationsThisMonth}</span>
            </div>
          </div>
        </div>

        {analytics.total === 0 && (
          <div className="overview-empty-action">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setActiveTab('add-job');
              }} 
              className="btn-primary"
            >
              Add Your First Application
            </button>
          </div>
        )}
      </div>

      {/* Status Cards Grid */}
      <div className="status-cards-grid">
        <div 
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'var(--warning-amber-tint)' }}
          onClick={() => handleNavigateToApps('status', 'Applied')}
        >
          <span className="status-card-header" style={{ color: 'var(--warning-amber)' }}>Applied</span>
          <span className="status-card-count" style={{ color: 'var(--warning-amber)' }}>{analytics.applied}</span>
        </div>
        <div 
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'var(--saved-gray-tint)' }}
          onClick={() => handleNavigateToApps('status', 'Saved')}
        >
          <span className="status-card-header" style={{ color: 'var(--saved-gray)' }}>Saved</span>
          <span className="status-card-count" style={{ color: 'var(--saved-gray)' }}>{analytics.saved}</span>
        </div>
        <div 
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'var(--accent-green-tint)' }}
          onClick={() => handleNavigateToApps('status', 'Interview')}
        >
          <span className="status-card-header" style={{ color: 'var(--accent-green)' }}>Interviews</span>
          <span className="status-card-count" style={{ color: 'var(--accent-green)' }}>{analytics.interviews}</span>
        </div>
        <div 
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'var(--link-blue-tint)' }}
          onClick={() => handleNavigateToApps('status', 'Offer')}
        >
          <span className="status-card-header" style={{ color: 'var(--link-blue)' }}>Offers</span>
          <span className="status-card-count" style={{ color: 'var(--link-blue)' }}>{analytics.offers}</span>
        </div>
        <div 
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'var(--error-red-tint)' }}
          onClick={() => handleNavigateToApps('status', 'Rejected')}
        >
          <span className="status-card-header" style={{ color: 'var(--error-red)' }}>Rejected</span>
          <span className="status-card-count" style={{ color: 'var(--error-red)' }}>{analytics.rejected}</span>
        </div>
        <div 
          className="status-stat-card card-base"
          style={{ backgroundColor: 'rgba(var(--brand-primary), 0.05)' }}
        >
          <span className="status-card-header">Response Rate</span>
          <span className="status-card-count" style={{ color: 'var(--brand-primary)' }}>{analytics.responses}%</span>
        </div>
      </div>

      {/* Conversion Funnel Row */}
      {analytics.total > 0 && (
        <div className="funnel-card card-base">
          <h3 className="section-title">Conversion Funnel</h3>
          <div className="funnel-container">
            <div className="funnel-stage">
              <span className="funnel-stage-label">Applications</span>
              <div className="funnel-bar-wrapper">
                <div className="funnel-bar-fill" style={{ width: '100%', backgroundColor: 'var(--brand-primary)' }}>
                  <span className="funnel-bar-text">{totalActive} Sent</span>
                </div>
              </div>
              <span className="funnel-stage-percent">100%</span>
            </div>
            
            <div className="funnel-stage">
              <span className="funnel-stage-label">Interviews</span>
              <div className="funnel-bar-wrapper">
                <div className="funnel-bar-fill" style={{ width: `${interviewRate}%`, backgroundColor: 'var(--accent-green)' }}>
                  {analytics.interviews > 0 && <span className="funnel-bar-text">{analytics.interviews} Met</span>}
                </div>
              </div>
              <span className="funnel-stage-percent">{interviewRate}%</span>
            </div>
            
            <div className="funnel-stage">
              <span className="funnel-stage-label">Offers</span>
              <div className="funnel-bar-wrapper">
                <div className="funnel-bar-fill" style={{ width: `${overallYield}%`, backgroundColor: 'var(--link-blue)' }}>
                  {analytics.offers > 0 && <span className="funnel-bar-text">{analytics.offers} Recd</span>}
                </div>
              </div>
              <span className="funnel-stage-percent">{overallYield}%</span>
            </div>
          </div>
        </div>
      )}

      {/* Double Column Section (Charts & platform list) */}
      {analytics.total > 0 && (
        <div className="dashboard-grid-2">
          {/* Status Distribution */}
          <div className="card-base donut-chart-container">
            <h3 className="section-title" style={{ alignSelf: 'flex-start' }}>Status Distribution</h3>
            <div style={{ position: 'relative' }}>
              {renderDonutChart()}
              <div className="donut-chart-center-text">
                <span className="donut-center-num">{analytics.total}</span>
                <span className="donut-center-label">Total</span>
              </div>
            </div>
            <div className="donut-legend">
              {analytics.statusDistribution.map((slice, idx) => (
                <div 
                  key={idx} 
                  className="legend-item"
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleNavigateToApps('status', slice.name === 'Offers' ? 'Offer' : slice.name)}
                >
                  <span className="legend-color" style={{ backgroundColor: slice.color }}></span>
                  <span style={{ color: 'var(--text-secondary)' }}>{slice.name} ({slice.count})</span>
                </div>
              ))}
            </div>
          </div>

          {/* Monthly Activity Section */}
          <div className="card-base" style={{ padding: '24px' }}>
            <div className="chart-header">
              <h3 className="section-title" style={{ margin: 0 }}>Monthly Activity</h3>
              {availableYears.length > 1 && (
                <select 
                  className="chart-select"
                  value={chartYear}
                  onChange={(e) => setSelectedYear(e.target.value)}
                >
                  {availableYears.map(y => (
                    <option key={y} value={y}>{y}</option>
                  ))}
                </select>
              )}
            </div>
            <div className="activity-chart-wrapper">
              {monthsList.map((month) => {
                const count = monthlyDataForYear[month] || 0;
                const percentHeight = (count / maxApplicationsInMonth) * 100;
                return (
                  <div key={month} className="chart-bar-column">
                    <div 
                      className="chart-bar" 
                      style={{ height: `${percentHeight}%` }}
                      onClick={() => count > 0 && handleNavigateToApps('month', { month, year: Number(chartYear) })}
                    >
                      <span className="chart-bar-tooltip">{count} applied</span>
                    </div>
                    <span className="chart-label">{month}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Double Column Section (Platform & Resume effectiveness) */}
      {analytics.total > 0 && (
        <div className="dashboard-grid-2">
          {/* Platform Breakdown */}
          <div className="card-base" style={{ padding: '24px 0' }}>
            <h3 className="section-title" style={{ padding: '0 24px', marginBottom: '8px' }}>Platform Breakdown</h3>
            <div className="platform-list">
              {analytics.platforms.length > 0 ? (
                analytics.platforms.map((p, idx) => {
                  const maxCount = Math.max(...analytics.platforms.map(item => item.count), 1);
                  const barPercent = (p.count / maxCount) * 100;
                  return (
                    <div 
                      key={idx} 
                      className="platform-row"
                      onClick={() => handleNavigateToApps('platform', p.name)}
                    >
                      <div className="platform-row-info">
                        <span style={{ color: 'var(--brand-primary)' }}>{p.name}</span>
                        <span style={{ color: 'var(--text-secondary)' }}>{p.count}</span>
                      </div>
                      <div className="platform-bar-container">
                        <div className="platform-bar-fill" style={{ width: `${barPercent}%` }}></div>
                      </div>
                    </div>
                  );
                })
              ) : (
                <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', textAlign: 'center', padding: '16px 0' }}>
                  No platform data available.
                </p>
              )}
            </div>
          </div>

          {/* Resume Effectiveness Section */}
          <div className="card-base resume-leaderboard-card">
            <h3 className="section-title">Resume Effectiveness</h3>
            <div className="resume-table-wrapper">
              {analytics.resumeStats.length > 0 ? (
                <table className="resume-table">
                  <thead>
                    <tr>
                      <th>Resume Name</th>
                      <th>Applied</th>
                      <th style={{ textAlign: 'right' }}>Conv. Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.resumeStats.map((r, idx) => (
                      <tr 
                        key={idx}
                        onClick={() => handleNavigateToApps('resume', r.resumeName)}
                      >
                        <td>
                          <div className="resume-tag-name" title={r.resumeName}>
                            {r.resumeName}
                          </div>
                        </td>
                        <td>{r.totalCount} sent</td>
                        <td style={{ textAlign: 'right' }}>
                          <span 
                            className="success-rate-badge"
                            style={{ 
                              backgroundColor: r.successRate >= 50 ? 'var(--accent-green-tint)' : 'var(--saved-gray-tint)',
                              color: r.successRate >= 50 ? 'var(--accent-green)' : 'var(--saved-gray)'
                            }}
                          >
                            {r.successRate}%
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', textAlign: 'center', padding: '24px 0' }}>
                  Upload resumes in job applications to see success statistics.
                </p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
