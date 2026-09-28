import React, { useMemo, useState, useEffect, useLayoutEffect, useRef } from 'react';
import { db } from '../utils/db';
import './ActivityGrid.css';
import { createPortal } from 'react-dom';

/* ───────── DATA SOURCE (edit this to match your db) ───────── */
export function getActivityDates() {
    return db.getApplications().map(a => a.createdAt);
}
/* ──────────────────────────────────────────────────────────── */

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const pad = (n) => String(n).padStart(2, '0');
const toKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

const toDateKey = (v) => {
    if (!v) return null;
    if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}/.test(v)) return v.slice(0, 10);
    const d = v?.toDate ? v.toDate() : new Date(v); // handles Firestore Timestamp
    return isNaN(d) ? null : toKey(d);
};

const ordinal = (n) => {
    const s = ['th', 'st', 'nd', 'rd'], v = n % 100;
    return n + (s[(v - 20) % 10] || s[v] || s[0]);
};

function getRange(selection) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    let start, end;
    if (selection === 'last') {
        end = today;
        start = new Date(today);
        start.setFullYear(start.getFullYear() - 1);
        start.setDate(start.getDate() + 1);
    } else {
        const y = Number(selection);
        start = new Date(y, 0, 1);
        end = new Date(y, 11, 31);
    }
    return { start, end };
}

