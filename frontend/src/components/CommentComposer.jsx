'use client';

import { useState } from 'react';
import { ApiError } from '@/api/client';
import { addComment, getTicket } from '@/api/tickets';
import ErrorBanner from '@/components/ErrorBanner';
import { mapFieldErrors } from '@/lib/mapFieldErrors';

/**
 * @typedef {import('../api/types.js').TicketDetail} TicketDetail
 * @param {{ ticketId: string, onCommentAdded: (ticket: TicketDetail) => void }} props
 */
export default function CommentComposer({ ticketId, onCommentAdded }) {
  const [body, setBody] = useState('');
  const [bodyError, setBodyError] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError('');
    setBodyError('');

    const trimmed = body.trim();
    if (!trimmed) {
      setBodyError('Comment cannot be blank.');
      return;
    }

    setSubmitting(true);
    try {
      await addComment(ticketId, { body: trimmed });
      const detail = await getTicket(ticketId);
      onCommentAdded(detail);
      setBody('');
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        const fields = mapFieldErrors(error.details);
        if (fields.body) {
          setBodyError(fields.body);
        }
      } else {
        setFormError('Could not add comment.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="comment-composer" onSubmit={handleSubmit}>
      {formError ? <ErrorBanner message={formError} /> : null}
      <div className="ticket-form__field">
        <label htmlFor="comment-body">Add comment</label>
        <textarea
          id="comment-body"
          name="body"
          rows={3}
          value={body}
          onChange={(event) => {
            setBody(event.target.value);
            setBodyError('');
          }}
          aria-invalid={Boolean(bodyError)}
          aria-describedby={bodyError ? 'comment-body-error' : undefined}
        />
        {bodyError ? (
          <span id="comment-body-error" className="field-error" role="alert">
            {bodyError}
          </span>
        ) : null}
      </div>
      <button type="submit" className="button button--secondary" disabled={submitting}>
        {submitting ? 'Posting…' : 'Post comment'}
      </button>
    </form>
  );
}
