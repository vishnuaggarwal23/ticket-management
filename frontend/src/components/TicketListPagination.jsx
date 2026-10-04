import Link from 'next/link';
import { ticketsListHref } from '@/lib/ticketsListHref';
import { DEFAULT_TICKETS_LIST_SORT } from '@/lib/ticketsListQuery';

/** @typedef {import('../api/types.js').ListMeta} ListMeta */

const MAX_PAGE_LINKS = 20;

/**
 * @param {{ meta: ListMeta, q: string, status: string, size: number, sort?: string }} props
 */
export default function TicketListPagination({
  meta,
  q,
  status,
  size,
  sort = DEFAULT_TICKETS_LIST_SORT,
}) {
  const { page, totalPages, totalElements } = meta;
  const hasPrev = page > 0;
  const hasNext = page + 1 < totalPages;

  const linkQuery = { size, q, status, sort };

  const prevHref = ticketsListHref({ ...linkQuery, page: page - 1 });
  const nextHref = ticketsListHref({ ...linkQuery, page: page + 1 });

  const showPageLinks = totalPages > 1 && totalPages <= MAX_PAGE_LINKS;

  return (
    <nav className="ticket-pagination" aria-label="Ticket list pagination">
      <p className="ticket-pagination__summary">
        Page {page + 1} of {totalPages} ({totalElements} ticket
        {totalElements === 1 ? '' : 's'})
      </p>
      {showPageLinks ? (
        <ol className="ticket-pagination__pages">
          {Array.from({ length: totalPages }, (_, index) => {
            const label = index + 1;
            if (index === page) {
              return (
                <li key={index}>
                  <span className="ticket-pagination__page ticket-pagination__page--current" aria-current="page">
                    {label}
                  </span>
                </li>
              );
            }
            return (
              <li key={index}>
                <Link
                  href={ticketsListHref({ ...linkQuery, page: index })}
                  className="ticket-pagination__page"
                >
                  {label}
                </Link>
              </li>
            );
          })}
        </ol>
      ) : null}
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
