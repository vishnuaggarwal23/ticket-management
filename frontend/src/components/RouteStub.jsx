/**
 * Placeholder until a UI slice wires real behaviour.
 * @param {{ title: string, hint?: string }} props
 */
export default function RouteStub({ title, hint }) {
  return (
    <section className="route-stub">
      <h1>{title}</h1>
      {hint ? <p>{hint}</p> : null}
      <p className="route-stub__soon">Coming soon.</p>
    </section>
  );
}
