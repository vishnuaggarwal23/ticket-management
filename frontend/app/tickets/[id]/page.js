import { getTicket } from '@/api/tickets';
import { ApiError } from '@/api/client';
import TicketDetailPanel from '@/components/TicketDetailPanel';
import TicketNotFound from '@/components/TicketNotFound';
import ErrorBanner from '@/components/ErrorBanner';

/**
 * @param {{ params: Promise<{ id: string }> }} props
 */
export default async function TicketDetailPage({ params }) {
  const { id } = await params;

  try {
    const ticket = await getTicket(id);
    return <TicketDetailPanel initialTicket={ticket} />;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return <TicketNotFound id={id} />;
    }
    if (error instanceof ApiError) {
      return (
        <ErrorBanner message={error.message} details={error.details} />
      );
    }
    return (
      <ErrorBanner
        message="Could not load ticket. Check that the API is running."
      />
    );
  }
}
