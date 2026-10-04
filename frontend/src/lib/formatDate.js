/**
 * @param {string|null|undefined} iso
 * @returns {string}
 */
export function formatInstant(iso) {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return date.toLocaleString();
}