export default function ActivityGrid({ dates = [], isLoading = false, onDayClick }) {
    const [selection, setSelection] = useState('last');
    const [tip, setTip] = useState(null);
    const scrollRef = useRef(null);

    const counts = useMemo(() => {
        const map = {};
        dates.forEach((v) => {
            const k = toDateKey(v);
            if (k) map[k] = (map[k] || 0) + 1;
        });
        return map;
    }, [dates]);

    const years = useMemo(
        () => [...new Set(Object.keys(counts).map(k => k.slice(0, 4)))].sort((a, b) => b - a),
        [counts]
    );

    const active = selection === 'last' || years.includes(selection) ? selection : 'last';

    const { weeks, monthLabels, total, max } = useMemo(() => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const { start, end } = getRange(active);
        const dow = (start.getDay() + 6) % 7; // Monday = 0
        const gs = new Date(start.getFullYear(), start.getMonth(), start.getDate() - dow);
        const spanDays = Math.round((end - gs) / 86400000) + 1;
        const weekCount = Math.ceil(spanDays / 7);

        const weeks = [];
        let total = 0, max = 0;
        let lastMonth = -1;
        const labels = [];

        for (let w = 0; w < weekCount; w++) {
            const days = [];
            let firstVisible = null;
            for (let d = 0; d < 7; d++) {
                const date = new Date(gs.getFullYear(), gs.getMonth(), gs.getDate() + w * 7 + d);
                const inRange = date >= start && date <= end;
                const count = inRange ? counts[toKey(date)] || 0 : 0;
                if (inRange) {
                    total += count;
                    max = Math.max(max, count);
                    if (!firstVisible) firstVisible = date;
                }
                days.push({ date, inRange, count, future: date > today });
            }
            weeks.push(days);
            if (firstVisible && firstVisible.getMonth() !== lastMonth) {
                lastMonth = firstVisible.getMonth();
                labels.push({ week: w, text: MONTHS[lastMonth].slice(0, 3) });
            }
        }
        // Drop a label that would collide with the next one
        const monthLabels = labels.filter((l, i) => !labels[i + 1] || labels[i + 1].week - l.week >= 3);
        return { weeks, monthLabels, total, max };
    }, [active, counts]);

    const getLevel = (count) => {
        if (count <= 0) return 0;
        return Math.min(4, Math.ceil((count / Math.max(max, 4)) * 4));
    };

    // Mobile: land on the most recent weeks
    useLayoutEffect(() => {
        const el = scrollRef.current;
        if (!el) return;
        if (active === 'last') {
            el.scrollLeft = el.scrollWidth;
        } else if (active === String(new Date().getFullYear())) {
            const now = new Date();
            const dayOfYear = Math.floor((now - new Date(now.getFullYear(), 0, 1)) / 86400000);
            const ratio = dayOfYear / 365;
            el.scrollLeft = ratio * el.scrollWidth - el.clientWidth / 2;
        } else {
            el.scrollLeft = 0;
        }
    }, [active, isLoading]);

    useEffect(() => {
        const hide = () => setTip(null);
        window.addEventListener('scroll', hide, true);
        return () => window.removeEventListener('scroll', hide, true);
    }, []);

    const showTip = (e, cell) => {
        if (isLoading || cell.future) return;
        const r = e.currentTarget.getBoundingClientRect();
        const { date, count } = cell;
        const when = `${MONTHS[date.getMonth()]} ${ordinal(date.getDate())}, ${date.getFullYear()}`;
        setTip({
            left: r.left + r.width / 2,
            top: r.top,
            text: count === 0
                ? `No applications on ${when}`
                : `${count} application${count === 1 ? '' : 's'} on ${when}`
        });
    };

    const dayLabels = { 0: 'Mon', 2: 'Wed', 4: 'Fri', 6: 'Sun' };

    return (
        <div className="card-base activity-card">
            <div className="chart-header">
                <h3 className="section-title" style={{ margin: 0 }}>Application Activity</h3>
                <select
                    className="ag-select"
                    value={active}
                    disabled={isLoading}
                    onChange={(e) => setSelection(e.target.value)}
                >
                    <option value="last">Last year</option>
                    {years.map(y => <option key={y} value={y}>{y}</option>)}
                </select>
            </div>

            <div className="ag-scroll" ref={scrollRef}>
                <div
                    className="ag-grid"
                    style={{ gridTemplateColumns: `28px repeat(${weeks.length}, minmax(0, 1fr))` }}
                    onMouseLeave={() => setTip(null)}
                >
                    {monthLabels.map(l => (
                        <span key={l.week} className="ag-month" style={{ gridColumn: l.week + 2, gridRow: 1 }}>
                            {isLoading ? '' : l.text}
                        </span>
                    ))}

                    {Object.entries(dayLabels).map(([i, t]) => (
                        <span key={t} className="ag-day" style={{ gridColumn: 1, gridRow: Number(i) + 2 }}>{t}</span>
                    ))}

                    {weeks.map((days, w) =>
                        days.map((cell, d) =>
                            cell.inRange ? (
                                <div
                                    key={`${w}-${d}`}
                                    className={`ag-cell ${isLoading ? 'ag-skeleton' : `ag-l${getLevel(cell.count)}`}${cell.future ? ' ag-future' : ''}${cell.count > 0 && onDayClick ? ' ag-clickable' : ''}`}
                                    style={{ gridColumn: w + 2, gridRow: d + 2 }}
                                    onMouseEnter={(e) => showTip(e, cell)}
                                    onClick={() => {
                                        if (!isLoading && !cell.future && cell.count > 0 && onDayClick) onDayClick(toKey(cell.date));
                                    }}
                                />
                            ) : (
                                <div key={`${w}-${d}`} style={{ gridColumn: w + 2, gridRow: d + 2 }} />
                            )
                        )
                    )}
                </div>
            </div>

            <div className="ag-footer">
                <span className="ag-summary">
                    {isLoading
                        ? 'Syncing activity...'
                        : `${total} application${total === 1 ? '' : 's'} ${active === 'last' ? 'in the last year' : `in ${active}`}`}
                </span>
                <div className="ag-legend">
                    <span>Less</span>
                    {[0, 1, 2, 3, 4].map(l => <span key={l} className={`ag-legend-cell ag-l${l}`} />)}
                    <span>More</span>
                </div>
            </div>

            {tip && createPortal(
                <div className="ag-tooltip" style={{ left: tip.left, top: tip.top }}>{tip.text}</div>,
                document.body
            )}
        </div>
    );
}