import { DEFAULT_TICKETS_LIST_SORT } from './ticketsListQuery.js';

/**
 * @param {Object} query
 * @param {number} [query.page]
 * @param {number} [query.size]
 * @param {string} [query.sort]
 * @param {string} [query.q]
 * @param {string} [query.status]
 * @returns {string}
 */
export function ticketsListHref({
  page = 0,
  size = 20,
  sort = DEFAULT_TICKETS_LIST_SORT,
  q = '',
  status = '',
} = {}) {
  const params = new URLSearchParams();
  if (page > 0) {
    params.set('page', String(page));
  }
  if (size !== 20) {
    params.set('size', String(size));
  }
  params.set('sort', sort || DEFAULT_TICKETS_LIST_SORT);
  if (q) {
    params.set('q', q);
  }
  if (status) {
    params.set('status', status);
  }
  const qs = params.toString();
  return qs ? `/tickets?${qs}` : '/tickets';
}
