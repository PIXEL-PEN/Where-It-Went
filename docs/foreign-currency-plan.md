# Foreign Currency for Travel Accounts — Build Plan

Status: agreed design, not yet built. Written 14 Oct 2026, after v3.2.

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

## Open details (decide while building)

- Currency list: the Settings list (12 currencies) is short for travel; likely
  extend it with common travel currencies.
- Decimal places: yen, won, dong have no minor unit. Show those with 0
  decimals.
- Version number: 3.3, or 4.0 as the last major feature.
