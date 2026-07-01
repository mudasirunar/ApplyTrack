import React, { useState, useEffect, useRef } from 'react';
import { db } from '../utils/db';
import { AppIcon } from '../components/Icons';
import { getDashboardMessage } from '../utils/dashboardMessageProvider';
import './Dashboard.css';

// Custom Hook for smooth animation mimicking Jetpack Compose FastOutSlowInEasing (easeOutCubic)
function useAnimateProgress(dependency, duration = 1000) {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    let start = null;
    let animationFrameId;

    const step = (timestamp) => {
      if (!start) start = timestamp;
      const elapsed = timestamp - start;
      const p = Math.min(elapsed / duration, 1);

      // Cubic-bezier ease-out (equivalent to easeOutCubic: t => 1 - Math.pow(1 - t, 3))
      const easeOutProgress = 1 - Math.pow(1 - p, 3);
      setProgress(easeOutProgress);

      if (elapsed < duration) {
        animationFrameId = requestAnimationFrame(step);
      }
    };

    setProgress(0); // Reset animation when dependency changes
    animationFrameId = requestAnimationFrame(step);
    return () => cancelAnimationFrame(animationFrameId);
  }, [dependency, duration]);

  return progress;
}

// Reusable CircularProgressRing component to match Android's CircularProgressRing.kt
function CircularProgressRing({ percentage, color, label, size = 60, strokeWidth = 7 }) {
  const progress = useAnimateProgress(percentage);
  const r = 40;
  const cx = 50;
  const cy = 50;
  const circumference = 2 * Math.PI * r;

  const currentPercentage = percentage * progress;
  const strokeDashoffset = circumference - (currentPercentage / 100) * circumference;

  return (
    <div className="rate-card">
      <div className="circular-progress-container" style={{ width: size, height: size, position: 'relative' }}>
        <svg viewBox="0 0 100 100" className="circular-progress-svg" style={{ transform: 'rotate(-90deg)', width: '100%', height: '100%' }}>
          {/* Background Track Circle */}
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke="var(--bg-surface-variant)"
            strokeWidth={strokeWidth}
          />
          {/* Active Progress Circle */}
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
          />
        </svg>
        {/* Centered Percentage Text */}
        <div
          className="circular-progress-text"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: color,
            fontSize: '0.85rem',
            fontWeight: '800'
          }}
        >
          {Math.round(currentPercentage)}%
        </div>
      </div>
      <span className="rate-card-label">{label}</span>
    </div>
  );
}

// Reusable DonutChart component to isolate frame-by-frame anim renders
function DonutChart({ total, statusDistribution }) {
  const progress = useAnimateProgress(statusDistribution);

  const renderDonutChart = () => {
    if (!statusDistribution || statusDistribution.length === 0) return null;

    let accumulatedCount = 0;
    return (
      <svg viewBox="0 0 36 36" className="donut-chart-svg">
        {/* Background circle */}
        <circle
          cx="18"
          cy="18"
          r="15.915"
          fill="none"
          stroke="var(--bg-surface-variant)"
          strokeWidth="5.5"
        />

        {statusDistribution.map((slice, idx) => {
          const arcLength = (slice.count / total) * 100 * progress;
          const strokeDash = `${arcLength} ${100 - arcLength}`;
          const rotation = -90 + (accumulatedCount / total) * 360 * progress;
          accumulatedCount += slice.count;

          return (
            <circle
              key={idx}
              cx="18"
              cy="18"
              r="15.915"
              fill="none"
              stroke={slice.color}
              strokeWidth="5.5"
              strokeDasharray={strokeDash}
              strokeLinecap="round"
              transform={`rotate(${rotation} 18 18)`}
            />
          );
        })}
      </svg>
    );
  };

  return (
    <div style={{ position: 'relative' }}>
      {renderDonutChart()}
      <div className="donut-chart-center-text">
        <span className="donut-center-num">{Math.round(total * progress)}</span>
        <span className="donut-center-label">Total</span>
      </div>
    </div>
  );
}

// Helper for linear color interpolation (lerp)
function lerpColor(color1, color2, fraction) {
  const r1 = parseInt(color1.substring(1, 3), 16);
  const g1 = parseInt(color1.substring(3, 5), 16);
  const b1 = parseInt(color1.substring(5, 7), 16);

  const r2 = parseInt(color2.substring(1, 3), 16);
  const g2 = parseInt(color2.substring(3, 5), 16);
  const b2 = parseInt(color2.substring(5, 7), 16);

  const r = Math.round(r1 + (r2 - r1) * fraction);
  const g = Math.round(g1 + (g2 - g1) * fraction);
  const b = Math.round(b1 + (b2 - b1) * fraction);

  const hex = (r << 16 | g << 8 | b).toString(16).padStart(6, '0');
  return `#${hex}`;
}

