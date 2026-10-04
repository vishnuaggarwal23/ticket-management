import AskPanel from '@/components/AskPanel';

/**
 * @param {{ searchParams: Promise<Record<string, string | string[] | undefined>> }} props
 */
export default async function AskPage({ searchParams }) {
  const raw = await searchParams;
  const prefill = pickString(raw.prefill);
  return <AskPanel initialQuestion={prefill} />;
}

/**
 * @param {string | string[] | undefined} value
 * @returns {string}
 */
function pickString(value) {
  if (typeof value === 'string') {
    return value.trim();
  }
  if (Array.isArray(value) && typeof value[0] === 'string') {
    return value[0].trim();
  }
  return '';
}
