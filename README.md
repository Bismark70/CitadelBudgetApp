# 🏰 Citadel Budget

**Build your money fortress with the 50/30/20 rule.**

Citadel Budget is a fast, beautiful, **100% offline** budgeting app for Android:
a 50/30/20 budget planner, expense tracker, bills & subscriptions organizer,
savings-goal builder and money-health coach — in one small, private app.

> The fortress metaphor: **Needs are the Walls**, **Wants are the Gardens**,
> **Savings are the Vault**. Your job is to keep the walls standing, the gardens
> blooming, and the vault filling.

---

## ✨ Features

| Area | What you get |
| --- | --- |
| 🧮 **50/30/20 planner** | Auto-splits your income; fully customizable percentages with presets (50/30/20, 60/20/20, 70/20/10) |
| 🛡️ **Citadel Score** | A 0–100 fortress-health gauge: savings rate + budget adherence + emergency fund + goal progress, with a coaching tip |
| 💸 **Expense & income tracking** | Quick add (Expense / Income / To-Vault), categories, notes, date picker, edit & delete, search + bucket filters |
| 🧱 **Category limits** | Set a monthly cap per category; over-budget walls turn red |
| 🧾 **Bills & subscriptions** | Recurring bills with due-day tracking, auto-pay flags, "Mark paid" (auto-logs the expense), monthly total |
| 🎯 **Savings goals** | Progress bars, quick "Add money" transfers (auto-logged as vault transactions) |
| 🛟 **Emergency Fund / The Moat** | Aliche-style **Survival Number** calculator with 1/3/6-month targets |
| 🪜 **Baby Steps tracker** | Dave Ramsey's 7-step build order, checkable |
| 📊 **Reports** | Donut (where money went), top-category bars, 6-month income-vs-outflow trend, savings rate, month switcher |
| 🔔 **Wisdom engine** | Daily rotating principle cards from 5 bestselling money books |
| 🔒 **Citadel Gate** | Optional 4-digit PIN lock (salted SHA-256, stored locally) |
| 📤 **CSV export** | Share your full transaction history from the Settings screen |
| 🌗 **Dark mode** | System / Light / Dark |
| 🌍 **15 currencies** | USD, EUR, GBP, CAD, AUD, JPY, CNY, INR, NGN, KES, ZAR, PHP, BRL, MXN, IDR |

### 100% offline & private by design
The manifest declares **no `INTERNET` permission at all**. There is no analytics,
no account, no cloud. Your data lives in a local Room (SQLite) database and can
only leave the device when *you* trigger a CSV export.

---

## 🧠 The Money Library (books baked into the app)

| Book | What Citadel took from it |
| --- | --- |
| **The Total Money Makeover** — Dave Ramsey | The Baby Steps tracker, "starter fund first", the *Live like no one else* spirit |
| **Get Good with Money** — Tiffany Aliche | The **Survival Number** tool and the money-leak alerts on the Home screen |
| **I Will Teach You to Be Rich** — Ramit Sethi | Conscious spending: custom split sliders, guilt-free Wants, automation-friendly design |
| **The Psychology of Money** — Morgan Housel | The savings-gap logic inside the Citadel Score; "wealth is what you don't see" |
| **Broke Millennial** — Erin Lowry | The tone: zero money-shame — every empty state and report is kind |

*(Wisdom cards are paraphrased principles attributed to their books.)*

---

## 🛠️ Tech stack

- **Kotlin** + **Jetpack Compose (Material 3)** — single-activity, declarative UI
- **Room** (SQLite) + **DataStore** — fully local persistence
- **MVVM** — `ViewModel` + Kotlin `Flow`/`StateFlow`, no DI framework (tiny service locator)
- **Custom Canvas charts** — donut, gauge and trend chart drawn in Compose; zero chart libraries
- **Navigation Compose** with bottom-bar tab navigation
- Min SDK 26 · Target/Compile SDK 34 · Gradle 8.9 · AGP 8.7.3 · Kotlin 2.0.21

## 📁 Project layout

```
app/src/main/java/com/citadel/budget/
├── CitadelBudgetApp.kt        # Application + AppContainer (service locator, DB seeding)
├── MainActivity.kt            # Theme + lock/onboarding routing
├── data/
│   ├── SettingsStore.kt       # DataStore-backed user settings (income, split, theme, PIN…)
│   └── local/                 # Room entities, DAOs, database, default categories
├── domain/
│   ├── MoneyMath.kt           # 50/30/20 split, Citadel Score, parsing, tiers
│   └── Wisdom.kt              # Book-wisdom principle cards
├── ui/
│   ├── components/            # Shared cards/rows + Canvas charts (donut, gauge, trend)
│   ├── nav/Nav.kt             # Routes + MainScaffold (bottom bar + FAB)
│   ├── theme/Theme.kt         # Logo-inspired navy/blue/teal/gold palette, dark mode
│   ├── onboarding/  lock/  home/  activity/  add/
│   ├── plan/        fortify/  reports/  settings/
└── util/Formatters.kt         # Money & date formatting
```

## 🚀 Getting started

1. Open **Android Studio** (Ladybug or newer recommended).
2. `File → Open…` and select the **`CitadelBudget`** folder.
3. Let Gradle sync — Android Studio will automatically download **Gradle 8.9**
   (via the committed wrapper) and any missing **SDK 34** components.
4. Press **Run ▶** on an emulator or device (Android 8.0 / API 26+).

Command line (optional, JDK 17+ required):

```bash
./gradlew assembleDebug     # build APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest # run the MoneyMath unit tests
```

> **If sync complains** about a missing wrapper, open
> `Settings → Build, Execution, Deployment → Build Tools → Gradle` and keep
> *"Use Gradle from: 'gradle-wrapper.properties' file"* selected, then re-sync.

## 🎨 Customization pointers

- **Colors/theme** → `ui/theme/Theme.kt` (palette mirrors the app logo: navy → blue → teal, gold accents)
- **Default categories** → `data/local/DefaultData.kt`
- **Score formula** → `domain/MoneyMath.kt#citadelScore` (weights: 30 savings / 25 adherence / 25 emergency fund / 20 goals)
- **Wisdom cards** → `domain/Wisdom.kt`
- **Baby steps content** → `ui/fortify/FortifyScreen.kt` (`babySteps` list)

## 🗺️ Roadmap ideas

- Monthly auto-repeat for salary income
- Local notifications for bill due dates (AlarmManager)
- Encrypted Room + biometric unlock (`BiometricPrompt`)
- Multi-profile budgets (you + partner)
- Widgets: safe-to-spend & score glance

## 📜 License

All rights reserved © 2026 Citadel Budget. Your logo, your app, your data.