// Map count to Android's activity spectrum color (red -> amber -> green)
function getBarColor(count, maxCount) {
  if (maxCount === 0 || count === 0) return '#78909C'; // default slate grey
  const ratio = count / maxCount;
  if (ratio < 0.5) {
    const fraction = ratio / 0.5;
    return lerpColor('#E53935', '#FFB300', fraction);
  } else {
    const fraction = (ratio - 0.5) / 0.5;
    return lerpColor('#FFB300', '#4CAF50', fraction);
  }
}

// Custom Year Selector matching YearFilterInput.kt
function YearSelector({ year, onChange }) {
  const handlePrev = () => {
    const current = parseInt(year, 10) || new Date().getFullYear();
    onChange((current - 1).toString());
  };

  const handleNext = () => {
    const current = parseInt(year, 10) || new Date().getFullYear();
    onChange((current + 1).toString());
  };

  const handleInputChange = (e) => {
    const val = e.target.value.replace(/\D/g, ''); // digits only
    if (val.length <= 4) {
      onChange(val);
    }
  };

  return (
    <div className="year-selector-container">
      <button type="button" onClick={handlePrev} className="year-selector-btn" title="Previous Year">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z" /></svg>
      </button>
      <input
        type="text"
        value={year}
        onChange={handleInputChange}
        className="year-selector-input"
        maxLength="4"
      />
      <button type="button" onClick={handleNext} className="year-selector-btn" title="Next Year">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z" /></svg>
      </button>
    </div>
  );
}

