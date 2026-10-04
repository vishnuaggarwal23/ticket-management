'use client';

import { legalTargetsForStatus } from '@/lib/legalStatusTransitions';
import { formatStatusLabel } from '@/lib/ticketStatuses';

/**
 * @typedef {import('../api/types.js').TicketStatus} TicketStatus
 * @param {{
 *   currentStatus: TicketStatus,
 *   onTransition: (target: TicketStatus) => void,
 *   disabled?: boolean,
 * }} props
 */
export default function StatusTransitionButtons({
  currentStatus,
  onTransition,
  disabled = false,
}) {
  const targets = legalTargetsForStatus(currentStatus);

  if (targets.length === 0) {
    return (
      <p className="status-actions__none">
        No status transitions available for {formatStatusLabel(currentStatus)}.
      </p>
    );
  }

  return (
    <div className="status-actions" role="group" aria-label="Status transitions">
      {targets.map((target) => (
        <button
          key={target}
          type="button"
          className="button button--secondary"
          disabled={disabled}
          onClick={() => onTransition(target)}
        >
          Move to {formatStatusLabel(target)}
        </button>
      ))}
    </div>
  );
}
