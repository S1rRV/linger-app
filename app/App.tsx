import * as Clipboard from "expo-clipboard";
import { StatusBar } from "expo-status-bar";
import { useState } from "react";
import {
  Pressable,
  ScrollView,
  SectionList,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from "react-native";

import trip from "./assets/trip.json";
import type { Event, Reference, Trip } from "./trip";

const itinerary = trip as Trip;

/**
 * The day view, which is the only thing this app draws.
 *
 * Every time, title and grouping in here was worked out by the Kotlin domain
 * and written to assets/trip.json. Nothing is computed on the phone, and that
 * is deliberate: a screen that did its own date arithmetic could disagree with
 * the 112 tests that decide what a trip is, and it would be the screen that was
 * wrong.
 */
export default function App() {
  const dark = useColorScheme() === "dark";
  const theme = dark ? palette.dark : palette.light;

  return (
    <View style={[styles.screen, { backgroundColor: theme.background }]}>
      <StatusBar style={dark ? "light" : "dark"} />
      <SectionList
        sections={itinerary.days.map((day) => ({ title: day.date, data: day.events }))}
        keyExtractor={(event, index) => `${event.at}-${index}`}
        stickySectionHeadersEnabled
        ListHeaderComponent={<Header theme={theme} />}
        ListFooterComponent={<Footer theme={theme} />}
        renderSectionHeader={({ section }) => <DayHeading date={section.title} theme={theme} />}
        renderItem={({ item }) => <EventRow event={item} theme={theme} />}
        contentContainerStyle={styles.list}
      />
    </View>
  );
}

function Header({ theme }: { theme: Theme }) {
  return (
    <View style={styles.header}>
      <Text style={[styles.tripName, { color: theme.text }]}>{itinerary.name}</Text>
      <Text style={[styles.tripRange, { color: theme.dim }]}>
        {readableRange(itinerary.startsAt, itinerary.endsAt)}
      </Text>
      {itinerary.waypoints.length > 0 && (
        <Text style={[styles.waypoints, { color: theme.dim }]}>
          via {itinerary.waypoints.join(", ")}
        </Text>
      )}
    </View>
  );
}

function DayHeading({ date, theme }: { date: string; theme: Theme }) {
  return (
    <View style={[styles.dayHeading, { backgroundColor: theme.background }]}>
      <Text style={[styles.dayText, { color: theme.accent }]}>{readableDay(date)}</Text>
      <View style={[styles.rule, { backgroundColor: theme.line }]} />
    </View>
  );
}

function EventRow({ event, theme }: { event: Event; theme: Theme }) {
  return (
    <View style={styles.row}>
      <View style={styles.timeColumn}>
        <Text style={[styles.clock, { color: theme.text }]}>{event.clock}</Text>
        <Text style={[styles.zone, { color: theme.faint }]}>{zoneLabel(event.zone)}</Text>
      </View>
      <View style={[styles.spine, { backgroundColor: theme.line }]}>
        <View
          style={[
            styles.dot,
            {
              backgroundColor: event.kind === "flight" ? theme.accent : theme.background,
              borderColor: theme.accent,
            },
          ]}
        />
      </View>
      <View style={styles.body}>
        <Text style={[styles.title, { color: theme.text }]}>{event.title}</Text>
        <Text style={[styles.detail, { color: theme.dim }]}>{event.detail}</Text>
        {event.references.map((reference) => (
          <CopyableCode key={`${reference.issuer}-${reference.code}`} reference={reference} theme={theme} />
        ))}
      </View>
    </View>
  );
}

/**
 * A reference number, tapped to copy.
 *
 * This is the thing the app exists for on the day: standing at a counter, the
 * number is on the screen already in your hand, and getting it into a web form
 * is one tap rather than a retype.
 */
function CopyableCode({ reference, theme }: { reference: Reference; theme: Theme }) {
  const [copied, setCopied] = useState(false);

  return (
    <Pressable
      onPress={async () => {
        await Clipboard.setStringAsync(reference.code);
        setCopied(true);
        setTimeout(() => setCopied(false), 1500);
      }}
      style={({ pressed }) => [
        styles.code,
        { borderColor: theme.line, backgroundColor: pressed ? theme.line : "transparent" },
      ]}
    >
      <Text style={[styles.codeIssuer, { color: theme.faint }]}>{reference.issuer}</Text>
      <Text style={[styles.codeValue, { color: theme.text }]}>{reference.code}</Text>
      <Text style={[styles.codeHint, { color: copied ? theme.accent : theme.faint }]}>
        {copied ? "copied" : "tap to copy"}
      </Text>
    </Pressable>
  );
}

function Footer({ theme }: { theme: Theme }) {
  const { totals } = itinerary;
  const lines = Object.entries(totals.perCurrency);

  return (
    <View style={[styles.footer, { borderTopColor: theme.line }]}>
      <Text style={[styles.footerHeading, { color: theme.faint }]}>WHAT IT COST</Text>
      {lines.map(([currency, stated]) => (
        <View key={currency} style={styles.totalRow}>
          <Text style={[styles.totalCurrency, { color: theme.dim }]}>{currency}</Text>
          <Text style={[styles.totalValue, { color: theme.text }]}>{stated}</Text>
        </View>
      ))}
      {totals.combined && totals.combined.isAGuess && (
        <Text style={[styles.estimateNote, { color: theme.faint }]}>
          An estimate. Tap to correct it once your statement arrives.
        </Text>
      )}
      {totals.bookingsWithNoPrice > 0 && (
        <Text style={[styles.estimateNote, { color: theme.faint }]}>
          {totals.bookingsWithNoPrice} booking(s) state no price, so this is not the whole trip.
        </Text>
      )}
    </View>
  );
}

/** "Wed 23 December" from "2026-12-23", without pulling in a date library. */
function readableDay(date: string): string {
  const parsed = new Date(`${date}T12:00:00Z`);
  const day = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][parsed.getUTCDay()];
  return `${day} ${parsed.getUTCDate()} ${months[parsed.getUTCMonth()]}`;
}

