/** @typedef {import('../api/types.js').TicketDetail} TicketDetail */
/** @typedef {import('../api/types.js').UpdateTicketRequest} UpdateTicketRequest */

const EDITABLE_KEYS = [
  'title',
  'description',
  'priority',
  'assignee',
  'category',
  'resolutionNotes',
];

/**
 * @param {TicketDetail} baseline
 * @param {Record<string, string>} values
 * @returns {UpdateTicketRequest}
 */
export function buildChangedTicketPatch(baseline, values) {
  /** @type {UpdateTicketRequest} */
  const patch = {};

  if (values.title !== baseline.title) {
    patch.title = values.title;
  }
  if (values.description !== baseline.description) {
    patch.description = values.description;
  }
  if (values.priority !== baseline.priority) {
    patch.priority = values.priority;
  }

  const baselineAssignee = baseline.assignee ?? '';
  const nextAssignee = values.assignee.trim();
  if (nextAssignee !== baselineAssignee) {
    patch.assignee = nextAssignee === '' ? null : nextAssignee;
  }

  const baselineCategory = baseline.category ?? '';
  const nextCategory = values.category;
  if (nextCategory !== baselineCategory) {
    patch.category = nextCategory === '' ? null : nextCategory;
  }

  const baselineNotes = baseline.resolutionNotes ?? '';
  const nextNotes = values.resolutionNotes;
  if (nextNotes !== baselineNotes) {
    patch.resolutionNotes = nextNotes === '' ? null : nextNotes;
  }

  return patch;
}

/**
 * @param {TicketDetail} ticket
 * @returns {Record<string, string>}
 */
export function ticketToFormValues(ticket) {
  return {
    title: ticket.title,
    description: ticket.description ?? '',
    priority: ticket.priority,
    assignee: ticket.assignee ?? '',
    category: ticket.category ?? '',
    resolutionNotes: ticket.resolutionNotes ?? '',
  };
}

export { EDITABLE_KEYS };
