'use client';

import Link from 'next/link';
import { useState } from 'react';
import { ApiError } from '@/api/client';
import { getTicket, patchTicket } from '@/api/tickets';
import CommentComposer from '@/components/CommentComposer';
import CommentList from '@/components/CommentList';
import ErrorBanner from '@/components/ErrorBanner';
import StatusTransitionButtons from '@/components/StatusTransitionButtons';
import {
  buildChangedTicketPatch,
  ticketToFormValues,
} from '@/lib/buildTicketPatch';
import { formatInstant } from '@/lib/formatDate';
import { mapFieldErrors } from '@/lib/mapFieldErrors';
import { TICKET_CATEGORIES, formatCategoryLabel } from '@/lib/ticketCategories';
import { TICKET_PRIORITIES } from '@/lib/ticketPriorities';
import { formatStatusLabel } from '@/lib/ticketStatuses';

/**
 * @typedef {import('../api/types.js').TicketDetail} TicketDetail
 * @typedef {import('../api/types.js').TicketStatus} TicketStatus
 * @param {{ initialTicket: TicketDetail }} props
 */
export default function TicketDetailPanel({ initialTicket }) {
  const [ticket, setTicket] = useState(initialTicket);
  const [values, setValues] = useState(() => ticketToFormValues(initialTicket));
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [transitionError, setTransitionError] = useState('');
  const [saveMessage, setSaveMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [transitioning, setTransitioning] = useState(false);

  function applyTicket(detail) {
    setTicket(detail);
    setValues(ticketToFormValues(detail));
  }

  /**
   * @param {React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>} event
   */
  function handleChange(event) {
    const { name, value } = event.target;
    setValues((prev) => ({ ...prev, [name]: value }));
    setSaveMessage('');
    setFieldErrors((prev) => {
      if (!prev[name]) {
        return prev;
      }
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }

  async function handleSave() {
    setFormError('');
    setTransitionError('');
    setSaveMessage('');
    setFieldErrors({});

    const patch = buildChangedTicketPatch(ticket, values);
    if (Object.keys(patch).length === 0) {
      setFormError('No changes to save.');
      return;
    }

    setSaving(true);
    try {
      const updated = await patchTicket(ticket.id, patch);
      applyTicket(updated);
      setSaveMessage('Changes saved.');
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        setFieldErrors(mapFieldErrors(error.details));
      } else {
        setFormError('Could not save changes.');
      }
    } finally {
      setSaving(false);
    }
  }

  /**
   * @param {TicketStatus} target
   */
  async function handleTransition(target) {
    setTransitionError('');
    setFormError('');
    setTransitioning(true);

    try {
      const updated = await patchTicket(ticket.id, { status: target });
      applyTicket(updated);
      setSaveMessage(`Status updated to ${formatStatusLabel(target)}.`);
    } catch (error) {
      if (error instanceof ApiError) {
        setTransitionError(error.message);
        if (error.status === 404 || error.code === 'ILLEGAL_TRANSITION') {
          try {
            const fresh = await getTicket(ticket.id);
            applyTicket(fresh);
          } catch {
            // keep current ticket if refetch fails
          }
        }
      } else {
        setTransitionError('Could not update status.');
      }
    } finally {
      setTransitioning(false);
    }
  }

  return (
    <article className="ticket-detail">
      <header className="ticket-detail__header">
        <div>
          <p className="ticket-detail__id">{ticket.id}</p>
          <span className="status-badge">{formatStatusLabel(ticket.status)}</span>
        </div>
        <Link href="/tickets" className="button button--secondary">
          Back to list
        </Link>
      </header>

      {formError ? <ErrorBanner message={formError} /> : null}
      {transitionError ? (
        <ErrorBanner message={transitionError} className="error-banner--prominent" />
      ) : null}
      {saveMessage ? <p className="form-success" role="status">{saveMessage}</p> : null}

      <div className="ticket-form">
        <div className="ticket-form__field">
          <label htmlFor="detail-title">Title</label>
          <input
            id="detail-title"
            name="title"
            type="text"
            value={values.title}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.title)}
          />
          {fieldErrors.title ? (
            <span className="field-error" role="alert">{fieldErrors.title}</span>
          ) : null}
        </div>

        <div className="ticket-form__row">
          <div className="ticket-form__field">
            <label htmlFor="detail-priority">Priority</label>
            <select
              id="detail-priority"
              name="priority"
              value={values.priority}
              onChange={handleChange}
            >
              {TICKET_PRIORITIES.map((value) => (
                <option key={value} value={value}>{value}</option>
              ))}
            </select>
          </div>

          <div className="ticket-form__field">
            <label htmlFor="detail-category">Category</label>
            <select
              id="detail-category"
              name="category"
              value={values.category}
              onChange={handleChange}
            >
              <option value="">—</option>
              {TICKET_CATEGORIES.map((value) => (
                <option key={value} value={value}>
                  {formatCategoryLabel(value)}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="ticket-form__field">
          <label htmlFor="detail-assignee">Assignee</label>
          <input
            id="detail-assignee"
            name="assignee"
            type="text"
            value={values.assignee}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.assignee)}
          />
          {fieldErrors.assignee ? (
            <span className="field-error" role="alert">{fieldErrors.assignee}</span>
          ) : null}
        </div>

        <div className="ticket-form__field">
          <label htmlFor="detail-description">Description</label>
          <textarea
            id="detail-description"
            name="description"
            rows={6}
            value={values.description}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.description)}
          />
          {fieldErrors.description ? (
            <span className="field-error" role="alert">{fieldErrors.description}</span>
          ) : null}
        </div>

        <div className="ticket-form__field">
          <label htmlFor="detail-resolutionNotes">Resolution notes</label>
          <textarea
            id="detail-resolutionNotes"
            name="resolutionNotes"
            rows={4}
            value={values.resolutionNotes}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.resolutionNotes)}
          />
          {fieldErrors.resolutionNotes ? (
            <span className="field-error" role="alert">
              {fieldErrors.resolutionNotes}
            </span>
          ) : null}
        </div>

        <div className="ticket-form__actions">
          <button
            type="button"
            className="button"
            onClick={handleSave}
            disabled={saving || transitioning}
          >
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </div>

      <section className="ticket-detail__section" aria-labelledby="status-heading">
        <h2 id="status-heading">Status</h2>
        <p className="ticket-detail__status-current">
          Current: <strong>{formatStatusLabel(ticket.status)}</strong>
        </p>
        <StatusTransitionButtons
          currentStatus={ticket.status}
          onTransition={handleTransition}
          disabled={saving || transitioning}
        />
      </section>

      <dl className="ticket-detail__meta">
        <div>
          <dt>Created</dt>
          <dd>{formatInstant(ticket.createdAt)}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatInstant(ticket.updatedAt)}</dd>
        </div>
      </dl>

      <section className="ticket-detail__section" aria-labelledby="comments-heading">
        <h2 id="comments-heading">Comments</h2>
        <CommentList comments={ticket.comments} />
        <CommentComposer ticketId={ticket.id} onCommentAdded={applyTicket} />
      </section>

      <footer className="ticket-detail__footer">
        <Link href="/ask">Ask about tickets</Link>
      </footer>
    </article>
  );
}
