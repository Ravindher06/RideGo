export default function ArchitecturePage() {
  const services = [
    ["Customer Service", "8081", "Profiles and customer registration", "PostgreSQL"],
    ["Rider Service", "8082", "Rider, vehicle and availability", "PostgreSQL"],
    ["Booking Service", "8083", "Ride lifecycle and dispatch orchestration", "PostgreSQL"],
    ["Location Service", "8084", "Live coordinates and nearby-rider search", "Redis GEO"],
    ["Payment Service", "8085", "Simulated payment and idempotent settlement", "PostgreSQL"],
    ["Notification Service", "8086", "In-app ride/payment notifications", "PostgreSQL"],
  ];

  return (
    <main className="architecture-shell">
      <header className="hero">
        <div>
          <span className="badge">RIDEGO ARCHITECTURE</span>
          <h1>Six services, one ride lifecycle.</h1>
          <p>Each service owns its responsibility and data. Booking coordinates the flow through REST; Location uses Redis GEO for nearby riders.</p>
        </div>
        <a href="/" className="outline-button">Back to demo</a>
      </header>
      <section className="arch-flow">
        <div className="flow-node">Next.js<br/><small>Frontend :3000</small></div>
        <span>→</span><div className="flow-node">REST APIs</div><span>→</span>
        <div className="flow-node highlight">Booking :8083<br/><small>Orchestrator</small></div>
        <span>→</span><div className="flow-node">Customer / Rider / Location / Payment / Notification</div>
      </section>
      <section className="service-grid">
        {services.map(([name, port, responsibility, data]) => (
          <article className="service-card" key={name}>
            <div className="service-port">:{port}</div><h2>{name}</h2><p>{responsibility}</p><strong>{data}</strong>
          </article>
        ))}
      </section>
      <section className="card">
        <h2>Ride lifecycle</h2>
        <div className="lifecycle">{["CREATED","SEARCHING","RIDER_ASSIGNED","RIDER_ACCEPTED","RIDER_ARRIVED","OTP_VERIFIED","STARTED","COMPLETED"].map((s,i)=><span key={s}>{i>0 && "→ "}{s}</span>)}</div>
        <p className="small">Cancellation is supported before a ride starts. Rider reservation uses an atomic ONLINE → BUSY transition. Payment uses an idempotency key so a retry does not create a second payment.</p>
      </section>
      <footer>Spring Boot 3 • Java 17 • PostgreSQL • Redis • Next.js • Docker Compose</footer>
    </main>
  );
}
