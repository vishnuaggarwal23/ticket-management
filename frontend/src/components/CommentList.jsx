import { formatInstant } from '@/lib/formatDate';

/**
 * @typedef {import('../api/types.js').Comment} Comment
 * @param {{ comments: Comment[] }} props
 */
export default function CommentList({ comments }) {
  if (!comments.length) {
    return (
      <p className="comment-list__empty">
        No comments yet. Add the first update below—comments are included in search knowledge.
      </p>
    );
  }

  return (
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment-list__item">
          <time className="comment-list__meta" dateTime={comment.createdAt}>
            {formatInstant(comment.createdAt)}
          </time>
          <p className="comment-list__body">{comment.body}</p>
        </li>
      ))}
    </ul>
  );
}
