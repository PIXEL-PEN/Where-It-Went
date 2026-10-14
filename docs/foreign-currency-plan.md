# Foreign Currency for Travel Accounts — Build Plan

Status: prototype built (steps 1–5, plus Yearly Summary and Distribution),
for testing on WIW-dev. Written 14 Oct 2026, after v3.2.

Prototype notes:
- Demo trips: WIW-dev Accounts menu → "Add demo trips (dev)" (debug builds only).
- Not yet done: Full backup v2 and the Accounts CSV columns (a backup made now
  keeps home amounts but not the dong/yuan amounts or rates), Guide text.
- Also fixed: database v2 (all releases before 3.0) had no upgrade path to
  v5, so updating 2.0 → 3.0 erased data. `MIGRATION_2_5` now covers it.

## What it does

A Travel account can have a foreign currency. Expenses are entered in that
currency (e.g. yen), and every row and total also shows its value in the
account's home currency (e.g. US dollars) underneath. Exchange rates are
entered by hand. Everything works offline.

```
Japan trip                       ¥36,200
                                 $240.00
  03 Mar  Hotel                  ¥30,000
                                 $200.00
  10 Mar  Dinner                  ¥6,200
                                  $40.00   (@155)
```

Daily Living, Project and Custom accounts are unchanged.

## Decisions (agreed)

1. **Each entry keeps its own rate.** Changing the trip's rate only affects
   new entries. Past rows and totals never shift. When an entry's rate
   differs from the trip's current rate, the row shows it, e.g. "(@155)".
2. **One foreign currency per trip.** Every entry in the account is in that
   currency; there is no per-entry currency picker.
3. **Home currency per trip.** It defaults to the currency in Settings but can
   differ (e.g. app in THB, Japan trip reported in USD). A trip whose home
   currency differs from Settings is **shown in its own currency and kept out
   of mixed totals**, with a short note, so baht and dollars are never added
   together.
4. **Rate direction is always written out**: "1 USD = 150 JPY" (foreign units
   per one home unit). ¥30,000 at 150 = $200.00.
5. **Manual rates only.** No network access, ever.

## Data model

Database version 5 → 6. The change only **adds** columns; no existing value is
modified.

`accounts`
| column            | type | meaning                                              |
|-------------------|------|------------------------------------------------------|
| `foreignCurrency` | TEXT, null | e.g. "JPY". Null = feature off for this account |
| `homeCurrency`    | TEXT, null | e.g. "USD". Null = the Settings currency       |
| `rate`            | REAL, default 0 | current rate: foreign units per 1 home unit |

`account_items`
| column          | type       | meaning                                         |
|-----------------|------------|-------------------------------------------------|
| `foreignAmount` | REAL, null | amount as entered, e.g. 30000 (yen)              |
| `rate`          | REAL, null | rate used for this entry, e.g. 150               |

**The existing `amount` column keeps holding the home-currency value**
(30000 / 150 = 200.00, rounded to 2 decimals when saved). So every existing
total — account totals, Summaries, Yearly Summary, Distribution, exports —
keeps adding `amount` exactly as today and stays correct, in the account's
home currency. Rows without `foreignAmount` are ordinary entries.

## Safety: the database upgrade

`ExpenseDatabase` currently calls `fallbackToDestructiveMigration()`. With
that setting, if the app ever meets a database version it has no upgrade step
for, Room **deletes all data** instead of failing. Before raising the version
to 6:

- Add `MIGRATION_5_6` (the `ALTER TABLE … ADD COLUMN` statements above).
- **Remove `fallbackToDestructiveMigration()`**, so a missing or broken upgrade
  stops the app with an error instead of wiping it.
- Rehearse on WIW-dev with a full backup of real data, before the real app.
- Make a fresh Full backup from the real app before installing.

## Screens

1. **New Travel account.** The existing "Foreign currency" switch opens the
   existing `dialog_foreign_currency.xml`: Home currency, Trip currency, rate
   ("1 USD = [ 150 ] JPY"), live preview ("¥10,000 = $66.67").
2. **Existing Travel accounts.** Manage Accounts → account → "Currency…" to
   turn it on, change the rate, or turn it off. Turning it off keeps the
   stored yen amounts; rows simply show the home amount only.
3. **Add entry.** The amount field is labelled with the trip currency (¥) and
   a line underneath shows the conversion: "= $20.00 at 150". A tap on that
   line changes the trip's rate.
4. **Edit entry.** Edit the yen amount; the entry keeps its own rate.
5. **Accounts overview.** Yen on each row with the home amount beneath;
   account header total in yen with the home total beneath.
6. **Main screen Summaries.** The account's total in its home currency, with
   that currency's symbol.
7. **Yearly Summary and Distribution.** Account totals in home currency. An
   account whose home currency differs from Settings shows its own currency
   and is left out of the Accounts total and grand total, with a note.

## Files: export, backup, import

