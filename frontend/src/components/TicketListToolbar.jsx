import Link from 'next/link';
import { TICKET_STATUSES, formatStatusLabel } from '@/lib/ticketStatuses';
import { ticketsListHref } from '@/lib/ticketsListHref';
import StatusFilterChips from '@/components/StatusFilterChips';

/**
 * @param {{ q: string, status: string }} props
 */
export default function TicketListToolbar({ q, status }) {
  const hasFilters = Boolean(q || status);

  return (
    <div className="list-toolbar">
      <StatusFilterChips q={q} status={status} />
      <form className="ticket-list-toolbar" method="get" action="/tickets">
        {status ? <input type="hidden" name="status" value={status} /> : null}
        <div className="ticket-list-toolbar__field ticket-list-toolbar__field--grow">
          <label htmlFor="ticket-q">Search</label>
          <input
            id="ticket-q"
            name="q"
            type="search"
            defaultValue={q}
            placeholder="Search title or description…"
            autoComplete="off"
          />
          <span className="field-hint">Press Enter to search</span>
        </div>
        <div className="ticket-list-toolbar__field ticket-list-toolbar__field--compact">
          <label htmlFor="ticket-status">Status</label>
          <select id="ticket-status" name="status" defaultValue={status}>
            <option value="">All statuses</option>
            {TICKET_STATUSES.map((value) => (
              <option key={value} value={value}>
                {formatStatusLabel(value)}
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className="button">
          Search
        </button>
      </form>
      {hasFilters ? (
        <p className="list-toolbar__active">
          Filtered view.
          <Link href={ticketsListHref()} className="list-toolbar__clear">
            Reset all
          </Link>
        </p>
      ) : null}
    </div>
  );
}
