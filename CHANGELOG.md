# Changelog

All notable changes to this project will be documented here.

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
