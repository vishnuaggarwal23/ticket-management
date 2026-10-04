'use client';

import Link from 'next/link';
import { useState } from 'react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { ApiError } from '@/api/client';
import { askQuestion } from '@/api/ask';
import ErrorBanner from '@/components/ErrorBanner';
import { mapFieldErrors } from '@/lib/mapFieldErrors';
import { ASK_QUESTION_MAX_LENGTH } from '@/lib/askConstraints';

const EXAMPLE_QUESTIONS = [
  'What payment or checkout issues were reported?',
  'Which tickets mention a gateway timeout?',
  'Summarize open high-priority work.',
];

/**
 * @param {{ initialQuestion?: string }} props
 */
export default function AskPanel({ initialQuestion = '' }) {
  const [question, setQuestion] = useState(initialQuestion);
  const [questionError, setQuestionError] = useState('');
  const [requestError, setRequestError] = useState('');
  const [loading, setLoading] = useState(false);
  /** @type {[{ answer: string, citedTicketIds: string[] } | null, Function]} */
  const [result, setResult] = useState(null);

  /**
   * @param {string} raw
   */
  async function runAsk(raw) {
    setRequestError('');
    setQuestionError('');
    setResult(null);

    const trimmed = raw.trim();
    setQuestion(trimmed);

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
        const isProxyTimeout =
          error.status === 500 &&
          (error.code === 'BAD_RESPONSE' || error.code === 'UNKNOWN') &&
          /internal server error/i.test(error.message);
        setRequestError(
          isProxyTimeout
            ? 'The assistant took too long. Set NEXT_PUBLIC_API_BASE_URL in .env.local (see .env.example) or use a faster chat model.'
            : error.message,
        );
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

  async function handleSubmit(event) {
    event.preventDefault();
    await runAsk(question);
  }

  const isNoMatch = result && result.citedTicketIds.length === 0;

  return (
    <div className="ask-page">
      <Breadcrumbs items={[{ label: 'Tickets', href: '/tickets' }, { label: 'Ask' }]} />

      <header className="page-header page-header--compact">
        <div>
          <h1>Ask about tickets</h1>
          <p className="page-header__lede">
            Pick an example to ask in one click, or type your own question.
          </p>
        </div>
      </header>

      <div className="panel ask-form-panel">
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
              {question.length}/{ASK_QUESTION_MAX_LENGTH} · Enter to submit
            </p>
            {questionError ? (
              <span id="ask-question-error" className="field-error" role="alert">
                {questionError}
              </span>
            ) : null}
          </div>
          <div className="ask-suggestions" aria-label="Example questions">
            <p className="ask-suggestions__label">One-click examples</p>
            <ul className="ask-suggestions__list">
              {EXAMPLE_QUESTIONS.map((sample) => (
                <li key={sample}>
                  <button
                    type="button"
                    className="chip chip--action"
                    disabled={loading}
                    onClick={() => runAsk(sample)}
                  >
                    {loading ? 'Asking…' : sample}
                  </button>
                </li>
              ))}
            </ul>
          </div>
          <button type="submit" className="button" disabled={loading}>
            {loading ? 'Asking…' : 'Ask'}
          </button>
        </form>
      </div>

      {loading && !result ? (
        <div className="loading-block loading-block--inline">
          <span className="loading-block__spinner" aria-hidden="true" />
          <p className="loading-block__message">Searching tickets and drafting an answer…</p>
        </div>
      ) : null}

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
                    <Link href={`/tickets/${id}`} className="citation-link">
                      {id}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="ask-citations__empty">
              No ticket citations—this response is an honest no-match, not a verified fact from
              your queue.
            </p>
          )}
        </section>
      ) : null}
    </div>
  );
}
