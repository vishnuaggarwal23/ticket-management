/** @typedef {import('../api/types.js').TicketStatus} TicketStatus */

/** @type {TicketStatus[]} */
export const TICKET_STATUSES = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
];

/** @type {Record<TicketStatus, string>} */
const STATUS_MODIFIERS = {
  OPEN: 'open',
  IN_PROGRESS: 'in-progress',
  RESOLVED: 'resolved',
  CLOSED: 'closed',
  CANCELLED: 'cancelled',
};

/**
 * @param {TicketStatus | string} status
 * @returns {string}
 */
export function formatStatusLabel(status) {
  return String(status).replace(/_/g, ' ');
}

/**
 * CSS modifier for colored status badges.
 * @param {TicketStatus | string} status
 * @returns {string}
 */
export function statusBadgeClass(status) {
  const key = String(status);
  const mod = STATUS_MODIFIERS[key] ?? 'unknown';
  return `status-badge status-badge--${mod}`;
}
