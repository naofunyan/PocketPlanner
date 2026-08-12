# PocketPlanner UI/UX Design Specifications

This document outlines the design system, typography, and core screen layouts for PocketPlanner. You can use this as a reference guide when designing high-fidelity mockups in tools like Figma.

---

## 1. Color Palette

The app uses a modern blue and vibrant orange palette, with full support for both Light and Dark modes.

### Light Mode
* **Primary:** `#4496D8` (Vibrant Blue - Used for primary actions, TopAppBar, FABs)
* **Secondary:** `#77BEF0` (Soft Blue - Used for accents, secondary buttons)
* **Tertiary:** `#E86A2D` (Vibrant Orange - Used for highlights, active tabs, warnings)
* **Error:** `#D32F2F` (Red)
* **Background:** `#F7FAFC` (Off-white - Main app background)
* **Surface:** `#FFFFFF` (White - Cards, Bottom Sheets, Dialogs)

### Dark Mode
* **Primary:** `#77BEF0` 
* **Secondary:** `#A8D7FA` 
* **Tertiary:** `#FFFFAC7A` 
* **Error:** `#EF5350`
* **Background:** `#121212` (Very dark gray)
* **Surface:** `#1E1E1E` (Dark gray - Cards, Bottom Sheets)

---

## 2. Typography

The app currently uses modern default system fonts (Roboto on Android), but the hierarchy is structured to easily support Google Fonts like **Inter** or **Outfit**.

* **Display Large** (57sp, Bold): Huge welcome headers (e.g., Login Screen).
* **Title Large** (22sp, SemiBold): App Bar titles, main screen headers.
* **Title Medium** (16sp, Medium): Card headers, list item titles.
* **Body Large** (16sp, Normal): Standard paragraph text, descriptions.
* **Body Medium** (14sp, Normal): Subtitles, secondary text.
* **Label Medium** (12sp, Medium): Navigation bar labels, small tags.

---

## 3. Core Screens & Components

### A. Login Screen
* **Layout:** Centered content.
* **Components:**
  * Huge "Welcome to PocketPlanner" text (Display Large, Primary color).
  * Two Outlined Text Fields: "Email" and "Password".
  * Primary Button: "Login" (Filled, full width, 50dp height).
  * Loading state: Circular progress indicator inside the button.

### B. Main App Shell
* **Layout:** Standard Scaffold with Bottom Navigation.
* **Components:**
  * **Bottom Navigation Bar:** Two tabs (Trips, Profile). Selected tab uses Primary color.

### C. Trips Screen (Home)
* **Layout:** List view of the user's trips.
* **Components:**
  * **Top App Bar:** Title "Your Trips".
  * **Trip Cards:** Displays Destination, Dates, and Budget.
  * **FAB (Floating Action Button):** Bottom right corner to create a new trip (Icon: `+`).

### D. Itinerary Screen (Trip Details)
* **Layout:** Detailed overview of a specific trip.
* **Components:**
  * **Top App Bar:** Destination name, back button.
  * **Trip Info Header:** Status (Upcoming/Active), Date range.
  * **Days List:** A scrollable list of cards for each day of the trip (e.g., "Day 1", "Day 2"). Clicking a day opens the Day Plan Screen.
  * **Extended FAB:** Pill-shaped floating button in the bottom right labeled "Expenses" (Icon: `$` / AttachMoney).

### E. Day Plan Screen
* **Layout:** Detailed schedule for a specific day.
* **Components:**
  * **Top App Bar:** "Day X", back button.
  * **Places List:** Chronological list of places to visit.
  * **Place Card:** Shows Name, Category, Estimated Cost, and Notes.

### F. Expense Tracker Screen
* **Layout:** Split view (Budget Overview + Expense List).
* **Components:**
  * **Top App Bar:** "Trip Expenses", back button.
  * **Budget Overview Card (Top):**
    * Text: "Budget Remaining".
    * Large text showing the remaining amount in VND.
    * Linear Progress Indicator (Progress bar): Shows how much has been spent. Turns red (Error color) if >90% spent.
    * Text: "Spent: X / Y VND".
  * **Recent Expenses List:**
    * Scrollable list below the Budget Card.
    * **Expense Row:** Description (left), Category (below description), Amount (right, in red/error color).
  * **FAB:** Standard `+` button in the bottom right.

### G. Add Expense Bottom Sheet
* **Layout:** Modal Bottom Sheet that slides up when the Expense FAB is clicked.
* **Components:**
  * **Header:** "Add New Expense" (Title Large).
  * **Text Fields:** "Amount (VND)" and "What was it for?".
  * **Category Selector:** A row of Filter Chips (`Food`, `Transport`, `Accommodation`, `Activities`, `Shopping`).
  * **Primary Button:** "Save Expense" (Full width, bottom aligned).

---

## 4. Interaction Guidelines
* **Navigation:** Slide-in from right/left for screen transitions.
* **Bottom Sheets:** Slide up from the bottom with a scrim (dimming) over the background.
* **Buttons:** Ripple effect on tap (default Material 3 behavior).
* **Loading:** Use circular progress indicators centered on screen or inside buttons during network calls (e.g., Auth, Gemini API generation).
