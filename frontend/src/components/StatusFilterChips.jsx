import Link from 'next/link';
import { TICKET_STATUSES, formatStatusLabel } from '@/lib/ticketStatuses';
import { ticketsListHref } from '@/lib/ticketsListHref';

/**
 * One-click status filters (preserves keyword search).
 * @param {{ q: string, status: string }} props
 */
export default function StatusFilterChips({ q, status }) {
  return (
    <div className="filter-chips" role="group" aria-label="Quick status filter">
      <Link
        href={ticketsListHref({ q })}
        className={`filter-chip${status === '' ? ' filter-chip--active' : ''}`}
      >
        All
      </Link>
      {TICKET_STATUSES.map((value) => (
        <Link
          key={value}
          href={ticketsListHref({ q, status: value })}
          className={`filter-chip${status === value ? ' filter-chip--active' : ''}`}
        >
          {formatStatusLabel(value)}
        </Link>
      ))}
    </div>
  );
}
