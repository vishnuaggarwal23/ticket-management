/** @typedef {import('../api/types.js').TicketStatus} TicketStatus */

/** @type {TicketStatus[]} */
export const TICKET_STATUSES = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
];

/**
 * @param {TicketStatus} status
 * @returns {string}
 */
export function formatStatusLabel(status) {
  return status.replace(/_/g, ' ');
}
