'use client';

import { useRouter } from 'next/navigation';
import { formatInstant } from '@/lib/formatDate';
import { priorityBadgeClass } from '@/lib/ticketPriorities';
import { formatStatusLabel, statusBadgeClass } from '@/lib/ticketStatuses';

/**
 * @typedef {import('../api/types.js').TicketSummary} TicketSummary
 * @param {{ tickets: TicketSummary[] }} props
 */
export default function TicketTable({ tickets }) {
  const router = useRouter();

  /**
   * @param {TicketSummary} ticket
   */
  function openTicket(ticket) {
    router.push(`/tickets/${ticket.id}`);
  }

  /**
   * @param {React.KeyboardEvent<HTMLTableRowElement>} event
   * @param {TicketSummary} ticket
   */
  function handleRowKeyDown(event, ticket) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openTicket(ticket);
    }
  }

  return (
    <div className="ticket-table-wrap">
      <table className="ticket-table">
        <caption className="visually-hidden">
          Support tickets; click a row or press Enter to open details
        </caption>
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
            <tr
              key={ticket.id}
              className="ticket-table__row ticket-table__row--clickable"
              tabIndex={0}
              role="link"
              aria-label={`Open ticket ${ticket.id}: ${ticket.title}`}
              onClick={() => openTicket(ticket)}
              onKeyDown={(event) => handleRowKeyDown(event, ticket)}
            >
              <td data-label="ID">
                <span className="ticket-table__id">{ticket.id}</span>
              </td>
              <td data-label="Title">
                <span className="ticket-table__title">{ticket.title}</span>
              </td>
              <td data-label="Status">
                <span className={statusBadgeClass(ticket.status)}>
                  {formatStatusLabel(ticket.status)}
                </span>
              </td>
              <td data-label="Priority">
                <span className={priorityBadgeClass(ticket.priority)}>{ticket.priority}</span>
              </td>
              <td data-label="Assignee">{ticket.assignee ?? '—'}</td>
              <td data-label="Updated">
                <time dateTime={ticket.updatedAt}>{formatInstant(ticket.updatedAt)}</time>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="ticket-table__hint">Tip: click any row to open the ticket.</p>
    </div>
  );
}
