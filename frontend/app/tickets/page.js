import Link from 'next/link';
import { listTickets } from '@/api/tickets';
import { ApiError } from '@/api/client';
import ErrorBanner from '@/components/ErrorBanner';
import TicketListPagination from '@/components/TicketListPagination';
import TicketListToolbar from '@/components/TicketListToolbar';
import TicketTable from '@/components/TicketTable';
import { formatStatusLabel } from '@/lib/ticketStatuses';

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

/**
 * @param {{ searchParams: Promise<Record<string, string | string[] | undefined>> }} props
 */
export default async function TicketsPage({ searchParams }) {
  const raw = await searchParams;
  const page = parsePage(raw.page);
  const size = parseSize(raw.size);
  const q = pickString(raw.q);
  const status = pickString(raw.status);

  const hasFilters = Boolean(q || status);

  let tickets = [];
  /** @type {import('@/api/types.js').ListMeta | null} */
  let meta = null;
  /** @type {ApiError | null} */
  let loadError = null;

  try {
    const result = await listTickets({
      page,
      size,
      q: q || undefined,
      status: status || undefined,
    });
    tickets = result.data;
    meta = result.meta;
  } catch (error) {
    if (error instanceof ApiError) {
      loadError = error;
    } else {
      loadError = new ApiError({
        message: 'Could not load tickets. Is the API running on port 8080?',
        status: 0,
        code: 'LOAD_FAILED',
        details: [],
      });
    }
  }

  return (
    <div className="ticket-list-page">
      <header className="page-header">
        <h1>Tickets</h1>
        <Link href="/tickets/new" className="button">
          New ticket
        </Link>
      </header>

      <TicketListToolbar q={q} status={status} />

      {loadError ? (
        <ErrorBanner message={loadError.message} details={loadError.details} />
      ) : null}

      {!loadError && tickets.length === 0 && !hasFilters ? (
        <div className="empty-state">
          <p>No tickets yet.</p>
          <Link href="/tickets/new">Create your first ticket</Link>
        </div>
      ) : null}

      {!loadError && tickets.length === 0 && hasFilters ? (
        <div className="empty-state">
          <p>{buildNoMatchesMessage(q, status)}</p>
          <Link href="/tickets">Clear filters</Link>
        </div>
      ) : null}

      {!loadError && tickets.length > 0 && meta ? (
        <>
          <TicketTable tickets={tickets} />
          <TicketListPagination meta={meta} q={q} status={status} size={size} />
        </>
      ) : null}
    </div>
  );
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

/**
 * @param {string | string[] | undefined} value
 * @returns {number}
 */
function parsePage(value) {
  const text = pickString(value);
  if (!text) {
    return DEFAULT_PAGE;
  }
  const n = Number.parseInt(text, 10);
  return Number.isFinite(n) && n >= 0 ? n : DEFAULT_PAGE;
}

/**
 * @param {string | string[] | undefined} value
 * @returns {number}
 */
function parseSize(value) {
  const text = pickString(value);
  if (!text) {
    return DEFAULT_SIZE;
  }
  const n = Number.parseInt(text, 10);
  if (!Number.isFinite(n) || n < 1 || n > 100) {
    return DEFAULT_SIZE;
  }
  return n;
}

/**
 * @param {string} q
 * @param {string} status
 * @returns {string}
 */
function buildNoMatchesMessage(q, status) {
  const parts = [];
  if (q) {
    parts.push(`keyword “${q}”`);
  }
  if (status) {
    parts.push(`status ${formatStatusLabel(status)}`);
  }
  if (parts.length === 0) {
    return 'No matches for your filters.';
  }
  return `No matches for ${parts.join(' and ')}.`;
}
