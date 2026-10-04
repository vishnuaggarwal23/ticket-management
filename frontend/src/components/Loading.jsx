/**
 * @param {{ message?: string }} props
 */
export default function Loading({ message = 'Loading…' }) {
  return (
    <p className="loading" role="status" aria-live="polite">
      {message}
    </p>
  );
}
