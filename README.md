# Where It Went

Where It Went is a lightweight, offline-first expense tracking app designed to answer a simple question:

Where did it go?

Unlike traditional budgeting apps, Where It Went does not require income tracking, envelopes, or complex budgeting systems. Your income already lives in your bank. This app focuses purely on understanding spending.

No ads.  
No cloud sync.  
No login.  
No tracking.  
Everything stays on your device.

---

## Core Philosophy

Most finance apps demand structure before insight.

Where It Went reverses that.

Log expenses quickly.  
Let categories and tags organize them.  
Review patterns month to month.  

Clarity without overhead.

---

## Architecture Overview

The app is structured into two independent modules:

### 1. Daily Living

The primary ledger for everyday expenses.

Features:
- Fast entry workflow
- Month grouping with expand/collapse
- 3-Month / 12-Month toggle
- Tag-based Distribution graph
- Category management
- Export and Reset tools

Each category is assigned one of four tags:

- Fixed
- Necessity
- Discretionary
- Off-Budget

Tags drive the Distribution graph and allow meaningful monthly comparison.

---

### 2. Accounts (Projects, Travel, Custom)

Accounts allow expenses to be tracked independently from Daily Living.

Designed for:
- Renovations
- Travel
- Events
- Special purchases
- Temporary or isolated financial activity

Account types:
- Project
- Travel
- Custom

Features:
- Independent grouping
- Filtering by account and category
- Optional note visibility
- Archived account support
- Long-press edit for items
- Submit-to-expand behavior for quick review

Accounts do not distort Daily Living insights.

---

## Design Principles

- Minimal UI
- No unnecessary automation
- Deterministic behavior
- Clear data ownership
- Local-only storage (Room database)
- Explicit backup and export

The app is built for clarity, not financial gamification.

---

## Data & Privacy

Where It Went:
- Stores data locally
- Uses no analytics
- Requires no internet
- Contains no advertisements

Your data is yours.

---

## Version Status

v2.x – Stable Daily Living release  
v3.x – Accounts architecture expansion  

---

## License

Copyright © 2025–2026 PIXEL-PEN

From version 3.3, Where It Went is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version. See [LICENSE](LICENSE) for the full
text. (SPDX: `GPL-3.0-or-later`)

It is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE.

Versions up to and including 3.2 were released under the MIT License, and
those releases remain available under it.

The name "Where It Went" and the app icon are not covered by the license.
