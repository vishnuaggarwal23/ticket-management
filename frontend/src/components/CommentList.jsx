import { formatInstant } from '@/lib/formatDate';

/**
 * @typedef {import('../api/types.js').Comment} Comment
 * @param {{ comments: Comment[] }} props
 */
export default function CommentList({ comments }) {
  if (!comments.length) {
    return <p className="comment-list__empty">No comments yet.</p>;
  }

  return (
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment-list__item">
          <p className="comment-list__meta">{formatInstant(comment.createdAt)}</p>
          <p className="comment-list__body">{comment.body}</p>
        </li>
      ))}
    </ul>
  );
}
