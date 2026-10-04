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
        No further transitions from <strong>{formatStatusLabel(currentStatus)}</strong>. This
        ticket is in a terminal state.
      </p>
    );
  }

  /**
   * @param {TicketStatus} target
   * @returns {string}
   */
  function buttonClass(target) {
    if (target === 'CANCELLED') {
      return 'button button--danger';
    }
    if (target === 'CLOSED') {
      return 'button button--success';
    }
    return 'button button--secondary';
  }

  return (
    <div className="status-actions" role="group" aria-label="Status transitions">
      {targets.map((target) => (
        <button
          key={target}
          type="button"
          className={buttonClass(target)}
          disabled={disabled}
          onClick={() => onTransition(target)}
        >
          Move to {formatStatusLabel(target)}
        </button>
      ))}
    </div>
  );
}