export default function Dashboard({ setActiveTab, setFilters }) {
  const [analytics, setAnalytics] = useState(db.getAnalytics());
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear().toString());
  const [user, setUser] = useState(db.getCurrentUser());
  const [dashboardMessage, setDashboardMessage] = useState('');
  const scrollRef = useRef(null);
  const legendScrollRef = useRef(null);

  // Monthly Chart Drag Scrolling
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeft, setScrollLeft] = useState(0);

  const handleMouseDown = (e) => {
    if (e.button !== 0) return;
    if (scrollRef.current.scrollWidth <= scrollRef.current.clientWidth) return;
    setIsDragging(true);
    setStartX(e.pageX - scrollRef.current.offsetLeft);
    setScrollLeft(scrollRef.current.scrollLeft);
    scrollRef.current.style.cursor = 'grabbing';
  };

  const handleMouseLeave = () => {
    if (isDragging) {
      setIsDragging(false);
      scrollRef.current.style.cursor = '';
    }
  };

  const handleMouseUp = () => {
    if (isDragging) {
      setIsDragging(false);
      scrollRef.current.style.cursor = '';
    }
  };

  const handleMouseMove = (e) => {
    if (!isDragging) return;
    e.preventDefault();
    const x = e.pageX - scrollRef.current.offsetLeft;
    const walk = (x - startX) * 1.5;
    scrollRef.current.scrollLeft = scrollLeft - walk;
  };

  // Legend Drag Scrolling
  const [isLegendDragging, setIsLegendDragging] = useState(false);
  const [legendStartX, setLegendStartX] = useState(0);
  const [legendScrollLeft, setLegendScrollLeft] = useState(0);

  const handleLegendMouseDown = (e) => {
    if (e.button !== 0) return;
    if (legendScrollRef.current.scrollWidth <= legendScrollRef.current.clientWidth) return;
    setIsLegendDragging(true);
    setLegendStartX(e.pageX - legendScrollRef.current.offsetLeft);
    setLegendScrollLeft(legendScrollRef.current.scrollLeft);
    legendScrollRef.current.style.cursor = 'grabbing';
  };

  const handleLegendMouseLeave = () => {
    if (isLegendDragging) {
      setIsLegendDragging(false);
      legendScrollRef.current.style.cursor = '';
    }
  };

  const handleLegendMouseUp = () => {
    if (isLegendDragging) {
      setIsLegendDragging(false);
      legendScrollRef.current.style.cursor = '';
    }
  };

  const handleLegendMouseMove = (e) => {
    if (!isLegendDragging) return;
    e.preventDefault();
    const x = e.pageX - legendScrollRef.current.offsetLeft;
    const walk = (x - legendStartX) * 1.5;
    legendScrollRef.current.scrollLeft = legendScrollLeft - walk;
  };

  useEffect(() => {
    const handleDataChange = () => {
      setAnalytics(db.getAnalytics());
    };
    window.addEventListener('applytrack_data_change', handleDataChange);
    return () => {
      window.removeEventListener('applytrack_data_change', handleDataChange);
    };
  }, []);

  useEffect(() => {
    // Set initial message immediately
    setDashboardMessage(getDashboardMessage(analytics.total));

    // Rotate message every 10 seconds
    const interval = setInterval(() => {
      setDashboardMessage(getDashboardMessage(analytics.total));
    }, 10000);

    return () => clearInterval(interval);
  }, [analytics.total]);

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
      const monthMap = {
        'Jan': '1', 'Feb': '2', 'Mar': '3', 'Apr': '4', 'May': '5', 'Jun': '6',
        'Jul': '7', 'Aug': '8', 'Sep': '9', 'Oct': '10', 'Nov': '11', 'Dec': '12'
      };
      newFilters.statusFilter = 'Date';
      newFilters.dateFilterMode = 'Month';
      newFilters.dateMonth = monthMap[value.month] || (new Date().getMonth() + 1).toString();
      newFilters.dateYear = value.year ? value.year.toString() : new Date().getFullYear().toString();
    }

    setFilters(newFilters);
    setActiveTab('applications');
  };

  // Monthly Chart Math
  const availableYears = Object.keys(analytics.monthlyActivity || {}).sort((a, b) => b - a);
  const chartYear = selectedYear || availableYears[0] || new Date().getFullYear().toString();
  const monthsList = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthlyDataForYear = analytics.monthlyActivity[chartYear] || {};
  const maxApplicationsInMonth = Math.max(...monthsList.map(m => monthlyDataForYear[m] || 0), 1);
  const isMonthlyActivityEmpty = !Object.values(monthlyDataForYear).some(count => count > 0);





  return (
    <div className="content-container dashboard-container animate-fade-in">
      {/* Dashboard Header */}
      <div className="dashboard-header">
        <h2 className="dashboard-greeting">Your Personal Job Tracker</h2>
        <p className="dashboard-subtext">
          {dashboardMessage}
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
          className="status-stat-card card-base card-interactive"
          style={{ backgroundColor: 'rgba(47, 58, 74, 0.05)' }}
          onClick={() => handleNavigateToApps('status', 'Response')}
        >
          <span className="status-card-header" style={{ color: 'var(--brand-primary)' }}>Response</span>
          <span className="status-card-count" style={{ color: 'var(--brand-primary)' }}>{analytics.responses}</span>
        </div>
      </div>

      {/* Conversion Rates Section */}
      {analytics.total > 0 && (
        <div className="conversion-rates-card card-base">
          <div className="chart-header">
            <h3 className="section-title" style={{ margin: 0 }}>Conversion Rates</h3>
          </div>
          <div className="rates-container">
            <CircularProgressRing
              label="Success"
              percentage={analytics.successRate}
              color="var(--link-blue)"
            />
            <CircularProgressRing
              label="Interview"
              percentage={analytics.interviewRate}
              color="var(--accent-green)"
            />
            <CircularProgressRing
              label="Rejection"
              percentage={analytics.rejectionRate}
              color="var(--error-red)"
            />
            <CircularProgressRing
              label="Response"
              percentage={analytics.responseRate}
              color="var(--brand-primary)"
            />
          </div>
        </div>
      )}

      {/* Double Column Section (Charts & platform list) */}
      {analytics.total > 0 && (
        <div className="dashboard-grid-2">
          {/* Status Distribution */}
          <div className="card-base donut-chart-container">
            <div className="chart-header" style={{ width: '100%' }}>
              <h3 className="section-title" style={{ margin: 0 }}>Status Distribution</h3>
            </div>
            <DonutChart total={analytics.total} statusDistribution={analytics.statusDistribution} />
            <div
              className="donut-legend"
              ref={legendScrollRef}
              onMouseDown={handleLegendMouseDown}
              onMouseLeave={handleLegendMouseLeave}
              onMouseUp={handleLegendMouseUp}
              onMouseMove={handleLegendMouseMove}
            >
              {analytics.statusDistribution.map((slice, idx) => (
                <div
                  key={idx}
                  className="legend-item"
                >
                  <span className="legend-color" style={{ backgroundColor: slice.color }}></span>
                  <span style={{ color: 'var(--text-secondary)' }}>{slice.name} ({slice.count})</span>
                </div>
              ))}
            </div>
          </div>

          {/* Monthly Activity Section */}
          <div className={`card-base monthly-activity-card${isMonthlyActivityEmpty ? ' empty' : ''}`}>
            <div className="chart-header">
              <h3 className="section-title" style={{ margin: 0 }}>Monthly Activity</h3>
              <YearSelector year={chartYear} onChange={setSelectedYear} />
            </div>

            {isMonthlyActivityEmpty ? (
              <div className="card-empty-text">
                No application activity recorded in {chartYear}. Try adding or editing applications to see activity.
              </div>
            ) : (
              <>
                {/* Less/More Active Legend matching native Compose BarChart */}
                <div className="activity-legend">
                  <span className="legend-text">Less Active</span>
                  <div className="legend-gradient-bar"></div>
                  <span className="legend-text">More Active</span>
                </div>

                {/* Scrollable drag-to-scroll chart wrapper */}
                <div
                  className="activity-chart-scroll-container"
                  ref={scrollRef}
                  onMouseDown={handleMouseDown}
                  onMouseLeave={handleMouseLeave}
                  onMouseUp={handleMouseUp}
                  onMouseMove={handleMouseMove}
                >
                  <div className="activity-chart-inner">
                    {monthsList.map((month) => {
                      const count = monthlyDataForYear[month] || 0;
                      const percentHeight = (count / maxApplicationsInMonth) * 100;
                      const barColor = getBarColor(count, maxApplicationsInMonth);
                      const barBackground = count > 0 ? `linear-gradient(to bottom, ${barColor}, ${barColor}80)` : 'transparent';

                      return (
                        <div key={month} className="chart-bar-column">
                          <div className="chart-bar-container">
                            {count > 0 && (
                              <div
                                className="chart-bar"
                                style={{
                                  height: `${percentHeight}%`,
                                  background: barBackground,
                                  border: `1px solid ${barColor}4d`
                                }}
                                onClick={() => handleNavigateToApps('month', { month, year: Number(chartYear) })}
                                title={`${count} ${count === 1 ? 'activity' : 'activities'}`}
                              />
                            )}
                          </div>
                          <span
                            className="chart-bar-count"
                            style={{
                              color: count > 0 ? barColor : 'var(--text-secondary)',
                              opacity: count > 0 ? 1 : 0.5
                            }}
                          >
                            {count}
                          </span>
                          <span className="chart-label">{month}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Double Column Section (Platform & Resume effectiveness) */}
      {analytics.total > 0 && (
        <div className="dashboard-grid-2">
          {/* Platform Breakdown */}
          <div className="card-base platform-breakdown-card">
            <div className="platform-header-container">
              <h3 className="section-title" style={{ margin: 0 }}>Platforms</h3>
            </div>
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
                <div className="card-empty-text">
                  No platform data recorded yet. Add platforms to your applications to see breakdown.
                </div>
              )}
            </div>
          </div>

          {/* Resume Effectiveness Section */}
          <div className="card-base resume-leaderboard-card">
            <div className="chart-header">
              <h3 className="section-title" style={{ margin: 0 }}>Resume Effectiveness</h3>
            </div>
            <div className="resume-table-wrapper">
              {analytics.resumeStats.length > 0 ? (
                <table className="resume-table">
                  <thead>
                    <tr>
                      <th style={{ textAlign: 'left' }}>Resume</th>
                      <th style={{ textAlign: 'center', width: '45px' }}>Used</th>
                      <th style={{ textAlign: 'center', width: '45px', color: 'var(--accent-green)' }}>Int.</th>
                      <th style={{ textAlign: 'center', width: '45px', color: 'var(--link-blue)' }}>Offer</th>
                      <th style={{ textAlign: 'center', width: '45px', color: 'var(--error-red)' }}>Rej.</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.resumeStats.map((r, idx) => {
                      const displayName = r.resumeName.replace(/\.(pdf|docx|doc)$/i, '');
                      return (
                        <tr key={idx}>
                          <td style={{ textAlign: 'left' }}>
                            <div
                              className="resume-tag-name"
                              title={r.resumeName}
                              onClick={() => handleNavigateToApps('resume', r.resumeName)}
                            >
                              {displayName}
                            </div>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            {r.totalUsed}
                          </td>
                          <td style={{ textAlign: 'center', color: 'var(--accent-green)' }}>
                            {r.interviewCount}
                          </td>
                          <td style={{ textAlign: 'center', color: 'var(--link-blue)' }}>
                            {r.offerCount}
                          </td>
                          <td style={{ textAlign: 'center', color: 'var(--error-red)' }}>
                            {r.rejectedCount}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              ) : (
                <div className="card-empty-text">
                  No resumes attached yet. Attach a CV/resume to applications to track which works best.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
