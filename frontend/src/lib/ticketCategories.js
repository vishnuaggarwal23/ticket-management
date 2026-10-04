/** @typedef {import('../api/types.js').TicketCategory} TicketCategory */

/** @type {TicketCategory[]} */
export const TICKET_CATEGORIES = [
  'PAYMENTS',
  'SHIPMENT',
  'BILLING',
  'LOGIN',
  'OTHER',
];

/**
 * @param {TicketCategory} category
 * @returns {string}
 */
export function formatCategoryLabel(category) {
  return category.replace(/_/g, ' ');
}
