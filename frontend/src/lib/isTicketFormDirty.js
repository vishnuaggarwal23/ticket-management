import { buildChangedTicketPatch } from './buildTicketPatch';

/** @typedef {import('../api/types.js').TicketDetail} TicketDetail */

/**
 * @param {TicketDetail} baseline
 * @param {Record<string, string>} values
 * @returns {boolean}
 */
export function isTicketFormDirty(baseline, values) {
  return Object.keys(buildChangedTicketPatch(baseline, values)).length > 0;
}
