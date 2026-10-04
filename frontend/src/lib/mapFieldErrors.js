/**
 * @param {import('../api/types.js').ApiErrorDetail[]} [details]
 * @returns {Record<string, string>}
 */
export function mapFieldErrors(details = []) {
  const map = {};
  for (const entry of details) {
    if (!entry.field) {
      continue;
    }
    const message = entry.message || entry.code || 'Invalid value';
    map[entry.field] = message;
  }
  return map;
}
