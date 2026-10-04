/** @typedef {import('../api/types.js').TicketPriority} TicketPriority */

/** @type {TicketPriority[]} */
export const TICKET_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

/** @type {Record<TicketPriority, string>} */
const PRIORITY_MODIFIERS = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical',
};

/**
 * @param {TicketPriority | string} priority
 * @returns {string}
 */
export function priorityBadgeClass(priority) {
  const mod = PRIORITY_MODIFIERS[String(priority)] ?? 'medium';
  return `priority-badge priority-badge--${mod}`;
}
