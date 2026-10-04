import Link from 'next/link';

/**
 * @param {{ id: string }} props
 */
export default function TicketNotFound({ id }) {
  return (
    <div className="empty-state">
      <h1>Ticket not found</h1>
      <p>No ticket exists for {id}.</p>
      <Link href="/tickets">Back to ticket list</Link>
    </div>
  );
}
