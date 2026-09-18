# Historical exchange rates for a frozen per-Booking conversion

Research date: 2026-09-18. All prices, limits and coverage figures below were
read on that date and are dated accordingly, because provider pricing changes
without notice.

Question: Linger stores a Booking's amount exactly as the vendor stated it and
freezes an exchange rate to the account's home currency at the moment the
Booking is first saved, so a Trip's combined total never moves
([ADR-0004](../adr/0004-freeze-the-exchange-rate.md), [Money in
DATA-MODEL.md](../DATA-MODEL.md)). The user asked for "Google's rate on the day
of purchase". There is no public Google FX API. So: what dated historical rate
source should Linger use, and is there a way to not need one at all?

Sources consulted, all primary: the ECB's own pages and its Data Portal API
(`data-api.ecb.europa.eu`), the Frankfurter project's documentation and its live
API, apilayer's three currency brands (exchangerate.host, Fixer, CurrencyLayer)
on their own sites, Open Exchange Rates' own signup and documentation pages,
ExchangeRate-API's own docs, Twelve Data's own pricing page, Wise's own API
reference, the US Federal Reserve H.10 release pages, the Reserve Bank of
India's own reference rate pages and press releases, the Bank of England's legal
page, the Bank of Canada Valet API, Colombia's national open data portal
(`datos.gov.co`), and the *Visa Core Rules and Visa Product and Service Rules*
(18 April 2026 edition) as a PDF from visa.com. Live API calls were made from
this environment and their responses are quoted verbatim where they establish
behaviour the documentation does not state.

Anything a source would not confirm, or that could not be reached from this
environment, is collected in "Ambiguities and unconfirmed items" at the end.

---

## 1. There is no single number called "the rate"

This has to come first, because it decides how much accuracy is worth buying.

Frankfurter's v2 API blends many official sources and can be asked to show its
working. Asked for USD to INR on 2026-03-10, with `expand=providers`:

```
GET https://api.frankfurter.dev/v2/rate/usd/inr?date=2026-03-10&expand=providers
```

54 official sources contributed. One (Da Afghanistan Bank, at 85.29) was flagged
`excluded` by Frankfurter's own outlier handling. Of the 53 it kept, the lowest
was 91.40 and the highest 93.00, a spread of 1.75 percent, with a median of
91.96. The blended answer returned was 92.04. The ECB's own reading for the same
pair and date, requested on its own at
`https://api.frankfurter.dev/v2/providers/ecb/rate/usd/inr?date=2026-03-10`, was
91.96.

So on a single ordinary Tuesday, fifty-three central banks publishing an
official daily rate for the same pair disagreed by 1.75 percent. A traveller's
card issuer will differ again, and by more. Any claim that Linger's combined
total is "correct" to better than a percent or two is not supportable from any
source found here.

