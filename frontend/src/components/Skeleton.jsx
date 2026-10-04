/**
 * @param {{ className?: string, width?: string, height?: string }} props
 */
export function SkeletonLine({ className = '', width = '100%', height = '0.875rem' }) {
  return (
    <span
      className={`skeleton skeleton--line ${className}`.trim()}
      style={{ width, height }}
      aria-hidden="true"
    />
  );
}

export function ListPageSkeleton() {
  return (
    <div className="skeleton-page" aria-busy="true" aria-label="Loading tickets">
      <SkeletonLine width="40%" height="1.75rem" />
      <SkeletonLine width="70%" height="0.875rem" />
      <div className="skeleton-panel">
        <SkeletonLine width="100%" height="2.5rem" />
        <SkeletonLine width="60%" height="2.5rem" />
      </div>
      <div className="skeleton-table">
        {Array.from({ length: 6 }, (_, i) => (
          <SkeletonLine key={i} width="100%" height="2.75rem" />
        ))}
      </div>
    </div>
  );
}

export function DetailPageSkeleton() {
  return (
    <div className="skeleton-page" aria-busy="true" aria-label="Loading ticket">
      <SkeletonLine width="30%" height="1.25rem" />
      <div className="skeleton-panel">
        <SkeletonLine width="50%" height="1.5rem" />
        <SkeletonLine width="100%" height="2.5rem" />
        <SkeletonLine width="100%" height="6rem" />
        <SkeletonLine width="40%" height="2.5rem" />
      </div>
      <div className="skeleton-panel">
        <SkeletonLine width="35%" height="1.25rem" />
        <SkeletonLine width="100%" height="2.5rem" />
      </div>
    </div>
  );
}
