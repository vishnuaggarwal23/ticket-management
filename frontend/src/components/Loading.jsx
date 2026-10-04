/**
 * @param {{ message?: string }} props
 */
export default function Loading({ message = 'Loading…' }) {
  return (
    <div className="loading-block" role="status" aria-live="polite">
      <span className="loading-block__spinner" aria-hidden="true" />
      <p className="loading-block__message">{message}</p>
    </div>
  );
}
