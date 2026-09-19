/**
 * The shape the domain sends over.
 *
 * Mirrors app.linger.core.TripJson in the Kotlin module, which is the single
 * source of truth for what a trip is. Nothing here works anything out: if a
 * time or a title looks wrong on the phone, it is wrong in the domain and the
 * fix belongs there, with a test.
 */
export type Reference = {
  issuer: string;
  code: string;
};

export type Event = {
  /** The wall clock reading where the event happens. Never recomputed here. */
  clock: string;
  /** The zone that reading was taken in, so the screen can say whose time it is. */
  zone: string;
  /** The same moment as an instant, for sorting and counting down. */
  at: string;
  title: string;
  detail: string;
  place: string;
  kind: "flight" | "appointment";
  references: Reference[];
};

export type Day = {
  date: string;
  events: Event[];
};

export type Totals = {
  perCurrency: Record<string, string>;
  combined: { currency: string; stated: string; isAGuess: boolean } | null;
  bookingsWithNoPrice: number;
};

export type Trip = {
  name: string;
  startsAt: string;
  endsAt: string;
  destinations: string[];
  waypoints: string[];
  totals: Totals;
  days: Day[];
};
