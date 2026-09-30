/**
 * Date and time utility functions for ApplyTrack Web.
 * Standardizes date parsing, display formatting, and range calculations.
 */

export const getLocalDateString = (timestampOrDate = new Date()) => {
  const date = new Date(timestampOrDate);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getLocalFirstOfMonth = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}-01`;
};

export const parseLocalDateStringToTimestamp = (selectedDateStr, originalTime = null) => {
  if (!selectedDateStr) return Date.now();
  
  if (originalTime) {
    const originalDateStr = getLocalDateString(originalTime);
    if (selectedDateStr === originalDateStr) {
      return originalTime;
    }
  }
  
  const todayStr = getLocalDateString(new Date());
  if (selectedDateStr === todayStr) {
    return Date.now();
  }
  
  const [year, month, day] = selectedDateStr.split('-').map(Number);
  return new Date(year, month - 1, day).getTime();
};

export const parseLocalDate = (dateStr) => {
  if (!dateStr) return null;
  const parts = dateStr.split('-').map(Number);
  if (parts.length !== 3 || parts.some(isNaN)) return null;
  const [year, month, day] = parts;
  return new Date(year, month - 1, day);
};

export const ymdToDmy = (ymd) => {
  if (!ymd) return '';
  const parts = ymd.split('-');
  if (parts.length !== 3) return ymd;
  const [year, month, day] = parts;
  return `${day}/${month}/${year}`;
};

export const dmyToYmd = (dmy) => {
  if (!dmy) return '';
  const parts = dmy.split('/');
  if (parts.length !== 3) return '';
  const [day, month, year] = parts;
  if (day.length === 2 && month.length === 2 && year.length === 4) {
    return `${year}-${month}-${day}`;
  }
  return '';
};

export const getFormattedStatusDate = (app) => {
  const history = app.statusHistory || [];
  const statusTimestamp = history.length > 0 ? history[history.length - 1].timestamp : app.createdAt;
  const dateStr = new Date(statusTimestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  return `${app.status} on ${dateStr}`;
};
