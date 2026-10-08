import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "RAPIDO — RideGo — On-Demand Ride Booking",
  description: "A deliberate foundation for an on-demand ride booking backend. Explore Phase 1 service boundaries, databases, REST contracts, and the Spring Boot repository blueprint.",
  applicationName: "RAPIDO RideGo — On-Demand Ride Booking",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
