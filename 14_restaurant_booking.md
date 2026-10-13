# Restaurant Table Booking System

## System Design Question

Design a scalable, low-latency restaurant reservation system. A user visits the app, enters when they want to dine, where they want to dine, and how many people are in their party. The system returns restaurants with suitable table availability, and the user can book or cancel a reservation.

The design must remain reliable during concurrent booking attempts and traffic spikes. In particular, it must prevent double-booking a table and avoid duplicate bookings or payments when requests are retried.

## Functional Requirements

- Search for restaurants using:
  - Location or search radius
  - Date and time
  - Party size
  - Optional filters such as cuisine, rating, or price
- Show restaurants with tables suitable for the requested party and time.
- Let a user select an available table and create a reservation.
- Let users view their reservations and cancel a reservation.
- Track reservation status, such as `pending`, `confirmed`, `cancelled`, and `completed`.
- Support restaurant details, table capacity, opening hours, and unavailable periods.
- If payment or a deposit is required, charge it as part of the booking flow and apply the restaurant's cancellation policy.

## Non-Functional Requirements

- **Low latency:** Restaurant search and availability results should return within **300 ms at p95** and **800 ms at p99** under normal peak load.
- **Fast booking:** Booking confirmation should complete within **1 second at p95**, excluding time spent waiting for an external payment provider.
- **Strong booking correctness:** A table must not be confirmed for overlapping reservations.
- **Payment safety:** Retries must not create duplicate bookings or charge a user more than once.
- **Scalability:** Handle many restaurants, tables, users, and concurrent searches and bookings. Scale horizontally to handle weekend and dinner-time spikes.
- **Availability:** Search should remain responsive during downstream degradation, while booking must fail safely if availability cannot be verified.
- **Reliability and auditability:** Record booking, payment, and cancellation state transitions so failures can be reconciled.
- **Fresh availability:** Search results should be recent, but the booking operation must always revalidate availability before confirmation.

## Important Constraints

- Search results are not a reservation. Availability must be checked and claimed atomically during booking.
- Use idempotency for booking and payment requests so a client retry returns the original result instead of creating another operation.
- Define how temporary holds, payment failures, timeouts, and abandoned checkouts affect table availability.
- Cancellation must safely transition the booking and release the table only when the cancellation is accepted under the policy.
- Do not let slow payment or restaurant services stall unrelated searches or bookings indefinitely; use bounded timeouts and a recoverable workflow.

## Scale and Load Discussion

Design for high read traffic and bursty writes:

- Many users may search for the same popular restaurants and dinner slots simultaneously.
- Booking traffic can spike around weekends, holidays, and reservation release times.
- Search and availability reads will substantially outnumber booking and cancellation writes.
- The system should scale across multiple cities and restaurants without making a single restaurant or time slot a global bottleneck.

During the interview, state reasonable traffic assumptions and estimate peak search requests, booking attempts, and data growth. Explain how the architecture behaves at peak load and how it can scale beyond the initial assumptions.

## Design Questions

1. What services and data stores would you use for restaurant search, availability, bookings, and payments?
2. How would you model restaurants, tables, operating hours, time slots, reservations, and payment state?
3. How would you prevent two concurrent users from booking the same table for overlapping times?
4. Would you use temporary inventory holds? If so, how would they expire and recover after a process failure?
5. How would you make booking and payment retries idempotent and prevent duplicate charges?
6. How would cancellation release inventory and coordinate any refund or cancellation fee?
7. How would you keep search and availability results fast without trusting stale cached data during booking?
8. How would you scale location-based search and availability reads for peak traffic?
9. How would the system recover from payment timeouts, database failures, duplicate events, or worker crashes?
10. What metrics, alerts, and reconciliation processes would detect overbooking, stuck holds, or payment/booking mismatches?

## Latency Targets and Design Guidance

- Target **300 ms p95 / 800 ms p99** for restaurant search and availability under normal peak load.
- Target **1 second p95** for booking confirmation, excluding external payment-provider latency.
- Use location-aware search indexes, caching, and precomputed availability where useful.
- Keep search independent of slow downstream services where possible; use bounded timeouts and graceful degradation.
- Clearly distinguish cached or estimated availability from availability confirmed for a booking.
- Revalidate and reserve inventory atomically before confirming a booking. Caching must never be the mechanism that prevents double booking.
- If availability or payment cannot be safely confirmed, return a clear pending or failure state rather than reporting a successful booking.
