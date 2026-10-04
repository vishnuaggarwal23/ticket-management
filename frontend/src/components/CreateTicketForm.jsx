'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ApiError } from '@/api/client';
import { createTicket } from '@/api/tickets';
import Breadcrumbs from '@/components/Breadcrumbs';
import ErrorBanner from '@/components/ErrorBanner';
import { TICKET_CATEGORIES, formatCategoryLabel } from '@/lib/ticketCategories';
import { mapFieldErrors } from '@/lib/mapFieldErrors';
import { TICKET_PRIORITIES } from '@/lib/ticketPriorities';

const emptyForm = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  assignee: '',
  category: '',
};

export default function CreateTicketForm() {
  const router = useRouter();
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  /**
   * @param {React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>} event
   */
  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => {
      if (!prev[name]) {
        return prev;
      }
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }

  /**
   * @param {React.FormEvent<HTMLFormElement>} event
   */
  async function handleSubmit(event) {
    event.preventDefault();
    setFormError('');
    setFieldErrors({});
    setSubmitting(true);

    try {
      const payload = {
        title: form.title.trim(),
        description: form.description,
        priority: form.priority,
      };
      const assignee = form.assignee.trim();
      if (assignee) {
        payload.assignee = assignee;
      }
      if (form.category) {
        payload.category = form.category;
      }

      const created = await createTicket(payload);
      router.push(`/tickets/${created.id}`);
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        setFieldErrors(mapFieldErrors(error.details));
      } else {
        setFormError('Could not create ticket. Check that the API is running.');
      }
      setSubmitting(false);
    }
  }

  return (
    <div className="ticket-form-page">
      <Breadcrumbs
        items={[
          { label: 'Tickets', href: '/tickets' },
          { label: 'New ticket' },
        ]}
      />

      <header className="page-header page-header--compact">
        <div>
          <h1>New ticket</h1>
          <p className="page-header__lede">Title is required; everything else is optional.</p>
        </div>
        <Link href="/tickets" className="button button--secondary">
          Cancel
        </Link>
      </header>

      {formError ? <ErrorBanner message={formError} /> : null}

      <form className="ticket-form panel" onSubmit={handleSubmit} noValidate>
        <div className="ticket-form__field">
          <label htmlFor="title">
            Title <span className="label-required">(required)</span>
          </label>
          <input
            id="title"
            name="title"
            type="text"
            required
            value={form.title}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.title)}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
          />
          {fieldErrors.title ? (
            <span id="title-error" className="field-error" role="alert">
              {fieldErrors.title}
            </span>
          ) : null}
        </div>

        <div className="ticket-form__field">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            name="description"
            rows={5}
            value={form.description}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.description)}
          />
          {fieldErrors.description ? (
            <span className="field-error" role="alert">{fieldErrors.description}</span>
          ) : null}
        </div>

        <div className="ticket-form__row">
          <div className="ticket-form__field">
            <label htmlFor="priority">Priority</label>
            <select
              id="priority"
              name="priority"
              value={form.priority}
              onChange={handleChange}
            >
              {TICKET_PRIORITIES.map((value) => (
                <option key={value} value={value}>{value}</option>
              ))}
            </select>
          </div>

          <div className="ticket-form__field">
            <label htmlFor="category">Category</label>
            <select
              id="category"
              name="category"
              value={form.category}
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
          <label htmlFor="assignee">Assignee</label>
          <input
            id="assignee"
            name="assignee"
            type="text"
            value={form.assignee}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors.assignee)}
          />
          {fieldErrors.assignee ? (
            <span className="field-error" role="alert">{fieldErrors.assignee}</span>
          ) : null}
        </div>

        <div className="ticket-form__actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create ticket'}
          </button>
        </div>
      </form>
    </div>
  );
}
