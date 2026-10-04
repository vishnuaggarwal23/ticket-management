/** @typedef {import('../api/types.js').TicketStatus} TicketStatus */

/** @type {Record<TicketStatus, TicketStatus[]>} */
export const LEGAL_STATUS_TARGETS = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

/**
 * @param {TicketStatus} current
 * @returns {TicketStatus[]}
 */
export function legalTargetsForStatus(current) {
  return LEGAL_STATUS_TARGETS[current] ?? [];
}
