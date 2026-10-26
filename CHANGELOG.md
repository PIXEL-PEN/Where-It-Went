# Changelog

All notable changes to this project will be documented here.

---

## v3.4

### Added
- Accounts only (Settings → Opening Screen): for tracking projects and trips
  without Daily Living. The app opens on the Accounts screen, which gets a +
  button, a "✈ trip" button while Travel mode is on, and a menu that also
  leads to Settings, Travel mode, Yearly Summary, Distribution and the Guide.
  The entry dialog shows only the Accounts card. Daily Living entries are
  kept; turning it off restores the full app.
- "Add Account" in the Accounts menu, which is now ordered: Add Account,
  Manage Accounts, Filter Accounts, Show / Hide Notes, Export Accounts.

### Improved
- Tap an account entry to edit or delete it (long-press still works); a
  one-time hint points this out.
- Settings: consistent section headers and cards, a compact amber currency
  field, blue on-state for switches, and a shorter About.
- Guide: rendered with headings, dividers and bullet points; new sections on
  Accounts only.
- Smaller "✈ trip" button; larger home icon on the travel screen.

### Fixed
- On newer Android, the bottom of Settings, Accounts, Yearly Summary,
  Distribution and the Guide was hidden behind the navigation bar.
- An account opened by adding an entry to it would not close on tap.

---

## v3.3

### License
- From this version the app is licensed under the GNU General Public License,
  version 3 or later (GPL-3.0-or-later). Versions up to 3.2 remain under the
  MIT License.

### Added
- Trip currencies: a Travel account can have its own currency, chosen from 92
  world currencies (tap a list to search it). Amounts are entered in the trip
  currency and shown with their home value; rates are quoted the way banks do,
  in whole numbers ("1 USD = 34 THB"), with a suggested approximate rate.
  Each entry keeps the rate it was entered at.
- Trips kept in their own currency, with no conversion (e.g. a US visit in
  dollars while Settings stays on Dong). A trip in a home currency other than
  Settings is kept out of mixed totals.
- Travel mode (menu → Travel mode): a full screen for one trip, with its
  entries day by day and an entry card (item, amount, category, date, note).
  While it is on, the app opens there with the same trip, category and date.
  A ✈ button on the main screen returns to it.
- Journeys: several country trips grouped under one name, with a journey
  total; "Next country…" starts the next one in the same home currency.

### Improved
- Full backup (version 2) and the Accounts CSV export keep trip currencies,
  rates, trip-currency amounts and journeys, and the Settings currency by code.
  Older backups still import, and 3.2 can still read a new backup.
- Distribution: the accounts total stands out more.
- Guide: sections on trip currencies, Travel mode and journeys.

### Fixed
- Updating from 2.x to 3.x could erase all data (the database had no upgrade
  path from version 2). Upgrades now never wipe data.

---

## v3.2

### Added
- Full backup (Settings → Export → Full backup): one file with every Daily Living
  entry, every account (including empty and archived ones) and its items, all
  categories with their tags, and the currency.
- Import (Settings → Import): restores a full backup, or imports an "All items"
  CSV export from Daily Living or Accounts, including exports made by older
  versions. Shows a preview first; adds only what is missing and never changes
  or deletes existing entries, so importing the same file twice is harmless.

### Improved
- "All items" CSV exports quote their fields, so commas in items and notes no
  longer shift columns, and amounts are written in plain form.
- Reset asks what to delete: Daily Living entries, Accounts, or both, and
  offers to make a full backup first.
- Settings: section colours restored (lost in the 3.0 dark-mode change), with
  dark-mode versions; About text, feature list and feedback links refreshed.
- Guide: new sections on the Yearly Summary and on backup and restore.

### Fixed
- Deleting an account now also deletes its entries. Entries left behind by
  accounts deleted in earlier versions are ignored by the Yearly Summary, its
  exports and the full backup.

---

## v3.1

### Added
- Yearly Summary screen (drawer → Yearly Summary, or Summaries → Yearly summaries).
  One banner per 12-month period showing the period total; tap a banner to show
  or hide category totals for Daily Living and the list of Accounts. Each account
  shows its own total and can be tapped to show its category totals. Each period
  can be exported as CSV (storage) or HTML (email), grouped the same way.
- Periods are anchored to the month of your first record: a January starter
  sees Jan–Dec years, a May starter sees May–Apr. The current period is marked
  "In progress · N of 12 months".
- The same category summary is also available from Settings → Export.

### Fixed
- "Last 12 Months" header total now counts only the 12 months shown
  (it previously added up every expense ever recorded).

### Notes
- No records are deleted automatically. Daily Living and Accounts data are kept
  indefinitely; the 12-month view is a display window only. The item exports
  ("All items") export every record.

---

## v3.0

### Added
- Accounts module (Project, Travel, Custom)
- Account filtering by account and category
- Archived account labeling in summaries
- Filter indicator with dismiss control
- Long-press edit interaction for account items
- Independent expansion behavior on submit

### Improved
- Stabilized Accounts Overview rebuild logic
- Corrected expansion state handling
- Restored month header active color state
- Refined summary presentation

### Fixed
- Show/Hide Notes causing expanded stale screen
- Trigger-happy edit dialog during scroll
- Filter persistence issues
- Expansion state corruption after dialog interaction

---

## v2.0

### Added
- 3-Month / 12-Month toggle
- Distribution graph (Fixed / Necessity / Discretionary)
- Off-Budget tag exclusion
- Improved month expand/collapse behavior
- Category manager improvements

### Improved
- UI consistency across Daily Living screens
- Tightened row spacing and visual hierarchy
- Currency formatting consistency

### Fixed
- Date restoration after edit
- Scroll-to-top behavior after submit
- Various layout alignment issues

---

## v1.x

Initial release:
- Daily Living expense tracking
- Category tagging
- Local-only storage
- Basic export functionality
