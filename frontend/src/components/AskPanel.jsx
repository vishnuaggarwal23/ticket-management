'use client';

import Link from 'next/link';
import { useState } from 'react';
import { ApiError } from '@/api/client';
import { askQuestion } from '@/api/ask';
import ErrorBanner from '@/components/ErrorBanner';
import { mapFieldErrors } from '@/lib/mapFieldErrors';
import { ASK_QUESTION_MAX_LENGTH } from '@/lib/askConstraints';

export default function AskPanel() {
  const [question, setQuestion] = useState('');
  const [questionError, setQuestionError] = useState('');
  const [requestError, setRequestError] = useState('');
  const [loading, setLoading] = useState(false);
  /** @type {[{ answer: string, citedTicketIds: string[] } | null, Function]} */
  const [result, setResult] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setRequestError('');
    setQuestionError('');
    setResult(null);

    const trimmed = question.trim();
    if (!trimmed) {
      setQuestionError('Question cannot be blank.');
      return;
    }
    if (trimmed.length > ASK_QUESTION_MAX_LENGTH) {
      setQuestionError(`Question must be at most ${ASK_QUESTION_MAX_LENGTH} characters.`);
      return;
    }

    setLoading(true);
    try {
      const data = await askQuestion(trimmed);
      setResult(data);
    } catch (error) {
      if (error instanceof ApiError) {
        setRequestError(error.message);
        const fields = mapFieldErrors(error.details);
        if (fields.question) {
          setQuestionError(fields.question);
        }
      } else {
        setRequestError('Could not reach the assistant. Check that the API is running.');
      }
    } finally {
      setLoading(false);
    }
  }

  const isNoMatch = result && result.citedTicketIds.length === 0;

  return (
    <div className="ask-page">
      <header className="page-header">
        <h1>Ask about tickets</h1>
        <Link href="/tickets" className="button button--secondary">
          Ticket list
        </Link>
      </header>

      <p className="ask-page__intro">
        Ask natural-language questions over ticket history. Answers are grounded in stored
        tickets with cited ticket IDs, or an honest no-match when nothing relevant is found.
      </p>

      <form className="ask-form" onSubmit={handleSubmit}>
        <div className="ticket-form__field">
          <label htmlFor="ask-question">Question</label>
          <textarea
            id="ask-question"
            name="question"
            rows={4}
            value={question}
            maxLength={ASK_QUESTION_MAX_LENGTH}
            disabled={loading}
            onChange={(event) => {
              setQuestion(event.target.value);
              setQuestionError('');
            }}
            aria-invalid={Boolean(questionError)}
            aria-describedby={questionError ? 'ask-question-error' : 'ask-question-hint'}
          />
          <p id="ask-question-hint" className="field-hint">
            {question.length}/{ASK_QUESTION_MAX_LENGTH} characters
          </p>
          {questionError ? (
            <span id="ask-question-error" className="field-error" role="alert">
              {questionError}
            </span>
          ) : null}
        </div>
        <button type="submit" className="button" disabled={loading}>
          {loading ? 'Asking…' : 'Ask'}
        </button>
      </form>

      {requestError ? <ErrorBanner message={requestError} /> : null}

      {result ? (
        <section
          className={`assistant-response${isNoMatch ? ' assistant-response--no-match' : ''}`}
          aria-live="polite"
          aria-label="Assistant answer"
        >
          <h2 className="assistant-response__heading">Assistant</h2>
          <p className="assistant-response__answer">{result.answer}</p>
          {result.citedTicketIds.length > 0 ? (
            <div className="ask-citations">
              <h3 className="ask-citations__heading">Cited tickets</h3>
              <ul className="ask-citations__list">
                {result.citedTicketIds.map((id) => (
                  <li key={id}>
                    <Link href={`/tickets/${id}`}>{id}</Link>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="ask-citations__empty">No ticket citations for this answer.</p>
          )}
        </section>
      ) : null}
    </div>
  );
}
