/**
 * @param {{
 *   message: string,
 *   details?: Array<{ field?: string, message?: string, code?: string }>,
 *   className?: string,
 * }} props
 */
export default function ErrorBanner({ message, details = [], className = '' }) {
  if (!message) {
    return null;
  }

  const classes = ['error-banner', className].filter(Boolean).join(' ');

  return (
    <div className={classes} role="alert">
      <p className="error-banner__message">{message}</p>
      {details.length > 0 && (
        <ul className="error-banner__details">
          {details.map((item, index) => {
            const text =
              item.field && item.message
                ? `${item.field}: ${item.message}`
                : item.message || item.field || item.code || 'Validation error';
            return (
              <li key={`${item.field ?? 'detail'}-${index}`}>{text}</li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
