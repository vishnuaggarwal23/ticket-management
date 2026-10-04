'use client';

import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/api/client';
import { getTicket, patchTicket } from '@/api/tickets';
import Breadcrumbs from '@/components/Breadcrumbs';
import CommentComposer from '@/components/CommentComposer';
import CommentList from '@/components/CommentList';
import ErrorBanner from '@/components/ErrorBanner';
import StatusTransitionButtons from '@/components/StatusTransitionButtons';
import {
  buildChangedTicketPatch,
  ticketToFormValues,
} from '@/lib/buildTicketPatch';
import { formatInstant } from '@/lib/formatDate';
import { isTicketFormDirty } from '@/lib/isTicketFormDirty';
import { mapFieldErrors } from '@/lib/mapFieldErrors';
import { TICKET_CATEGORIES, formatCategoryLabel } from '@/lib/ticketCategories';
import { TICKET_PRIORITIES } from '@/lib/ticketPriorities';
import { formatStatusLabel, statusBadgeClass } from '@/lib/ticketStatuses';

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
  const commentsRef = useRef(null);

  const isDirty = isTicketFormDirty(ticket, values);

  function applyTicket(detail) {
    setTicket(detail);
    setValues(ticketToFormValues(detail));
  }

  function handleCommentAdded(detail) {
    applyTicket(detail);
    requestAnimationFrame(() => {
      commentsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  function discardChanges() {
    setValues(ticketToFormValues(ticket));
    setFieldErrors({});
    setFormError('');
    setSaveMessage('');
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

  const handleSave = useCallback(async () => {
    setFormError('');
    setTransitionError('');
    setSaveMessage('');
    setFieldErrors({});

    const patch = buildChangedTicketPatch(ticket, values);
    if (Object.keys(patch).length === 0) {
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
  }, [ticket, values]);

  useEffect(() => {
    function onKeyDown(event) {
      if ((event.metaKey || event.ctrlKey) && event.key === 's') {
        event.preventDefault();
        if (isDirty && !saving && !transitioning) {
          handleSave();
        }
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [handleSave, isDirty, saving, transitioning]);

  /**
   * @param {TicketStatus} target
   */
  async function handleTransition(target) {
    setTransitionError('');
    setFormError('');

    if (isDirty) {
      setFormError('Save or discard your edits before changing status.');
      return;
    }

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
      <Breadcrumbs
        items={[
          { label: 'Tickets', href: '/tickets' },
          { label: ticket.id },
        ]}
      />

      <header className="ticket-detail__header panel">
        <div>
          <p className="ticket-detail__id">{ticket.id}</p>
          <span className={statusBadgeClass(ticket.status)}>
            {formatStatusLabel(ticket.status)}
          </span>
        </div>
        <Link
          href={`/ask?prefill=${encodeURIComponent(`What is the status of ${ticket.id}?`)}`}
          className="button button--secondary"
        >
          Ask about this
        </Link>
      </header>

      {formError ? <ErrorBanner message={formError} /> : null}
      {transitionError ? (
        <ErrorBanner message={transitionError} className="error-banner--prominent" />
      ) : null}
      {saveMessage ? (
        <p className="form-success" role="status">
          {saveMessage}
        </p>
      ) : null}

      {isDirty ? (
        <div className="sticky-actions" role="region" aria-label="Unsaved changes">
          <p className="sticky-actions__text">You have unsaved edits</p>
          <div className="sticky-actions__buttons">
            <button
              type="button"
              className="button"
              onClick={handleSave}
              disabled={saving || transitioning}
            >
              {saving ? 'Saving…' : 'Save'}
            </button>
            <button
              type="button"
              className="button button--secondary"
              onClick={discardChanges}
              disabled={saving || transitioning}
            >
              Discard
            </button>
          </div>
          <span className="sticky-actions__hint">⌘/Ctrl + S to save</span>
        </div>
      ) : null}

      <section className="ticket-detail__section panel" aria-labelledby="fields-heading">
        <h2 id="fields-heading">Details</h2>
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
              <span className="field-error" role="alert">
                {fieldErrors.title}
              </span>
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
                  <option key={value} value={value}>
                    {value}
                  </option>
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
              <span className="field-error" role="alert">
                {fieldErrors.assignee}
              </span>
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
              <span className="field-error" role="alert">
                {fieldErrors.description}
              </span>
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
        </div>
      </section>

      <section className="ticket-detail__section panel" aria-labelledby="status-heading">
        <h2 id="status-heading">Status</h2>
        <StatusTransitionButtons
          currentStatus={ticket.status}
          onTransition={handleTransition}
          disabled={saving || transitioning || isDirty}
        />
        {isDirty ? (
          <p className="field-hint">Save details above before moving status.</p>
        ) : null}
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

      <section
        ref={commentsRef}
        className="ticket-detail__section panel"
        aria-labelledby="comments-heading"
      >
        <h2 id="comments-heading">Comments</h2>
        <CommentList comments={ticket.comments} />
        <CommentComposer ticketId={ticket.id} onCommentAdded={handleCommentAdded} />
      </section>
    </article>
  );
}
