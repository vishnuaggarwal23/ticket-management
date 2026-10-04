import Link from 'next/link';
import { listTickets } from '@/api/tickets';
import { ApiError } from '@/api/client';
import Breadcrumbs from '@/components/Breadcrumbs';
import ErrorBanner from '@/components/ErrorBanner';
import TicketListPagination from '@/components/TicketListPagination';
import TicketListToolbar from '@/components/TicketListToolbar';
import TicketTable from '@/components/TicketTable';
import { formatStatusLabel } from '@/lib/ticketStatuses';
import { DEFAULT_TICKETS_LIST_SORT } from '@/lib/ticketsListQuery';

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
      sort: DEFAULT_TICKETS_LIST_SORT,
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
        message: 'Could not load tickets. Check that the API is running.',
        status: 0,
        code: 'LOAD_FAILED',
        details: [],
      });
    }
  }

  return (
    <div className="ticket-list-page">
      <Breadcrumbs items={[{ label: 'Tickets' }]} />

      <header className="page-header page-header--compact">
        <div>
          <h1>Tickets</h1>
          <p className="page-header__lede">
            Use status chips for one-click filters, or search and press Enter.
          </p>
        </div>
      </header>

      <TicketListToolbar q={q} status={status} />

      {loadError ? (
        <ErrorBanner message={loadError.message} details={loadError.details} />
      ) : null}

      {!loadError && tickets.length === 0 && !hasFilters ? (
        <div className="empty-state empty-state--centered">
          <p className="empty-state__eyebrow">Get started</p>
          <h2 className="empty-state__title">No tickets yet</h2>
          <p className="empty-state__body">
            Create a ticket to track support work, add comments, and try grounded search on the Ask
            page.
          </p>
          <div className="empty-state__actions">
            <Link href="/tickets/new" className="button">
              Create your first ticket
            </Link>
          </div>
        </div>
      ) : null}

      {!loadError && tickets.length === 0 && hasFilters ? (
        <div className="empty-state empty-state--centered">
          <h2 className="empty-state__title">No matching tickets</h2>
          <p className="empty-state__body">{buildNoMatchesMessage(q, status)}</p>
          <div className="empty-state__actions">
            <Link href="/tickets" className="button button--secondary">
              Clear filters
            </Link>
          </div>
        </div>
      ) : null}

      {!loadError && tickets.length > 0 && meta ? (
        <>
          <TicketTable tickets={tickets} />
          <TicketListPagination
            meta={meta}
            q={q}
            status={status}
            size={size}
            sort={DEFAULT_TICKETS_LIST_SORT}
          />
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
