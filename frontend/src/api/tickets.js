/**
 * Ticket and comment API calls.
 * @module api/tickets
 */

import { requestJson, unwrapData, unwrapListEnvelope } from './client.js';

/**
 * @typedef {import('./types.js').TicketSummary} TicketSummary
 * @typedef {import('./types.js').TicketDetail} TicketDetail
 * @typedef {import('./types.js').Comment} Comment
 * @typedef {import('./types.js').ListMeta} ListMeta
 * @typedef {import('./types.js').CreateTicketRequest} CreateTicketRequest
 * @typedef {import('./types.js').UpdateTicketRequest} UpdateTicketRequest
 * @typedef {import('./types.js').CreateCommentRequest} CreateCommentRequest
 * @typedef {import('./types.js').TicketStatus} TicketStatus
 */

/**
 * @param {Record<string, string|number|undefined|null>} params
 * @returns {string}
 */
function toQueryString(params) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  }
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

/**
 * @param {Object} [query]
 * @param {number} [query.page]
 * @param {number} [query.size]
 * @param {string} [query.sort]
 * @param {string} [query.q]
 * @param {TicketStatus} [query.status]
 * @returns {Promise<{ data: TicketSummary[], meta: ListMeta }>}
 */
export async function listTickets({ page, size, sort, q, status } = {}) {
  const path = `/api/v1/tickets${toQueryString({ page, size, sort, q, status })}`;
  const body = await requestJson(path, { cache: 'no-store' });
  return unwrapListEnvelope(body);
}

/**
 * @param {string} id
 * @returns {Promise<TicketDetail>}
 */
export async function getTicket(id) {
  const body = await requestJson(`/api/v1/tickets/${encodeURIComponent(id)}`);
  return /** @type {TicketDetail} */ (unwrapData(body));
}

/**
 * @param {CreateTicketRequest} payload
 * @returns {Promise<TicketDetail>}
 */
export async function createTicket(payload) {
  const body = await requestJson('/api/v1/tickets', {
    method: 'POST',
    body: payload,
  });
  return /** @type {TicketDetail} */ (unwrapData(body));
}

/**
 * @param {string} id
 * @param {UpdateTicketRequest} payload
 * @returns {Promise<TicketDetail>}
 */
export async function patchTicket(id, payload) {
  const body = await requestJson(`/api/v1/tickets/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: payload,
  });
  return /** @type {TicketDetail} */ (unwrapData(body));
}

/**
 * @param {string} ticketId
 * @param {CreateCommentRequest} payload
 * @returns {Promise<Comment>}
 */
export async function addComment(ticketId, payload) {
  const body = await requestJson(
    `/api/v1/tickets/${encodeURIComponent(ticketId)}/comments`,
    {
      method: 'POST',
      body: payload,
    },
  );
  return /** @type {Comment} */ (unwrapData(body));
}
