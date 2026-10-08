"use client";

import { FormEvent, useMemo, useState } from "react";

type ApiEnvelope<T> = { success: boolean; message: string; data: T; timestamp?: string };
type Customer = { customerId: number; name: string; phone: string; email: string; walletBalance: number };
type Rider = { riderId: number; name: string; phone: string; email: string; status: string; walletBalance: number };
type Booking = {
  bookingId: number; customerId: number; riderId: number | null; pickupAddress: string;
  dropAddress: string; distance: number; estimatedTime: number; fare: number; otp?: string;
  bookingStatus: string;
};

const services = {
  customer: process.env.NEXT_PUBLIC_CUSTOMER_SERVICE_URL ?? "http://localhost:8081",
  rider: process.env.NEXT_PUBLIC_RIDER_SERVICE_URL ?? "http://localhost:8082",
  booking: process.env.NEXT_PUBLIC_BOOKING_SERVICE_URL ?? "http://localhost:8083",
};

async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) } });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body?.message ?? `Request failed (${response.status})`);
  return (body?.data ?? body) as T;
}

export default function RideDashboard() {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [rider, setRider] = useState<Rider | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [customerForm, setCustomerForm] = useState({ name: "Ravindher", phone: "", email: "" });
  const [riderForm, setRiderForm] = useState({ name: "Arjun", phone: "", email: "", drivingLicenseNumber: "" });
  const [vehicleForm, setVehicleForm] = useState({ vehicleNumber: "", vehicleType: "BIKE", vehicleBrand: "Hero", vehicleModel: "Splendor", vehicleColor: "Black", vehicleCapacity: 1, registrationNumber: "", insuranceNumber: "", pollutionCertificate: "" });
  const [rideForm, setRideForm] = useState({ pickupAddress: "Secunderabad, Hyderabad", dropAddress: "HITEC City, Hyderabad", pickupLatitude: 17.4399, pickupLongitude: 78.4983, dropLatitude: 17.4435, dropLongitude: 78.3772 });
  const [otp, setOtp] = useState("");
  const [requests, setRequests] = useState<Booking[]>([]);
  const [message, setMessage] = useState("Backend services are configured for local ports 8081–8086.");
  const [busy, setBusy] = useState(false);

  const active = useMemo(() => booking?.bookingStatus ?? "NO ACTIVE RIDE", [booking]);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    try { await action(); }
    catch (e) { setMessage(e instanceof Error ? e.message : "Something went wrong"); }
    finally { setBusy(false); }
  }

  function submitCustomer(e: FormEvent) {
    e.preventDefault();
    return run(async () => {
      const data = await api<Customer>(`${services.customer}/customers`, { method: "POST", body: JSON.stringify(customerForm) });
      setCustomer(data); setMessage(`Customer ${data.customerId} registered.`);
    });
  }

  function submitRider(e: FormEvent) {
    e.preventDefault();
    return run(async () => {
      const data = await api<Rider>(`${services.rider}/riders`, { method: "POST", body: JSON.stringify(riderForm) });
      setRider(data); setMessage(`Rider ${data.riderId} registered.`);
    });
  }

  function registerVehicle(e: FormEvent) {
    e.preventDefault();
    if (!rider) return setMessage("Register a rider first.");
    return run(async () => {
      await api(`${services.rider}/riders/${rider.riderId}/vehicle`, { method: "POST", body: JSON.stringify(vehicleForm) });
      const data = await api<Rider>(`${services.rider}/riders/${rider.riderId}`);
      setRider(data); setMessage("Vehicle registered.");
    });
  }

  function goOnline() {
    if (!rider) return setMessage("Register a rider first.");
    return run(async () => {
      const data = await api<Rider>(`${services.rider}/riders/${rider.riderId}/online`, { method: "PUT" });
      setRider(data); setMessage("Rider is ONLINE and ready for dispatch.");
    });
  }

  function bookRide(e: FormEvent) {
    e.preventDefault();
    if (!customer) return setMessage("Register a customer first.");
    return run(async () => {
      const data = await api<Booking>(`${services.booking}/bookings`, { method: "POST", body: JSON.stringify({ ...rideForm, customerId: customer.customerId }) });
      setBooking(data); setOtp(data.otp ?? ""); setMessage(`Booking ${data.bookingId} created with status ${data.bookingStatus}.`);
    });
  }

  function loadRequests() {
    if (!rider) return setMessage("Register a rider first.");
    return run(async () => {
      const data = await api<Booking[]>(`${services.booking}/bookings/rider/${rider.riderId}/requests`);
      setRequests(data); setMessage(`${data.length} assigned ride request(s) found.`);
    });
  }

  async function actionBooking(path: string, body?: unknown) {
    if (!booking || !rider) return setMessage("Create a booking and register a rider first.");
    const data = await api<Booking>(`${services.booking}/bookings/${booking.bookingId}${path}`, { method: body ? "POST" : "PUT", body: body ? JSON.stringify(body) : undefined });
    setBooking(data); setMessage(`Booking ${data.bookingId} is now ${data.bookingStatus}.`);
  }

  return (
    <main className="ride-shell">
      <header className="hero">
        <div>
          <span className="badge">RAPIDO • MICROSERVICES DEMO</span>
          <h1>RideGo</h1>
          <p>End-to-end ride booking using Java Spring Boot, PostgreSQL, Redis and six independent services.</p>
        </div>
        <a href="/architecture" className="outline-button">View architecture</a>
      </header>

      <div className="status">{busy ? "Working…" : message}</div>

      <section className="grid two">
        <article className="card">
          <h2>1. Customer</h2>
          <form onSubmit={submitCustomer}>
            <input placeholder="Name" value={customerForm.name} onChange={e => setCustomerForm({...customerForm,name:e.target.value})} required />
            <input placeholder="Phone" value={customerForm.phone} onChange={e => setCustomerForm({...customerForm,phone:e.target.value})} required />
            <input type="email" placeholder="Email" value={customerForm.email} onChange={e => setCustomerForm({...customerForm,email:e.target.value})} required />
            <button disabled={busy}>Register customer</button>
          </form>
          {customer && <p className="small">Customer #{customer.customerId} • {customer.name}</p>}
        </article>

        <article className="card">
          <h2>2. Rider</h2>
          <form onSubmit={submitRider}>
            {(["name","phone","email","drivingLicenseNumber"] as const).map(key => <input key={key} placeholder={key === "drivingLicenseNumber" ? "Driving license" : key[0].toUpperCase()+key.slice(1)} type={key==="email"?"email":"text"} value={riderForm[key]} onChange={e => setRiderForm({...riderForm,[key]:e.target.value})} required />)}
            <button disabled={busy}>Register rider</button>
          </form>
          {rider && <p className="small">Rider #{rider.riderId} • {rider.status}</p>}
        </article>
      </section>

      <section className="grid two">
        <article className="card">
          <h2>3. Vehicle & availability</h2>
          <form onSubmit={registerVehicle}>
            <input placeholder="Vehicle number" value={vehicleForm.vehicleNumber} onChange={e => setVehicleForm({...vehicleForm,vehicleNumber:e.target.value})} required />
            <input placeholder="Registration number" value={vehicleForm.registrationNumber} onChange={e => setVehicleForm({...vehicleForm,registrationNumber:e.target.value})} required />
            <div className="row"><input placeholder="Brand" value={vehicleForm.vehicleBrand} onChange={e => setVehicleForm({...vehicleForm,vehicleBrand:e.target.value})} /><input placeholder="Model" value={vehicleForm.vehicleModel} onChange={e => setVehicleForm({...vehicleForm,vehicleModel:e.target.value})} /></div>
            <button disabled={busy}>Register vehicle</button>
          </form>
          <button className="secondary" onClick={goOnline} disabled={busy || !rider}>Set rider ONLINE</button>
        </article>

        <article className="card">
          <h2>4. Request a ride</h2>
          <form onSubmit={bookRide}>
            <input value={rideForm.pickupAddress} onChange={e => setRideForm({...rideForm,pickupAddress:e.target.value})} required />
            <input value={rideForm.dropAddress} onChange={e => setRideForm({...rideForm,dropAddress:e.target.value})} required />
            <div className="row"><input type="number" step="any" value={rideForm.pickupLatitude} onChange={e => setRideForm({...rideForm,pickupLatitude:Number(e.target.value)})} /><input type="number" step="any" value={rideForm.pickupLongitude} onChange={e => setRideForm({...rideForm,pickupLongitude:Number(e.target.value)})} /></div>
            <div className="row"><input type="number" step="any" value={rideForm.dropLatitude} onChange={e => setRideForm({...rideForm,dropLatitude:Number(e.target.value)})} /><input type="number" step="any" value={rideForm.dropLongitude} onChange={e => setRideForm({...rideForm,dropLongitude:Number(e.target.value)})} /></div>
            <button disabled={busy || !customer}>Book ride</button>
          </form>
        </article>
      </section>

      <section className="card flow">
        <div className="flow-head"><div><span className="eyebrow">LIVE BOOKING</span><h2>{booking ? `Booking #${booking.bookingId}` : "No booking selected"}</h2></div><strong>{active}</strong></div>
        {booking && <div className="ride-summary"><span>{booking.pickupAddress}</span><b>→</b><span>{booking.dropAddress}</span><span>₹{booking.fare}</span><span>{booking.distance} km</span></div>}
        <div className="actions">
          <button className="secondary" onClick={loadRequests} disabled={busy || !rider}>Refresh rider requests</button>
          <button onClick={() => booking && rider && actionBooking(`/accept?riderId=${rider.riderId}`)} disabled={busy || !booking || !rider}>Accept</button>
          <button onClick={() => booking && rider && actionBooking(`/arrived?riderId=${rider.riderId}`)} disabled={busy || !booking || !rider}>Arrived</button>
          <input className="otp" placeholder="OTP" value={otp} onChange={e => setOtp(e.target.value)} />
          <button onClick={() => actionBooking("/verify-otp", { riderId: rider?.riderId, otp })} disabled={busy || !booking || !rider}>Start ride</button>
          <button onClick={() => booking && rider && actionBooking(`/complete?riderId=${rider.riderId}`)} disabled={busy || !booking || !rider}>Complete</button>
          <button className="danger" onClick={() => booking && actionBooking("/cancel")} disabled={busy || !booking}>Cancel</button>
        </div>
        {requests.length > 0 && <div className="requests">{requests.map(r => <button key={r.bookingId} className="request" onClick={() => { setBooking(r); setOtp(r.otp ?? ""); }}><b>#{r.bookingId}</b> {r.pickupAddress} → {r.dropAddress}</button>)}</div>}
      </section>

      <footer>Customer • Rider • Booking • Payment • Location • Notification · Spring Boot 3 · PostgreSQL · Redis</footer>
    </main>
  );
}