- **Accounts CSV export** gains columns `ForeignAmount, ForeignCurrency, Rate,
  HomeCurrency` at the end. The importer accepts both the old and the new
  header.
- **Full backup** goes to format version 2: `ACCOUNT` lines carry the three
  currency settings; `ACCOUNT_ITEM` lines carry foreign amount and rate.
  Version 1 backups stay importable. (3.2 can't read version 2 files.)
- **Category summary export** follows the Yearly Summary rule for mixed
  currencies.

## Build order

Each step is tested on WIW-dev before the next.

1. **Conversion core** (pure Java + unit tests): conversion and rounding,
   rate labels, formatting both amounts, summing per currency.
2. **Database upgrade** to version 6, with the safety changes above.
   Rehearse with a restored real backup on WIW-dev.
3. **Account currency settings**: the dialog for new and existing Travel
   accounts.
4. **Entry and edit** with the foreign amount and live conversion.
5. **Display**: Accounts overview rows and totals; main Summaries.
6. **Yearly Summary, Distribution, exports, Full backup v2, Import.**
7. **Guide, changelogs, store text**, version bump, release.

## Next pass: everyday denominations (requested 14 Oct 2026)

People think in familiar whole amounts: 250,000 dong, 25 baht for a coffee,
a dollar. Big-number currencies like dong should never turn into cryptic
decimals such as "1 VND = 0.0000386 USD". Proposal:

1. **Quote rates the way banks do: the stronger currency is the "1".**
   "1 USD = 25,900 VND", "1 CNY = 5.02 THB" — never 0.0000x. The rate field
   in the dialog follows the same direction; the app converts internally.
2. **Everyday reference amounts.** Each currency gets a round "pocket" amount
   worth roughly 1–5 US dollars, picked from the built-in rate table
   (e.g. ₫100,000 · ฿100 · ¥20 (CNY) · ¥500 (JPY) · $5). The dialog example
   and a glance line on the trip use it: "₫100,000 ≈ ฿130".
3. **Converted amounts in whole numbers** — confirmed: round off cents.

Rule (confirmed 14 Oct 2026): **always whole denominations**, never decimal
rates. Done: rates are quoted bank-style with the stronger currency as the
base and a round quantity (1, 10, 100…) so the other side is a whole number
≥ 20 ("1 USD = 34 THB", "10 CNY = 50 THB", "100 CAD = 104 USD"); the rate
field takes whole numbers in that form; converted amounts under trip entries,
trip totals and the entry line are whole. Still to do: the pocket-amount
glance line (point 2).

## Open details (decide while building)

- Currency list: the Settings list (12 currencies) is short for travel; likely
  extend it with common travel currencies.
- Decimal places: yen, won, dong have no minor unit. Show those with 0
  decimals.
- Version number: 3.3, or 4.0 as the last major feature.

## Travel mode (requested 14 Oct 2026)

On a trip, Daily Living can be left aside. Menu → **Travel mode** → pick a
Travel account. A full screen for that trip (`TravelActivity`) shows its
entries day by day (newest first, with day totals) and an entry card at the
bottom: item, amount in the trip currency (with the "= 3,000 ฿ · 1 THB =
772 VND" line, tap to change the rate), category, date, Add. While the
keyboard is open the card moves up under the top bar and the newest entries
show beneath it. The trip name and total are in the top bar.

- **Opens where you left off**: while travel mode is on, the app starts on the
  travel screen, with the same trip, the last category used on that trip, and
  the date. A date left on "today" moves with the calendar; a date picked by
  hand stays until changed (`TravelDate`).
- **Toggle**: the home button (or Back) goes to the main screen, which shows a
  "✈ trip" button; one tap returns. Tapping the trip name switches trips.
  ✕ ends travel mode; the trip and entries stay in Accounts.
- First tried as a floating card over the main screen (commit "Travel mode:
  floating trip card"); replaced by the full screen for more room for entries.
- State lives in the `travel_mode` preferences (`TravelMode`); no database
  change.

## Journeys: multi-country trips (requested 14 Oct 2026)

Each country stays its own Travel account with its own currency; a
**journey** name (e.g. "SE Asia 2026") groups them. Database v7 adds
`accounts.journey` (TEXT, null = none; `MIGRATION_6_7` adds the column only).

- Travel screen → trip name ▾ → **Next country…**: name the country and the
  journey, then choose its currency. The new trip keeps the journey's home
  currency (locked), gets a suggested rate, and travel mode moves to it.
- **Journey…** in the same picker: put the current trip in a journey (with
  suggestions of existing names), rename the journey for all its trips, or
  clear it to take the trip out.
- The picker lists a journey's trips together: "SE Asia 2026 › Vietnam (VND)".
- The top bar adds "SE Asia 2026 · 2 countries · 6,800 ฿": the trips in the
  same home currency added up; a trip in another home currency is counted
  apart ("+1 in another currency"), never mixed in.
- Not yet: the journey in Accounts, Yearly Summary and the exports, and in
  Full backup v2 (to add with the currency fields).

