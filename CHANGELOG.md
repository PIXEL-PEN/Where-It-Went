# Changelog

## v1.0.2 (2025-10-08)
**Feature Upgrades — Unified UI Polish**

### Added
- Floating **“Add Expense”** button on the View Menu for faster entry.
- Category/date filter dialog with dynamic range detection and bold clickable date fields.
- Auto-sorted category spinner (defaults first: Groceries → Rent → Utilities → Bills → Transport → Other).
- Divider line between default and user-added categories for clarity.

### Improved
- Consistent color scheme and padding for FAB and button elements.
- Restored *Manage Categories* spinner functionality after separator fix.
- Unified “Available Range” display for filters.

### Fixed
- Resolved layout regression in View Menu and restored proper FAB positioning.
- Corrected missing **Bills** entry in default category list.

### Technical
- Branch lineage: `feature/category-period-view` → `feature/fab-button`
- Snapshot tag: `freeze-20251008-merge-ui-fab`
- Database: consistent `BETWEEN` date-range queries
- XML: unified ripple drawables, color match for FAB (#546E7A)

---

## v1.0 (2025-09-24)
**Public Release — Stable Build**

### Features
- Add, view, and manage expenses with multiple views:
  - View All
  - Date-wise
  - Month-wise
  - Category-wise
  - Day detail view
- Decimal formatting for totals (`#,##0.00`).
- Category manager with fixed defaults (`Groceries, Rent, Utilities, Bills, Transport, Other`) and custom categories (alphabetical).
- Settings:
  - Currency selection
  - Date format options
  - Export to CSV and HTML
  - Reset database (expenses only, categories preserved)
- Lightweight, fully offline design (no internet required).
- Simple, consistent UI with banners, totals, and editable items.

### Fixes
- Corrected banners for DayDetail screen.
- Spinner now shows defaults first, custom categories sorted.
- UI polish on Settings and totals.

### Known Limitations
- Screenshots to be added.
- Categories cannot yet be labeled/tagged as *Essential*, *Fixed*, *Discretionary* (planned).