function readableRange(from: string, to: string): string {
  const start = new Date(from);
  const end = new Date(to);
  return `${start.getUTCDate()} ${months[start.getUTCMonth()]} to ${end.getUTCDate()} ${
    months[end.getUTCMonth()]
  }`;
}

/** "America/New_York" reads as "New York". */
function zoneLabel(zone: string): string {
  return zone.split("/").pop()!.replace(/_/g, " ");
}

const months = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

type Theme = typeof palette.light;

const palette = {
  light: {
    background: "#fbfaf8",
    text: "#1c1b19",
    dim: "#5c5952",
    faint: "#96918a",
    line: "#e4e0d9",
    accent: "#9c5518",
  },
  dark: {
    background: "#15140f",
    text: "#f4f1eb",
    dim: "#a8a39a",
    faint: "#6e6a63",
    line: "#2e2c26",
    accent: "#e09a4c",
  },
};

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { paddingHorizontal: 16, paddingBottom: 48 },
  header: { paddingTop: 64, paddingBottom: 12 },
  tripName: { fontSize: 32, fontWeight: "700", letterSpacing: -0.5 },
  tripRange: { fontSize: 15, marginTop: 4 },
  waypoints: { fontSize: 13, marginTop: 2, fontStyle: "italic" },
  dayHeading: { paddingTop: 22, paddingBottom: 8 },
  dayText: { fontSize: 13, fontWeight: "700", letterSpacing: 1.1, textTransform: "uppercase" },
  rule: { height: 1, marginTop: 8 },
  row: { flexDirection: "row", paddingVertical: 12 },
  timeColumn: { width: 62 },
  clock: { fontSize: 17, fontWeight: "600", fontVariant: ["tabular-nums"] },
  zone: { fontSize: 10, marginTop: 1 },
  spine: { width: 2, marginHorizontal: 14, alignItems: "center" },
  dot: { width: 11, height: 11, borderRadius: 6, borderWidth: 2, marginLeft: -4.5, marginTop: 5 },
  body: { flex: 1 },
  title: { fontSize: 17, fontWeight: "600" },
  detail: { fontSize: 14, marginTop: 2 },
  code: { borderWidth: 1, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 7, marginTop: 8, alignSelf: "flex-start" },
  codeIssuer: { fontSize: 10, letterSpacing: 0.6, textTransform: "uppercase" },
  codeValue: { fontSize: 15, fontWeight: "600", marginTop: 1, fontVariant: ["tabular-nums"] },
  codeHint: { fontSize: 10, marginTop: 2 },
  footer: { marginTop: 32, paddingTop: 16, borderTopWidth: 1 },
  footerHeading: { fontSize: 11, letterSpacing: 1.1, marginBottom: 8 },
  totalRow: { flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 },
  totalCurrency: { fontSize: 15 },
  totalValue: { fontSize: 17, fontWeight: "600", fontVariant: ["tabular-nums"] },
  estimateNote: { fontSize: 12, marginTop: 8, lineHeight: 17 },
});
