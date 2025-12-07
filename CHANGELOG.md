# Changelog

## **v2.0 — Full UI/UX Overhaul + Navigation Redesign (2025-02-XX)**
**A major upgrade focused on clarity, speed, and modernized interaction.**

### Added
- **New Month View (Accordion Layout):** clean collapsible rows with dynamic totals.
- **New Add-Expense Dialog:** Add Expense now opens in a modal dialog from the FAB.
- **Adaptive version banner:** About screen now shows `v.X`, build variant, and date.
- **Automatic variant detection:** dev / debug / release shown accurately.
- **Off-Budget all-time summary block** in Distribution screen.
- **New distribution legend styling** with proper alignment and stable typography.

### Improved
- **Distribution Screen:**
  - Currency selection now reliably propagates across views.
  - Legend totals and deltas fully recalculated on resume.
  - Layout spacing corrected; top “This Month” block visually aligned.
- **Main Screen:**
  - Rewritten from static XML rows to dynamic programmatic inflation.
  - Smoother month switching and row expansion behavior.
- **Add Expense flow:**
  - Expanded month row auto-opens after adding an expense.
  - Revised light-amber highlight (more subtle and balanced).
- **Global UI polish:**
  - Numerous padding, margin, and color corrections.
  - Unified top-bar behavior across all screens.

### Fixed
- Distribution not updating after currency change (critical regression).
- Category → Distribution navigation now updates values correctly.
- Multiple strobe/recreate loops resolved (eliminated periodic flicker).
- Off-Budget rows now render consistently and without misalignment.
- Several legacy regressions from rollback patches fully eliminated.

### Technical Notes
- Branch-of-truth: `version2.0-fin`
- All major views restructured; codebase now cleaner and more maintainable.
- Minimum UI dependencies retained; no layout inflation recursion.



_All notable changes to **Where It Went** will be documented in this file._

---

## **v1.0.2 — UI Polish & Final Stabilization (2025-01-15)**
**Refined for release-quality field testing.**

### Added
- New polished launcher icon (512×512) for GitHub & F-Droid.
- Full F-Droid metadata structure (screenshots, icon, descriptions).
- Guide screen using clean markdown (`/res/raw/guide.md`).
- “Off-Budget” tag added to category system.
- Automatic previous-month comparison on Distribution Graph.
- Build variants (`devDebug`, `stableDebug`) with custom APK names.

### Improved
- All headers, banners, and view screens unified with consistent padding, colors, and typography.
- Date view and month view restored to original stable collapsible behavior.
- Side drawer UI refined; all navigation links stable and verified.
- Add-Expense form polished (spacing, icons, interaction cues).
- Spinner tag insertion line cleaned (removed stray arrow artifact).
- Category dialog UI fully restored and standardized.
- Distribution screen legends, percentages, and layout finalized.

### Fixed
- Multiple spinner edit regressions resolved.
- Category-tag persistence now stable across all views.
- View-All totals corrected and made persistent.
- Collapsible date headers no longer misalign on reopen.
- Numerous rollback-fix checkpoints merged into a consistent baseline.

### Technical Notes
- Final freeze tag: `freeze-202501XX-final-ui-polish`
- Directory cleanup: old screenshots removed, new structure adopted.
- Repo renamed fully to **WhereItWent**; legacy references removed.
- Credential caching fixed (libsecret) for repeatable Git pushes.

---

## **v1.0.1 — Feature-Complete Baseline (2024-12-12)**
**Core functionality locked in; all modules integrated.**

### Features
- Distribution Graph built from category tags:  
  **Fixed · Necessities · Discretionary · Off-Budget**
- Multi-view navigation:  
  **View All · By Date · By Month · By Category**
- Day-Detail view with totals and rolling calculations.
- Category Manager with radio-button tag selector.
- Auto-insert tag logic during expense entry.
- Full Export module: HTML + CSV.
- Fully offline; zero network permission footprint.

### Improvements
- Unified color system across all screens.
- Clean top-bar layouts with accent strips.
- Navigation robust across all routes.
- Spinner dialogs visually corrected and behavior stabilized.

### Fixes
- Category edits now update across all views instantly.
- By-Date and By-Month views no longer lose state after edits.
- Arrow inconsistencies removed throughout UI.

---

## **v1.0.0 — First Complete Working Build (2024-11-01)**
**Where It Went becomes functional end-to-end.**

### Initial Feature Set
- Add expenses with date, category, and amount.
- Manage categories (defaults + user-defined).
- Basic tagging foundation (Fixed / Necessity / Discretionary).
- View All with totals.
- Monthly and daily rollups.
- Reset database tool.
- Fully local data (SQLite), no accounts, no ads, no cloud.

### Notes
- UI was experimental; later replaced by several polished rewrites.
- Internal versions were not publicly released.

