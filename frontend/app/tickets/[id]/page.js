import RouteStub from '@/components/RouteStub';

/**
 * @param {{ params: Promise<{ id: string }> }} props
 */
export default async function TicketDetailPage({ params }) {
  const { id } = await params;
  return (
    <RouteStub
      title={`Ticket ${id}`}
      hint="View and update ticket fields, comments, and status."
    />
  );
}
