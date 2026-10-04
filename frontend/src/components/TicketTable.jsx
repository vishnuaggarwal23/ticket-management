import Link from 'next/link';
import { formatInstant } from '@/lib/formatDate';
import { formatStatusLabel } from '@/lib/ticketStatuses';

/**
 * @typedef {import('../api/types.js').TicketSummary} TicketSummary
 * @param {{ tickets: TicketSummary[] }} props
 */
export default function TicketTable({ tickets }) {
  return (
    <div className="ticket-table-wrap">
      <table className="ticket-table">
        <thead>
          <tr>
            <th scope="col">ID</th>
            <th scope="col">Title</th>
            <th scope="col">Status</th>
            <th scope="col">Priority</th>
            <th scope="col">Assignee</th>
            <th scope="col">Updated</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((ticket) => (
            <tr key={ticket.id} className="ticket-table__row">
              <td>
                <Link href={`/tickets/${ticket.id}`} className="ticket-table__link">
                  {ticket.id}
                </Link>
              </td>
              <td>
                <Link href={`/tickets/${ticket.id}`} className="ticket-table__link">
                  {ticket.title}
                </Link>
              </td>
              <td>{formatStatusLabel(ticket.status)}</td>
              <td>{ticket.priority}</td>
              <td>{ticket.assignee ?? '—'}</td>
              <td>{formatInstant(ticket.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
