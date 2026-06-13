# SpendLess

> A privacy-first, Nothing OS-inspired Android expense tracker that automatically reads bank SMS messages, tracks your spending, and presents beautiful analytics — all stored locally on your device.

---

## ✦ Features

| Feature | Details |
|---------|---------|
| SMS Auto-Detection | Reads bank/UPI/wallet SMS, extracts amount, merchant, date |
| 50+ Bank Support | HDFC, SBI, ICICI, Axis, PhonePe, GPay, Paytm, and more |
| Smart Categorization | 500+ merchant rules across 17 categories |
| Custom Budget Cycles | Reset on any day (1–31), not just month start |
| Historical Import | Scans all existing SMS on first launch |
| Analytics | Daily / Weekly / Monthly / Yearly insights |
| 3 Home Widgets | Budget (4×2), Compact (2×2), Overview (4×3) |
| Budget Alerts | Smart notifications at 50%, 75%, 90%, 100% |
| Dark / Light / System | Instant theme switching, Nothing-inspired design |
| Biometric Lock | Optional fingerprint/face app lock |
| App Icon Picker | 3 built-in icon variants via activity aliases |
| 100% Offline | No cloud required, no data leaves your device |

---

## ✦ Architecture

```
MVVM + Clean Architecture
├── Presentation  → Jetpack Compose + ViewModel + StateFlow
├── Domain        → Use Cases / Engines (BudgetEngine, AnalyticsEngine)
└── Data          → Room DB + DataStore + Repository
```

**Key Libraries:**
- `Kotlin 2.x` + Coroutines + Flow
- `Jetpack Compose` + Material 3
- `Room 2.x` + Paging 3
- `Hilt` (Dependency Injection)
- `WorkManager` (background SMS processing)
- `DataStore Preferences`
- `Vico` (Compose-native charts)
- `BiometricPrompt`

---

## ✦ Project Structure

```
app/src/main/java/com/spendless/app/
├── SpendLessApplication.kt       # Hilt app + WorkManager config
├── MainActivity.kt               # Edge-to-edge, theme, biometric
├── core/
│   ├── data/
│   │   ├── database/             # Room DB, 5 entities, 4 DAOs
│   │   ├── datastore/            # DataStore preferences
│   │   └── repository/           # Data repositories
│   ├── sms/
│   │   ├── BankPatterns.kt       # 50+ bank regex patterns
│   │   ├── SmsFilter.kt          # OTP/promo filter
│   │   └── SmsParser.kt          # Multi-bank SMS parser
│   ├── categorization/
│   │   ├── MerchantDatabase.kt   # 500+ merchant→category rules
│   │   └── CategorizationEngine.kt
│   ├── analytics/
│   │   ├── BudgetEngine.kt       # Custom cycle calculation
│   │   └── AnalyticsEngine.kt    # Aggregation & insights
│   └── di/                       # Hilt modules
├── receiver/
│   └── SmsReceiver.kt            # Battery-safe broadcast receiver
├── worker/
│   ├── SmsProcessWorker.kt       # Single SMS processing
│   ├── SmsHistoryWorker.kt       # Historical SMS import
│   └── BudgetAlertWorker.kt      # Budget threshold notifications
├── widget/
│   ├── WidgetProviders.kt        # 3 AppWidgetProvider classes
│   └── WidgetUpdateUtil.kt       # Efficient widget updates
└── ui/
    ├── theme/                    # Nothing OS color/type/shape tokens
    ├── components/               # CircularRing, DonutChart, Cards
    ├── navigation/               # NavGraph with animations
    └── screens/
        ├── onboarding/           # 4-page onboarding flow
        ├── dashboard/            # Main dashboard
        ├── transactions/         # Paged transaction list + search
        ├── analytics/            # Period analytics
        ├── budget/               # Budget & cycle settings
        └── settings/             # Theme, biometric, icon, rescan
```

---

## ✦ Database Schema

```
transactions (id, amount, type, merchant, merchantNormalized, category,
              account, rawSmsBody, smsHash [unique], timestamp, cycleId,
              isManuallyEdited, userNote, isExcluded, createdAt)

budgets (id, totalBudget, resetDay, categoryBudgetsJson, isActive, createdAt)

budget_cycles (id, budgetId, startDate, endDate, totalSpent, totalIncome, isActive)

category_rules (id, keyword, merchantPattern, category, priority, isUserDefined)

monthly_summaries (id, cycleId, categoryBreakdownJson, dailySpendingJson,
                   highestSpendingDay, highestCategory, transactionCount)
```

---

## ✦ Setup & Build

### Prerequisites
- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android SDK with API 35

### Steps

```bash
# 1. Clone the repository
cd /home/bhlp0089/Documents/expense-tracker

# 2. Download Space Grotesk + Space Mono fonts from Google Fonts
#    Place in: app/src/main/res/font/
#    Required files:
#    - space_grotesk_regular.ttf
#    - space_grotesk_medium.ttf
#    - space_grotesk_semibold.ttf
#    - space_grotesk_bold.ttf
#    - space_mono_regular.ttf
#    - space_mono_bold.ttf

# 3. Add launcher icons to:
#    app/src/main/res/mipmap-*/ic_launcher.png (all densities)

# 4. Open in Android Studio and sync Gradle

# 5. Run tests
./gradlew test

# 6. Build debug APK
./gradlew assembleDebug

# 7. Install on device
./gradlew installDebug
```

---

## ✦ SMS Permissions (Google Play)

If you publish to Google Play, you'll need to submit the **SMS Permission Declaration** form explaining that the app reads SMS for financial tracking. Key points to include:

- App reads SMS to detect bank transactions
- All processing is local — no data leaves the device
- Users can see and delete all transactions
- No third-party analytics or ad SDKs

---

## ✦ Nothing OS Design Principles Applied

| Principle | Implementation |
|-----------|---------------|
| Pure black background | `Color(0xFF000000)` system background |
| White typography | Primary text color = `#FFFFFF` |
| Dot-matrix accents | `DotMatrixBackground` composable, Space Mono font |
| Minimalist UI | No excessive gradients, clean spacing |
| Smooth animations | `animateFloatAsState`, `tween(1200ms)` for rings |
| Large circular elements | `CircularProgressRing` with animated sweep |
| Glass-like cards | `GlassBackground = Color(0x14FFFFFF)` + border |
| Consistent spacing | 20dp horizontal padding throughout |

---

## ✦ Battery Design

- **No persistent foreground service** — uses BroadcastReceiver → WorkManager
- **SMS processing time**: < 100ms per message
- **Widget updates**: Only on new transactions or date change
- **No polling** — purely event-driven
- **WorkManager**: System-managed, battery-aware scheduling
- Target: Similar battery impact to Google Messages / SMS Organizer

---

## ✦ Roadmap (Phase 2)

- [ ] Google Drive backup/restore
- [ ] TensorFlow Lite on-device ML categorization
- [ ] Custom categories with icon picker
- [ ] Export to CSV/PDF
- [ ] Split expenses
- [ ] Family sharing (local sync via Bluetooth)
- [ ] Recurring transaction detection
- [ ] UPI ID → merchant name resolution
