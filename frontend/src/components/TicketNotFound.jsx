import Link from 'next/link';

/**
 * @param {{ id: string }} props
 */
export default function TicketNotFound({ id }) {
  return (
    <div className="empty-state empty-state--centered">
      <p className="empty-state__eyebrow">404</p>
      <h1 className="empty-state__title">Ticket not found</h1>
      <p className="empty-state__body">
        No ticket exists for <strong>{id}</strong>. It may have been mistyped or removed from
        another environment.
      </p>
      <div className="empty-state__actions">
        <Link href="/tickets" className="button">
          Back to ticket list
        </Link>
      </div>
    </div>
  );
}
