import Link from 'next/link';
import { ticketsListHref } from '@/lib/ticketsListHref';

/**
 * @typedef {import('../api/types.js').ListMeta} ListMeta
 * @param {{ meta: ListMeta, q: string, status: string, size: number }} props
 */
export default function TicketListPagination({ meta, q, status, size }) {
  const { page, totalPages, totalElements } = meta;
  const hasPrev = page > 0;
  const hasNext = page + 1 < totalPages;

  const prevHref = ticketsListHref({
    page: page - 1,
    size,
    q,
    status,
  });
  const nextHref = ticketsListHref({
    page: page + 1,
    size,
    q,
    status,
  });

  return (
    <nav className="ticket-pagination" aria-label="Ticket list pagination">
      <p className="ticket-pagination__summary">
        Page {page + 1} of {totalPages} ({totalElements} ticket
        {totalElements === 1 ? '' : 's'})
      </p>
      <div className="ticket-pagination__controls">
        {hasPrev ? (
          <Link href={prevHref} className="button button--secondary">
            Previous
          </Link>
        ) : (
          <span className="button button--secondary button--disabled" aria-disabled="true">
            Previous
          </span>
        )}
        {hasNext ? (
          <Link href={nextHref} className="button button--secondary">
            Next
          </Link>
        ) : (
          <span className="button button--secondary button--disabled" aria-disabled="true">
            Next
          </span>
        )}
      </div>
    </nav>
  );
}
