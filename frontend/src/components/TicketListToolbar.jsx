import { TICKET_STATUSES, formatStatusLabel } from '@/lib/ticketStatuses';

/**
 * @param {{ q: string, status: string }} props
 */
export default function TicketListToolbar({ q, status }) {
  return (
    <form className="ticket-list-toolbar" method="get" action="/tickets">
      <div className="ticket-list-toolbar__field">
        <label htmlFor="ticket-q">Search</label>
        <input
          id="ticket-q"
          name="q"
          type="search"
          defaultValue={q}
          placeholder="Keyword in title or description"
          autoComplete="off"
        />
      </div>
      <div className="ticket-list-toolbar__field">
        <label htmlFor="ticket-status">Status</label>
        <select id="ticket-status" name="status" defaultValue={status}>
          <option value="">All</option>
          {TICKET_STATUSES.map((value) => (
            <option key={value} value={value}>
              {formatStatusLabel(value)}
            </option>
          ))}
        </select>
      </div>
      <button type="submit" className="button button--secondary">
        Apply
      </button>
    </form>
  );
}