Frankfurter says as much itself: "For compliance, filter by a specific provider
to get official reference rates. For general use, the default blended rates work
well, though the last decimal places may shift as new data comes in"
([frankfurter.dev](https://frankfurter.dev/)). "May shift" is fatal for a frozen
rate if taken literally, and section 3 shows the shift is larger than "the last
decimal places" in practice.

### What the traveller actually paid is a different number again

The *Visa Core Rules and Visa Product and Service Rules*, 18 April 2026 edition,
defines the Currency Conversion Rate in its glossary (page 851) as:

> "A rate set by Visa from the range of rates available (not necessarily
> executed) in wholesale currency markets for the applicable Transaction, which
> rate may vary from the rate Visa itself receives; or the rate mandated by a
> government or a governing body in the country in which the Transaction
> occurred."

and continues:

> "The Visa rate may be adjusted by application of an Optional Issuer Fee as
> determined by the Issuer when VisaNet converts the Transaction Currency to the
> Billing Currency."

with "Optional Issuer Fee" defined (page 880) as "A fee that an Issuer may
charge a Cardholder by the application of a percentage increase to the Currency
Conversion Rate". And finally:

> "An Issuer shall set the conversion rate to its Cardholder and an Acquirer
> shall set the conversion rate to its Merchant, as specified in applicable laws
> and regulations."

([Visa Core Rules and Visa Product and Service Rules, 18 April
2026](https://usa.visa.com/dam/VCOM/download/about-visa/visa-rules-public.pdf),
glossary pages 851 and 880.)

Three consequences, all load bearing for section 4:

1. The rate the traveller was charged is set by their issuing bank, not by Visa
   and certainly not by any central bank. It is not knowable from outside.
2. It carries a percentage markup that Visa itself does not fix.
3. Conversion happens at clearing, not at purchase. Rule 7.5.2.1 says "Visa
   converts the Transaction Currency to the Issuer's or Acquirer's Settlement
   Currency using the Currency Conversion Rate" and sits under section 7.5,
   "Clearing" (same PDF, page 566). The glossary defines Processing Date as "The
   date (based on Greenwich Mean Time) on which the Member submitted, and Visa
   accepted, Interchange data" (page 888). A hotel authorised on a Friday and
   cleared on a Tuesday converts at Tuesday's rate, not Friday's.

ADR-0004 already says this ("The rate that actually applied is the card issuer's
on the day it settled, which we cannot see"). The Visa rules are the primary
source that backs it.

---

## 2. Candidate sources

### 2.1 Summary table

Read 2026-09-18. "Key in client" asks whether an Android app could call the
service directly without shipping a secret in the APK.

| Source | COP | INR | JPY | THB | CAD | GBP | EUR | History from | Key in client | Commercial | Price |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ECB reference rates (direct or via Data Portal API) | no | yes | yes | yes | yes | yes | base | 1999-01-04 | yes, no key | yes, with attribution | free |
| Frankfurter v1 (ECB only, deprecated) | no | yes | yes | yes | yes | yes | yes | 1999-01-04 | yes, no key | yes | free |
| Frankfurter v2 (98 sources blended) | yes | yes | yes | yes | yes | yes | yes | 1948-06-21 for USD | yes, no key | yes | free |
| Bank of Canada Valet | no | yes | yes | yes | base | yes | yes | 2017-01-03 | yes, no key | yes, with attribution | free |
| Bank of England (IADB) | no | yes | yes | yes | yes | base | yes | 2000-01-04 | yes, no key | database only, under OGL | free |
| Banco de la Republica TRM, via datos.gov.co | base | no | no | no | no | no | no | 2000-01-04 | yes, no key | yes, CC BY-SA 4.0 | free |
| US Federal Reserve H.10 | no | yes | yes | yes | yes | yes | yes | see 2.7 | not confirmed | not stated | free |
| IMF representative rates | yes | yes | yes | yes | yes | yes | yes | 2003-04-01 | not confirmed | not confirmed | not confirmed |
| Open Exchange Rates | yes | yes | yes | yes | yes | yes | yes | 1999-01-01 | no, `app_id` required | paid tiers | $0 / $12 / $47 / $97 per month |
| exchangerate.host (apilayer) | not confirmed | yes | yes | yes | yes | yes | yes | ~19 years claimed | no, `access_key` required | paid tiers only | $0 / $14.99 / $59.99 / $99.99 per month |
| Fixer (apilayer) | not confirmed | yes | yes | yes | yes | yes | yes | 1999-01-01 | no, `access_key` required | paid tiers only | $0 / $14.99 / $59.99 / $99.99 per month |
| CurrencyLayer (apilayer) | not confirmed | yes | yes | yes | yes | yes | yes | 1999 | no, `access_key` required | paid tiers | $0 / $14.99 / $59.99 / $99.99 per month |
| ExchangeRate-API | yes | yes | yes | yes | yes | yes | yes | 1990 for 35 codes, 2021 for all | no, key in path | yes | $0 / $10 / $30 per month |
| Twelve Data | not confirmed | yes | yes | yes | yes | yes | yes | not stated | no, key required | individual plans are non-commercial | $0 / $29 / $99 / $329 per month |
| Wise Rate API | not confirmed | yes | yes | yes | yes | yes | yes | not stated | no, bearer token | partner agreement | not published |

### 2.2 ECB euro foreign exchange reference rates

The ECB publishes a daily reference set against the euro. Its own page states
the rates are "usually updated at around 16:00 CET every working day" and are
not published "on TARGET closing days"
([ECB](https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html)).
The Data Portal API returns a series title complement of "ECB reference exchange
rate, US dollar/Euro, 2.15 pm (C.E.T.)", which is the fixing time as distinct
from the publication time (observed in the response to
`https://data-api.ecb.europa.eu/service/data/EXR/D.USD.EUR.SP00.A?startPeriod=2024-06-14&endPeriod=2024-06-17`).

The ECB is explicit that these are not transaction rates: "published for
information purposes only. Using the rates for transaction purposes is strongly
discouraged" (same page). For Linger's stated purpose, an estimate labelled as
an estimate, that is fine, and the caveat is worth repeating in the UI copy.

**Currency list, read 2026-09-18.** 30 currencies plus the euro: USD, JPY, CZK,
DKK, GBP, HUF, PLN, RON, SEK, CHF, ISK, NOK, TRY, AUD, CNY, HKD, IDR, INR, KRW,
MYR, NZD, PHP, SGD, THB, BRL, CAD, MXN, ZAR (plus BGN and a few others shown in
the table). **COP is not in it.** INR is (109.8755 per euro on the day read).
THB is. Verified independently against the live Frankfurter v1 currency list,
which mirrors the ECB set exactly and returns 30 codes plus EUR
(`https://api.frankfurter.dev/v1/currencies`).

**Licence.** The ECB's disclaimer page permits reuse: "When such information is
distributed or reproduced, it must appear accurately and the ECB must be cited
as the source", with the added condition that if it is put into documents sold
commercially, buyers must be told it is available free on the ECB website
([ECB
disclaimer](https://www.ecb.europa.eu/services/using-our-site/disclaimer/html/index.en.html)).
So: free, commercial use allowed, attribution mandatory.

**Access.** Two ways, both keyless.

- The Data Portal API at `https://data-api.ecb.europa.eu/service/data/EXR/...`
  returned HTTP 200 with data and no authentication from this environment. The
  Data Portal API overview page
  (`https://data.ecb.europa.eu/help/api/overview`) returned HTTP 503 when
  fetched, so documented rate limits could not be read; see ambiguities.
- A bulk historical CSV. `https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.zip`
  downloaded in one request at **639,491 bytes**, containing a single CSV of
  **1,922,007 bytes** with **7,096 daily rows** from 1999-01-04 to 2026-09-18
  and 41 currency columns. Currencies not published on a given date appear as
  `N/A`. This file is the basis of the bundled-table option in section 4.5.

### 2.3 Frankfurter

An MIT-licensed open source API, self-hostable, with a free public instance.
Its own summary: "Currency data API. Current and historical exchange rates for
206 currencies from 98 central banks and official sources. No authentication
required. Open-source. Genuinely free, not 'free with limits.'"
([frankfurter.dev/llms.txt](https://frankfurter.dev/llms.txt)). Licence MIT
([GitHub](https://github.com/lineofflight/frankfurter)).

**Limits.** "There are no quotas. Requests are rate-limited to prevent abuse,
but there are no monthly or daily caps. For high-volume use, consider caching
responses, self-hosting, or querying the datasets directly"
([frankfurter.dev](https://frankfurter.dev/)).

**Commercial use.** "Is the API free for commercial use? Yes, absolutely. See
each provider's terms for details on the underlying data" (same page). Note the
second sentence: Frankfurter passes the licensing question through to the
upstream central banks, which means Linger inherits, for example, the ECB's
attribution requirement.

**Privacy, which matters for the "No data out" guardrail.** "The API does not
collect or log personal data, IP addresses, or request URLs." And: "Do I need to
disclose the API in App Store or Google Play privacy labels? No. Because queries
are anonymous and no request logs are retained, requests qualify as Data Not
Collected under Apple and Google Play guidelines" (same page). This is the
provider's own claim about its own logging, not an independent audit, but it is
the strongest such statement found among all candidates.

**Two versions, and they are very different.**

- **v1** serves the ECB set only, keyless, nested `rates` object. It is
  deprecated: a live response to `https://api.frankfurter.dev/v1/latest` carried
  the headers `deprecation: @1779103800` (2026-05-18) and
  `link: <https://api.frankfurter.dev/v2/rates>; rel="successor-version"`. The
  FAQ says it "remains available indefinitely"
  ([frankfurter.dev](https://frankfurter.dev/)).
- **v2** blends 98 providers. `https://api.frankfurter.dev/v2/currencies`
  returned **166 currency entries** with per-currency `start_date` and
  `end_date`. Relevant entries, read 2026-09-18:

  | Code | Name | Earliest |
  | --- | --- | --- |
  | USD | United States Dollar | 1948-06-21 |
  | GBP | British Pound | 1949-12-21 |
  | CAD | Canadian Dollar | 1953-05-11 |
  | JPY | Japanese Yen | 1969-12-01 |
  | THB | Thai Baht | 1990-12-31 |
  | INR | Indian Rupee | 1994-03-01 |
  | COP | Colombian Peso | 1998-07-07 |
  | EUR | Euro | 1999-01-04 |

  **COP is covered in v2 and not in v1.** That is the single fact that decides
  between them for Linger, given the sample trip.

The v2 rate endpoints are `GET /v2/rates` and `GET /v2/rate/{base}/{quote}`,
with `date`, `from`, `to`, `base`, `quotes`, `providers`, `group` and `expand`
parameters
([OpenAPI](https://api.frankfurter.dev/v2/openapi.json),
[frankfurter.dev](https://frankfurter.dev/)). A single provider can be pinned:
`GET /v2/providers/ecb/rate/usd/gbp?date=2024-06-14`. Rates are mid-market:
"The API returns mid-market rates. When a source publishes bid and ask quotes
instead of a reference rate, Frankfurter stores both and calculates the
midpoint" ([frankfurter.dev](https://frankfurter.dev/)).

Provider metadata is queryable at `https://api.frankfurter.dev/v2/providers`,
which returned 98 entries, each with a `terms_url`, a `pivot_currency`, a
`publish_cadence` and a currency list. Counting providers per currency from that
response: GBP 92, EUR 91, JPY 88, CAD 82, INR 59, THB 46, **COP 13**. COP is the
thinnest of Linger's target currencies by a wide margin, and section 3.3 shows
what that thinness does.

### 2.4 apilayer: exchangerate.host, Fixer and CurrencyLayer are one company

All three sites say so on their own pages. exchangerate.host: operated by
"APILayer, a technology company that provides a variety of reliable and
essential APIs for developers", headquartered in Vienna
([exchangerate.host](https://exchangerate.host/)). Fixer names APILayer as its
operator ([fixer.io](https://fixer.io/)). CurrencyLayer likewise
([currencylayer.com/product](https://currencylayer.com/product)). The "APILayer"
string appears in the page footer of all three when fetched directly.

They also behave as one service. Called without credentials, all three returned
a byte-identical error body:

```
{"success":false,"error":{"code":101,"type":"missing_access_key",
 "info":"You have not supplied an API Access Key. [Required format: access_key=YOUR_ACCESS_KEY]"}}
```

from `https://api.exchangerate.host/list`, `https://data.fixer.io/api/2024-06-15`
and `https://api.currencylayer.com/historical?date=2024-06-15` respectively.
Treat them as one vendor with one dependency risk, not three options.

Pricing, read 2026-09-18, is also near identical across the three:

| Tier | exchangerate.host | Fixer | CurrencyLayer |
| --- | --- | --- | --- |
| Free | $0, 100 requests/month, **non-commercial** | $0, 100 calls, **non-commercial** | $0, 100 calls |
| Basic | $14.99/month, 10,000 | $14.99/month, 10,000 | $14.99/month, 10,000 |
| Professional / Enterprise | $59.99/month, 100,000 | $59.99/month, 100,000 | $59.99/month, 100,000 |
| Professional Plus / Business | $99.99/month, 500,000 | $99.99/month, 500,000 | $99.99/month, 500,000 |

Sources: [exchangerate.host/pricing](https://exchangerate.host/pricing),
[fixer.io](https://fixer.io/), [currencylayer.com/product](https://currencylayer.com/product).
Fixer states history "all the way back to 1st January, 1999" and 170 currencies;
CurrencyLayer states 168 currencies and metals, history back to 1999;
exchangerate.host states approximately 168 currencies and 19 years of history.

The free tier of exchangerate.host and Fixer is explicitly non-commercial, so it
is not usable for a published app even at Linger's trivial request volume. The
cheapest lawful tier is $14.99 per month, which is $180 per year to convert a
few dozen bookings.

### 2.5 Open Exchange Rates

Plans, read 2026-09-18 from
[openexchangerates.org/signup](https://openexchangerates.org/signup): Free $0 for
"up to 1,000 requests/month", USD base only; Developer $12/month for 10,000
requests with all base currencies; Enterprise $47/month for 100,000 requests
with time-series; Unlimited $97/month. Two months free on annual billing.

The historical endpoint is `/historical/:date.json`, with history "currently
going back to 1st January 1999", and "changing the base currency and requesting
specific symbols are currently available for clients on the Developer,
Enterprise and Unlimited plans"
([docs](https://docs.openexchangerates.org/reference/historical-json)). A
free-plan client is therefore stuck on a USD base and must cross-rate itself.

Coverage is good: `https://openexchangerates.org/api/currencies.json` is public
without a key and returned **173 currencies including COP, INR, JPY, THB, CAD,
GBP and EUR**. The rate endpoints are not public: `/historical/2024-06-15.json`
without an `app_id` returned HTTP 403 `missing_app_id`.

Their timezone note is useful and unusual in being stated at all: historical
rates are "the last values we published for a given UTC day (up to and including
23:59:59 UTC)" and "All dates and timestamps in the Open Exchange Rates API
refer to the UTC timezone (i.e. GMT+00:00), with no daylight saving applied"
(same docs page).

**Licensing could not be pinned down.** The licence page says the old licence
agreement "was replaced by our refreshed Terms and Conditions of Website Use" in
2013 ([openexchangerates.org/license](https://openexchangerates.org/license)).
Neither the [terms](https://openexchangerates.org/terms) nor the
[services agreement](https://openexchangerates.org/services-agreement) contains
an attribution clause, a caching clause or a redistribution clause that could be
quoted. This is recorded as unconfirmed rather than assumed permissive.

### 2.6 ExchangeRate-API

Pricing, read 2026-09-18 from
[exchangerate-api.com](https://www.exchangerate-api.com/#pricing): Free $0 for
1,500 requests/month, Pro $10/month for 30,000, Business $30/month for 125,000.
161 currencies stated.

The keyless open-access endpoint requires attribution: display "Rates By
Exchange Rate API" with a link, and it is current rates only, updated once per
day ([docs/free](https://www.exchangerate-api.com/docs/free)). Historical rates
are a separate, keyed endpoint: `GET https://v6.exchangerate-api.com/v6/YOUR-API-KEY/history/BASE/YEAR/MONTH/DAY`,
"only available to users on our Pro, Business or Volume plans", with all
currencies from 2021-01-01 and a limited set of 35 codes from 1990-01-01 to
2020-12-31
([docs/historical-data-requests](https://www.exchangerate-api.com/docs/historical-data-requests)).

So the free tier is useless for Linger (no dated history) and the paid tier puts
a key in the APK. $10/month is the cheapest commercial-safe keyed option found.

### 2.7 US Federal Reserve H.10

The H.10 gives "daily bilateral exchange rates and U.S. dollar indexes for the
previous business week", released "On Mondays at 4:15 p.m." with "If Monday
falls on a Federal Holiday, the data will be released on the following business
day" ([federalreserve.gov/releases/h10](https://www.federalreserve.gov/releases/h10/)).
The rates are "noon buying rates in New York for cable transfers payable in the
listed currencies", from the Federal Reserve Bank of New York, certified "for
customs purposes as required by section 522 of the amended Tariff Act of 1930"
([H.10 about page](https://www.federalreserve.gov/releases/h10/about.htm)).

The current table lists Australia, Brazil, Canada, China, Denmark, EMU Members,
Hong Kong, India, Japan, Malaysia, Mexico, New Zealand, Norway, Singapore, South
Africa, South Korea, Sri Lanka, Sweden, Switzerland, Taiwan, Thailand, the
United Kingdom and Venezuela
([current H.10](https://www.federalreserve.gov/releases/h10/current/)).
**Colombia is not in it.** Missing observations are marked "ND = No Data for
this date" in the table itself, which is a notably honest convention and worth
copying: the release distinguishes "no rate exists" from "here is a rate".

Access is via the Data Download Program, "with a noted upcoming retirement", or
FRED, or XML ([H.10 page](https://www.federalreserve.gov/releases/h10/)). A
weekly release cadence means a booking made on Thursday cannot be priced from
H.10 until the following Monday, which rules it out as a fetch-at-ingestion
source regardless of coverage.

### 2.8 Central banks for the target regions

| Bank | Pivot | Covers | From | Key | Notes |
| --- | --- | --- | --- | --- | --- |
| ECB | EUR | 47 codes incl. INR, THB, CAD, GBP; no COP | 1999-01-04 | none | see 2.2 |
| Bank of England | GBP | 27 codes incl. INR, CAD, EUR, THB; no COP | 2000-01-04 | none | licence split, see below |
| Bank of Canada | CAD | 28 codes incl. INR, GBP, EUR, THB; no COP | 2017-01-03 | none | Valet API, see below |
| Banco de la Republica | COP | USD only | 2000-01-04 | none | TRM, see below |
| Reserve Bank of India | INR | discontinued, see below | n/a | n/a | now FBIL |

Coverage and start dates in that table are from Frankfurter's provider metadata
(`https://api.frankfurter.dev/v2/providers`), not from each bank's own
publication, and are marked as such in the ambiguities section.

**Bank of Canada Valet.** Keyless and clean. A live call to
`https://www.bankofcanada.ca/valet/observations/FXUSDCAD/json?start_date=2024-06-14&end_date=2024-06-17`
returned data with no credentials, and every response embeds
`"terms": {"url": "https://www.bankofcanada.ca/terms/"}`. Those terms say "the
Bank permits you to freely use, copy, distribute and transmit its website
content" provided "You must attribute the Bank of Canada as the source of the
content, and indicate if changes were made"
([bankofcanada.ca/terms](https://www.bankofcanada.ca/terms/)). Commercial use is
allowed with the caveat that if content is supplied through a paid service,
purchasers must be told it came from the Bank's site and is available free. The
series list at `https://www.bankofcanada.ca/valet/lists/series/json` contains 81
`FX*CAD` series including `FXINRCAD`, `FXGBPCAD`, `FXEURCAD` and `FXTHBCAD`, and
**no COP series**.

**Bank of England.** Two different licences on one site. General resources may
be downloaded "for personal use or internal use within an individual
organisation for non-commercial purposes", but "Database material is governed by
the UK Open Government Licence", which permits commercial reuse with attribution
([bankofengland.co.uk/legal](https://www.bankofengland.co.uk/legal)). The rates
themselves come with a disclaimer on the rate lookup page: "The exchange rates
are not official rates and are no more authoritative than that of any commercial
bank operating in the London foreign exchange market"
([BoE rate lookup](https://www.bankofengland.co.uk/boeapps/database/Rates.asp?TD=18&TM=Sep&TY=2026&into=GBP&rateview=D)).

**Banco de la Republica, Colombia.** The TRM is the only rate Colombia
publishes, and it is USD to COP only. The dataset on the national open data
portal is titled "Tasa de Cambio Representativa del Mercado- TRM", described as
"el promedio ponderado de las operaciones de compra y venta de contado de
dolares de los Estados Unidos de America a cambio de moneda legal colombiana",
attributed to the "Superintendencia Financiera de Colombia", and licensed
**CC BY-SA 4.0**
([metadata](https://www.datos.gov.co/api/views/32sa-8pi3.json),
[dataset](https://www.datos.gov.co/Econom-a-y-Finanzas/Tasa-de-Cambio-Representativa-del-Mercado-TRM/32sa-8pi3/about_data)).
It is queryable keyless over the Socrata API at
`https://www.datos.gov.co/resource/32sa-8pi3.json`. CC BY-SA is a share-alike
licence, which is a real consideration if the rate table were to be bundled into
a closed-source app; see ambiguities.

**Reserve Bank of India: not a source any more.** RBI's own page states "Since
July 10, 2018 Computation and Dissemination has been taken up by FBIL (Financial
Benchmarks India Ltd.)"
([RBI](https://www.rbi.org.in/scripts/BS_DisplayReferenceRate.aspx)). The
handover press release says "FBIL will commence the process of computing and
disseminating reference rate for USD/INR and exchange rate of other major
currencies with effect from July 10, 2018 (Tuesday)", that rates are published
"on every week-day (excluding Saturdays, Sundays and Bank Holidays in Mumbai)",
and that "The daily press release on Reference Rate issued by the RBI will be
discontinued after July 9, 2018 (Monday)"
([RBI press release, 4 July 2018](https://www.rbi.org.in/Scripts/BS_PressReleaseDisplay.aspx?prid=44393)).
FBIL's own site ([fbil.org.in](https://www.fbil.org.in/)) returned only the
company name to this environment's fetcher, so FBIL's publication time, archive
depth and terms of use could not be read; see ambiguities. Any code written
against "the RBI reference rate" today is writing against FBIL.

### 2.9 IMF, Twelve Data and Wise

**IMF.** The IMF publishes representative rates "reported daily to the Fund by
the issuing central bank", posted "every 20 minutes from 11:00 AM to 6:00 PM
U.S. EST Monday to Friday", with an archive back to January 1995
([IMF](https://www.imf.org/external/np/fin/data/param_rms_mth.aspx)). The IMF's
query tool and its copyright page both returned errors to this environment
(redirect loop and HTTP 403 respectively), so currency coverage, machine access
and data licence could not be read from the IMF itself. Frankfurter's provider
metadata lists the IMF with 49 currencies **including COP and INR**, daily
cadence, from 2003-04-01. That is a second-hand reading and is flagged as such.

**Twelve Data.** Individual plans, read 2026-09-18: Basic free (8 credits per
minute, 800 per day), Grow $29/month, Pro $99/month, Ultra $329/month
([twelvedata.com/pricing](https://twelvedata.com/pricing)). The same page
describes these plans as for "personal, internal, and non-commercial purposes",
so a published app needs a business plan whose price is not on that page. It is
a market-data API rather than a reference-rate source: forex history comes from
`/time_series` with OHLC bars ([docs](https://twelvedata.com/docs)). For a
once-per-booking daily reference rate this is the wrong shape of product and the
wrong price.

**Wise.** `GET /v1/rates` accepts `source`, `target` and a `time` parameter for
a historical timestamp, for example
`GET /rates?source=EUR&target=USD&time=2019-02-13T14:53:01`, and supports
`from`, `to` and `group` for series
([docs.wise.com](https://docs.wise.com/api-reference/rate/rateget)). It "only
supports Bearer authentication for non-Affiliate partners" and uses UserToken or
PersonalToken. That is a per-user credential behind a partner relationship, so
it cannot live in a client app and there is no published price. Interesting as
a source that is closer to what a traveller would actually get, but not
available on these terms.

---

## 3. The structural problem: bases, baskets and non-business days

### 3.1 Every central bank publishes against itself, for a limited basket

The ECB publishes against EUR, the Bank of England against GBP, the Bank of
Canada against CAD, Banco de la Republica against COP, FBIL against INR. None of
them publishes an arbitrary pair. To get USD to INR from the ECB you divide
EUR/INR by EUR/USD, which is exactly what Frankfurter's `base` parameter does
and what a bundled ECB table would do.

Cross-rating through a pivot is arithmetically fine, and it is what every
consumer of these feeds does, but it doubles the rounding error and, more
importantly, it means the resulting number is nobody's published rate. The ECB
has never published a USD/INR figure. Linger would be computing one and calling
it "the ECB rate". The honest label is "derived from ECB reference rates".

The baskets are also small and do not contain what Linger needs. The ECB's 30
currencies do not include COP. The Bank of Canada's 81 `FX*CAD` series do not
include COP. The H.10 list does not include Colombia. **For USD to COP, exactly
one authority exists, and it is Banco de la Republica.**

### 3.2 What actually happens on a weekend, from the sources

Four different behaviours were observed, all on the same weekend of Saturday
2024-06-15.

| Source | Request | Response |
| --- | --- | --- |
| ECB Data Portal API | `startPeriod=2024-06-15&endPeriod=2024-06-16` | HTTP 200 with an **empty body** |
| ECB Data Portal API | `startPeriod=2024-06-14&endPeriod=2024-06-17` | two observations, 2024-06-14 and 2024-06-17, nothing between |
| Bank of Canada Valet | `start_date=2024-06-14&end_date=2024-06-17` | two observations, `d: 2024-06-14` then `d: 2024-06-17` |
| Frankfurter v1 (ECB) | `/v1/2024-06-15` | rates returned, but the response's own `date` field reads **`2024-06-14`** |
| Frankfurter v2, single provider | `/v2/providers/ecb/rate/usd/gbp?date=2024-06-15` | `{"date":"2024-06-14", ... }` |
| Frankfurter v2, blended | `/v2/rate/usd/gbp?date=2024-06-15` | `{"date":"2024-06-15","rate":0.78529}` |
| Banco de la Republica TRM | rows spanning 2024-06-13 to 2024-06-18 | a row with `vigenciadesde: 2024-06-15`, `vigenciahasta: 2024-06-17` |

Three distinct conventions are visible there, and they are the conventional
answers to the question:

1. **Say nothing.** The ECB and the Bank of Canada simply have no observation
   for a non-business day. The ECB's own page is explicit that no rate is
   published "on TARGET closing days"
   ([ECB](https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html)).
   The TARGET operating schedule confirms closing days exist as a defined
   concept and gives Good Friday and Easter as worked examples
   ([TARGET System operating
   schedule](https://www.ecb.europa.eu/paym/target/shared/pdf/TARGET_System_operating_schedule.en.pdf)).
   The full ECB history CSV encodes the same idea within a row, using `N/A` for
   a currency with no value that day.
2. **Carry the previous business day forward, and say you did.** Frankfurter's
   single-provider responses return the Friday rate with the `date` field
   honestly reading `2024-06-14`. This is the right convention and it is what
   Linger should implement: the rate is Friday's, the label says Friday.
3. **Define the rate as valid over the gap.** Colombia does this explicitly.
   The TRM row carries `vigenciadesde` and `vigenciahasta`, and the Friday
   calculation is stamped as valid from Saturday 15 June through Monday 17 June.
   There is a real, official, published answer to "what was the rate on
   Saturday" in Colombia, and it is 4151.55.

### 3.3 The convention Linger must not adopt

Frankfurter's **blended** v2 endpoint does something different and, for a frozen
rate, dangerous: it returns the requested date in the `date` field even when no
contributing source observed anything on that date.

For USD to GBP on Saturday 2024-06-15, the blended answer was `{"date":
"2024-06-15", "rate": 0.78529}`. Asked to expand, **every single one of the
contributing providers had an observation date of 2024-06-14**. The Saturday
date on that response is the date that was asked for, not the date anything was
measured.

The COP case is worse, because it shows the blend actively degrading a rate that
does have an official Saturday value. For USD to COP on 2024-06-15:

| Provider | Observation date | Rate |
| --- | --- | --- |
| BANREP (the authority) | 2024-06-15 | **4151.55** |
| BCBO | 2024-06-14 | 4187.88 |
| BCP | 2024-06-14 | 4160.88 |
| BCRA | 2024-06-14 | 4137.17 |
| BCU | 2024-06-14 | 4130.91 |
| BCV | 2024-06-14 | 4100.00 |
| BDI | 2024-06-14 | 4144.71 |
| CBKKW | 2024-06-13 | 4030.92 |
| NBP | 2024-06-12 | 4031.65 |
| **Blended result** | **stated as 2024-06-15** | **4119.52** |

(From `https://api.frankfurter.dev/v2/rate/usd/cop?date=2024-06-15&expand=providers`
and `https://api.frankfurter.dev/v2/providers/banrep/rate/usd/cop?date=2024-06-15`,
cross-checked against the TRM row on
[datos.gov.co](https://www.datos.gov.co/resource/32sa-8pi3.json).)

The blend is 0.77 percent below the official number, dragged down by eight
sources whose readings are one to three days stale, one of them from the
previous Wednesday. Frankfurter's own FAQ warns about this in mild terms ("the
last decimal places may shift"); the observed error here is four orders of
magnitude larger than a last decimal place, and it is systematic rather than
noisy, because staleness always points backwards in time.

**Conclusion for the code: if Linger uses Frankfurter, it must pin a provider.**
`?providers=...` or the `/v2/providers/{key}/...` routes. The blended default is
convenient and wrong for this use.

---

## 4. Alternatives and twists

Each of these is judged on three things: what it costs, what it buys, and
whether it removes the dependency or just moves it.

### 4.1 Capture the settled amount instead of the rate

**The idea.** The number the traveller will eventually care about is the line on
their card statement in their home currency, which already contains the issuer's
rate and the issuer's markup. Ask for that instead of computing anything.

**Is it the truer number?** Yes, unambiguously, per section 1. The Visa rules
put the cardholder rate in the issuer's hands, allow a percentage markup on top,
and convert at clearing rather than at purchase. A market mid-rate on the
purchase date is a different quantity from what was debited, and no amount of
source selection closes that gap.

**What it costs.** Two sub-options, and they are very different.

- *Automatic*, via a bank aggregator such as Plaid. This is dead on arrival for
  Linger. It requires the traveller to hand over bank credentials to a third
  party, which is the precise opposite of the "No data out" guardrail in
  [INGESTION.md](../INGESTION.md). It is also a per-user paid dependency. And it
  would not even work well: Plaid's transaction object carries a single `amount`
  described as "The settled value of the transaction, denominated in the
  transaction's currency", with `iso_currency_code` and `unofficial_currency_code`
  being mutually exclusive
  ([Plaid docs](https://plaid.com/docs/api/products/transactions/)). No field
  found there pairs the original foreign amount with the settled home amount, so
  even with full bank access, deriving the implied rate would be a matching
  exercise across two records.
- *Manual*, one number typed by the traveller when the statement arrives. Costs
  one field and one prompt. See 4.2.

**Removes or moves the dependency?** The manual form **removes** it completely.
No FX provider, no key, no network, no licence, no attribution, no pricing page
to re-read in two years.

**Verdict.** The manual form is the strongest idea in this document, and it fits
a pattern Linger already has: ROADMAP.md already specifies "A post-return prompt
when the vendor said 'estimated'", and a settled-amount prompt is the same
mechanism aimed at a different field. It is out of build one today ("Tracking
what actually hit the card" is explicitly listed as out), but it is the honest
long-run answer, not a workaround.

### 4.2 Ask the traveller once, at the point where it matters

**The idea.** The app never fetches a rate. The first time a Trip contains two
currencies and the traveller looks at a total, it says "I can add these up if
you tell me roughly what a dollar was worth", with one input, one currency pair,
one number.

**What it costs them.** One number per currency pair per trip, not per booking.
The sample trip is USD only, so it costs zero. A US to India trip costs one
number. The effort is a few seconds of typing and, realistically, a glance at a
search engine, which is exactly where the user's original "use Google's rate"
answer was pointing anyway. The app is not scraping Google; the traveller is
using it, which is entirely permitted.

**What it buys.** Total independence, an exactly correct "as at" story (the
traveller states what they used), and a number the traveller already believes
because they chose it. It also sidesteps every licensing and attribution
question in section 2.

**Costs to weigh honestly.** It is a prompt in a flow that is otherwise
zero-effort, and Linger's whole intake philosophy is "upload and scan, not
typing" (ROADMAP.md). A blank field is also a worse default than a nearly-right
number: some travellers will skip it and get no total at all.

**Removes or moves?** Removes.

### 4.3 Present it as an estimate with a visible "as at" date and a tap to correct

**The idea.** Not an alternative to a source so much as the framing that makes a
cheap source acceptable. Show "about $1,240 total, at ECB rates as at 14 Jun
2024", where the date and the rate are both tappable and editable.

**What it buys.** It converts accuracy from a promise the app makes into a
choice the traveller makes. It also makes the weekend problem presentable rather
than hidden: if a booking was made on Saturday and the rate is Friday's, the
label says Friday, and nobody has been misled. Section 3.2's convention 2 is
exactly this, implemented in the data rather than the copy.

ADR-0004 and DATA-MODEL.md already commit to most of it: the combined total "is
always marked as an estimate, with its rate and date shown". The additions are
(a) the rate being user-editable, which turns 4.2 from a blocking prompt into an
optional correction, and (b) the "as at" date being the *observation* date, not
the booking date, which is the thing section 3.3 warns about.

**Costs.** One editable field and one extra column in the Booking row
(observation date, distinct from the date the rate was fetched). Small.

**Removes or moves?** Neither. It reduces the cost of being wrong, which is what
makes the cheapest source viable.

### 4.4 Do not combine at all: per-currency subtotals

**The idea.** A Trip shows "$1,240 and INR 18,400" and stops. No rate, no
estimate, no dependency.

**What it buys.** Everything in 4.2 plus zero UI for a rate. It is also
arguably more truthful: the traveller paid two amounts in two currencies and
that is the fact.

**What it costs.** The one thing a combined total is for, per ADR-0004:
"comparing trips, and remembering what something cost". Two trips with different
currency mixes become incomparable. This is a real loss and the reason the ADR
chose a frozen rate in the first place.

**What comparable apps do.** This was the hardest thing in this brief to source
primarily, and the honest answer is that it was not sourced. Splitwise's help
site (`help.splitwise.com`, `support.splitwise.com`) did not resolve from this
environment; `splitwise.com/faq` returned HTTP 404; the Splitwise blog was
readable and contains nothing about currencies. TravelSpend's FAQ redirected to
a domain that then returned 404. Search results describing what these apps do
were all secondary (roundups and forum posts) and are deliberately not cited
here. So: **no primary-source claim about competitor behaviour is made in this
document.** See ambiguities.

**Removes or moves?** Removes, at a product cost the ADR has already decided
against paying.

A softer version worth considering: per-currency subtotals are the *default*
display, and the combined estimate appears only once the traveller has supplied
or accepted a rate. That makes 4.2 non-blocking and leaves the app correct when
it has nothing.

### 4.5 Bundle a static rate table, updated per release

**The idea.** Ship the history in the APK. No network call ever, for any
booking, which is a better fit for "offline-first" than a once-per-booking fetch
that ADR-0004 concedes can fail ("If the source is unreachable when a Booking is
saved, the rate stays empty").

**This is more feasible than it sounds.** The full ECB history is one file:
`eurofxref-hist.zip` downloaded at **639,491 bytes**, expanding to a
**1,922,007 byte** CSV with **7,096 daily rows covering 1999-01-04 to
2026-09-18** across 41 currencies
([ECB](https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.zip)). Cut down
to the columns Linger cares about (Date, USD, GBP, INR, CAD, JPY, THB), the same
7,096 rows are **371,442 bytes of CSV, or 118,991 bytes gzipped**. That is under
120 KB of APK for every business day of the euro era, in six currencies, with no
network and no provider.

**What it costs.**

- It goes stale between releases. A booking made after the last release has no
  rate in the table. Mitigations: ship a wider recent window, or fall back to
  the network only for dates newer than the bundled table, or accept a gap and
  use 4.2 for it.
- **COP is not in the ECB table**, so a bundled ECB file does not cover the
  sample trip's target region. Colombia would need the TRM series bundled
  alongside, which is CC BY-SA 4.0. Whether embedding a CC BY-SA dataset in a
  closed-source app triggers the share-alike obligation on the app or only on
  the dataset is a licensing question this document does not answer.
- Attribution is required either way, per the ECB disclaimer.

**Removes or moves?** Removes the runtime dependency entirely and converts it
into a build-time one, which is a much better place for it: a broken build is
visible, a broken API call at 2am in a hotel is not.

**Verdict.** Genuinely strong, and it is the option that best matches the stated
architecture. The honest limitation is that it turns "which provider" into
"which providers do we bundle, and how do we handle the one that is not in the
main file".

### 4.6 Options that keep "No data out" intact

Ranked by how little leaves the device.

| Option | What leaves the device | Notes |
| --- | --- | --- |
| Bundled table (4.5) | nothing | strongest |
| Traveller types the rate (4.2) | nothing | strongest |
| Per-currency subtotals (4.4) | nothing | strongest |
| Frankfurter public instance | a date and a currency pair | no key, and the operator states "The API does not collect or log personal data, IP addresses, or request URLs" and that requests "qualify as Data Not Collected under Apple and Google Play guidelines" ([frankfurter.dev](https://frankfurter.dev/)) |
| Self-hosted Frankfurter | a date and a currency pair, to a host Linger controls | Docker image published, MIT licensed ([frankfurter.dev/deploy](https://frankfurter.dev/deploy/), [GitHub](https://github.com/lineofflight/frankfurter)) |
| ECB Data Portal or Bank of Canada Valet direct | a date and a series id | keyless, first-party, no privacy statement found either way |
| Any keyed API | a date, a pair, and a shared secret | see below |

On the keyed APIs: Android's own security guidance is that this does not work.
"When your app is compiled, and your app's source code includes API keys, it's
possible for an attacker to decompile the app and find these resources"
([developer.android.com](https://developer.android.com/privacy-and-security/security-tips)).
Shipping an Open Exchange Rates `app_id` or an apilayer `access_key` in the APK
means shipping a credential that can be extracted and burned through by anyone,
at Linger's expense, against a quota Linger pays for. The alternatives are a
proxy server (which Linger does not have, and which would put a Linger-operated
server in the path of every booking, weakening the guardrail) or picking a
keyless source. **Key requirement is not a minor inconvenience here; it is close
to disqualifying.**

Worth noting: a rate lookup carries a date and a currency pair, which is a weak
but real signal about where and when someone travelled. It is not traveller
data in the sense INGESTION.md means (no name, no reference number, no vendor
contact), but "no vendor is contacted on the traveller's behalf" is satisfied by
every option above, including the network ones, since an FX provider is not a
vendor in the booking.

---

## 5. Recommendation

**Use Frankfurter v2 with a pinned provider, fetched once at ingestion, with the
ECB bundled table as the offline fallback, and the traveller able to overwrite
the number.** Concretely, in priority order:

1. **Bundle `eurofxref-hist.csv`, trimmed.** Under 120 KB gzipped for six
   currencies back to 1999 (measured, section 4.5). Every USD, EUR, GBP, INR,
   CAD, JPY and THB booking on a past business day resolves with no network at
   all. This is the primary path, not the fallback, and it is what makes the
   feature genuinely offline-first rather than offline-after-the-first-fetch.
2. **For dates the bundled table does not cover, and for COP, call Frankfurter
   v2 with a pinned provider.** `GET /v2/providers/ecb/rate/{base}/{quote}?date=`
   for everything the ECB covers, and `GET /v2/providers/banrep/rate/usd/cop?date=`
   for Colombia. No key, no quota, commercial use confirmed by the operator, and
   the response carries the honest observation date.
3. **Never call the blended `/v2/rates` or `/v2/rate/...` default.** Section 3.3
   shows why: the `date` field on a blended response is the requested date, not
   an observation date, and the blend is systematically dragged stale.
4. **Store the observation date, not the request date.** The Booking's
   `rate_at_purchase` needs a companion field for the date the rate was actually
   observed, and the UI shows that date. A Saturday booking says "at Friday's
   rate", which is correct and is the convention the ECB and the Bank of Canada
   both enforce by simply having no Saturday value.
5. **Make the rate editable.** One tap to correct, per 4.3. This is what turns
   an unavoidable approximation into the traveller's own number, and it is the
   escape hatch for every currency and date the chosen sources miss.
6. **Attribute.** The ECB requires it ("the ECB must be cited as the source"),
   Banco de la Republica's dataset is CC BY-SA 4.0, and Frankfurter explicitly
   passes the upstream terms through. One line in an about screen.

### Why not the others

- **A keyed commercial API** (Open Exchange Rates at $12/month, apilayer at
  $14.99/month, ExchangeRate-API at $10/month) buys wider currency coverage and
  a support contract. It costs a secret in the APK that Android's own docs say
  cannot be kept, or a proxy server Linger does not have and that would weaken
  the "No data out" posture. For a call volume of a few dozen per user per year,
  paying $120 to $180 a year to introduce a credential leak is a bad trade.
- **The free tiers are not usable.** exchangerate.host and Fixer state free is
  non-commercial. Open Exchange Rates' free tier is USD base only. Twelve Data's
  individual plans are described as non-commercial. ExchangeRate-API's free tier
  has no dated history at all.
- **Direct central bank feeds** (ECB Data Portal, Bank of Canada Valet) are
  excellent and keyless, but Linger would have to write and maintain a different
  client, a different date convention and a different cross-rating path per
  bank, and would still have no single source that covers COP plus INR plus GBP.
  Frankfurter with a pinned provider is that same data with one client, and it
  is self-hostable and MIT licensed if the public instance ever goes away, which
  is the thing that would otherwise make depending on a free service reckless.
- **H.10, IMF, Wise, Twelve Data** are ruled out by cadence, by unreadable
  terms, by credential model and by product shape respectively; see 2.7 and 2.9.

### The uncomfortable part of this recommendation

Everything above buys a number that is, per section 1, within roughly 1.75
percent of what fifty-three other official sources would have said, and an
unknown further distance from what the traveller's bank actually charged. The
engineering cost of the difference between the best and worst options in this
document is much smaller than that error bar. **The framing in 4.3 is therefore
worth more than the source selection**, and if effort has to be cut, cut it from
the source and spend it on the label, the editable field and the observation
date.

### If a currency the recommended source does not cover turns up

The sequence, in order:

1. **Check whether a single other Frankfurter provider covers it**, via
   `https://api.frankfurter.dev/v2/currencies` (166 currencies with per-currency
   date ranges) and `https://api.frankfurter.dev/v2/providers` (98 sources with
   per-provider currency lists). Both are keyless and can be checked at build
   time rather than runtime. Adding a currency then costs one more pinned
   provider key in a lookup table, not a new integration.
2. **If Frankfurter has it but only thinly** (COP has 13 providers against GBP's
   92), pin the domestic authority specifically. The country's own central bank
   is the one source that is definitionally not stale for its own currency, and
   as the COP table in 3.3 shows, it is the one the blend gets wrong.
3. **If Frankfurter does not have it, do not add a paid provider for one
   currency.** Fall back to 4.2: show the per-currency subtotal on its own line,
   exactly as ADR-0004 already specifies for a missing rate ("A currency whose
   rate could not be fetched sits on its own line rather than being folded in"),
   and offer the tap-to-enter field. A currency Frankfurter cannot reach across
   98 central banks is one where a commercial aggregator's number would be an
   unverifiable guess anyway.
4. **Only if it becomes common** should a keyed provider enter the picture, and
   then behind a proxy rather than with a key in the APK, and as a deliberate
   weakening of the "No data out" guardrail that gets its own ADR.

---

## Ambiguities and unconfirmed items

1. **ECB Data Portal API rate limits.** `https://data.ecb.europa.eu/help/api/overview`
   returned HTTP 503 when fetched. The API itself answered requests without
   authentication, but no documented quota, throttle or fair-use policy was
   read. If the ECB API is used directly rather than through Frankfurter, this
   needs checking.
2. **Frankfurter's blending algorithm is not documented.** The site says rates
   are "blended across all providers" and `expand=providers` shows an `excluded`
   flag on outliers, but the selection rule (median? trimmed mean? what makes a
   provider excluded? how stale is too stale?) is not published. The observed
   USD/INR blend of 92.04 against a median of 91.96 suggests something near but
   not equal to a median. This is a reason to pin a provider regardless.
3. **Frankfurter's public instance has no SLA and one maintainer.** A status
   page exists (`frankfurter.instatus.com`) and the Docker image is published,
   but the free public instance is a single point of failure. The mitigation is
   the bundled table plus self-hosting, both already in the recommendation.
4. **Whether Frankfurter v1's deprecation will become removal.** The response
   headers carry `deprecation: @1779103800` (2026-05-18) while the FAQ says v1
   "remains available indefinitely". Those are not contradictory but they are
   not reassuring. Build against v2.
5. **Open Exchange Rates' actual data licence.** The licence page points to
   terms that were replaced in 2013; neither the terms nor the services
   agreement contains a quotable clause on attribution, caching or
   redistribution of rates. Any use of OXR needs a direct answer from them.
6. **exchangerate.host, Fixer and CurrencyLayer COP coverage.** All three state
   currency counts (168 to 170) but their currency lists are behind the API key,
   so COP could not be verified. Their free-tier error responses are identical,
   which is itself evidence of shared infrastructure.
7. **Whether apilayer free tiers permit HTTPS.** The pricing pages read on
   2026-09-18 show SSL on all tiers including free. Historically these brands
   restricted HTTPS to paid tiers. If a free tier is ever used, verify before
   relying on it.
8. **Twelve Data's business pricing.** The individual pricing page says those
   plans are "for personal, internal, and non-commercial purposes" but the
   business plan prices are on a separate tab that did not render to this
   environment's fetcher. No commercial price for Twelve Data is stated in this
   document.
9. **FBIL's terms, archive depth and publication time.** `fbil.org.in` returned
   only the company name to this environment. RBI confirms FBIL took over on
   2018-07-10 and publishes on weekdays excluding Mumbai bank holidays, but
   FBIL's own statement of what it publishes, from when, and under what terms
   could not be read. Relevant if Linger ever wants the authoritative INR rate
   rather than a cross-rate.
10. **IMF coverage, access and licence.** The IMF's query tool URL produced a
    redirect loop and `imf.org/en/about/copyright-and-terms` returned HTTP 403.
    The claim that the IMF covers 49 currencies including COP and INR daily from
    2003-04-01 comes from **Frankfurter's provider metadata, not from the IMF**,
    and is unconfirmed.
11. **Central bank coverage and start dates in the 2.8 table** are likewise from
    Frankfurter's provider metadata rather than each bank's own publication,
    with the exception of the Bank of Canada series list and the TRM dataset,
    which were queried directly.
12. **Visa's and Mastercard's consumer-facing currency converter pages could not
    be read.** `usa.visa.com`, `visa.co.uk` and `mastercard.com` consumer pages
    all returned HTTP 403 to this environment. The Visa claims in section 1 come
    from the public *Visa Core Rules and Visa Product and Service Rules* PDF,
    which is a stronger source anyway. **No Mastercard primary source was
    obtained**, so nothing in this document claims anything about Mastercard's
    conversion behaviour.
13. **What comparable apps actually do.** Not established. Splitwise's help
    domains did not resolve, `splitwise.com/faq` returned 404, and TravelSpend's
    FAQ redirect returned 404. Only secondary sources were available and they
    are deliberately not cited. If the per-currency-subtotals question in 4.4
    needs settling, it needs someone to install the apps and look.
14. **Whether bundling the CC BY-SA 4.0 TRM dataset in a closed-source APK
    triggers share-alike on anything other than the dataset.** The licence is
    stated plainly in the dataset metadata; its interaction with app
    distribution is a legal question, not a research one.
15. **Whether H.10 data is redistributable.** The release pages carry no terms of
    use statement that could be quoted. Moot given the weekly cadence rules it
    out, but noted.
16. **Whether Plaid or any aggregator can pair an original foreign amount with a
    settled home-currency amount.** The transactions documentation describes a
    single `amount` field as "The settled value of the transaction, denominated
    in the transaction's currency" and two mutually exclusive currency code
    fields. No field pairing the two was found, but the documentation is large
    and this was not exhaustively searched. Moot unless 4.1's automatic variant
    is ever reconsidered, which the guardrail says it should not be.
