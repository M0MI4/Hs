# Real-Time Firestore Migration & In-App Admin Studio

Upgrade HsStore by completely eliminating brittle Pastebin/raw GitHub JSON dependencies, transitioning all catalog, news, and version config streams to real-time Cloud Firestore collections (`/apps`, `/news`, `/config/store`), and introducing a discrete, hidden in-app Admin Studio with local APK auto-parsing and instant real-time propagation across all devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during the planning interview:

- **Admin Studio Access**: When authenticated as an admin, access is provided through a high-polish **Floating Cupertino Admin Badge** (`AdminBadge` with status indicator) on all screens, alongside an unlocked **Admin Studio entry row** inside the Settings/About section.
- **Initial Data Seeding**: If the Firestore collections (`/apps`, `/news`) are empty on first startup, the app will **automatically seed initial data** from the bundled local assets (`mod_catalog.json` and `news_announcements.json`) so users and admins experience an immediate, vibrant catalog without manual copy-pasting.
- **Frictionless Public Experience**: General users experience **zero login or signup screens**. The app opens instantly to the Dashboard and Apps catalog with unrestricted read access.
- **Discrete 5-Tap Gesture**: Admin login is concealed behind a 5-tap gesture on the app version row in `AboutScreen.kt`, opening an authentic Cupertino email/password authentication modal.

---

## 1. Overview & Core Concept

- **What It Does**: Replaces static/remote Pastebin JSON files with live, bidirectional Firestore collections. General users enjoy instant snapshot updates whenever catalog items, changelogs, or editorial news posts change. Authorized store administrators can manage the app catalog (create, edit, delete, inspect APKs), broadcast breaking news banners, and publish forced store update configurations right from their Android devices.
- **Target Audience**:
  - *General Modding Community*: Users discovering, downloading, and tracking modded Android applications with zero friction, offline Room caching, and live remote updates.
  - *Store Administrators (HAMA SADE Team)*: Store managers needing instant catalog publishing, APK file parsing, and broadcast announcements without modifying external text paste sites or pushing code commits.
- **Key Value**: 100% serverless, zero-maintenance real-time store management with automated APK metadata extraction (package name, version, versionCode, SHA-256 checksum, size).

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Frictionless Public Launch**:
   - App launches immediately into `DashboardScreen.kt` or `AppsScreen.kt`.
   - Real-time snapshot listener (`addSnapshotListener`) populates apps and stories seamlessly.
   - If offline, Room database provides cached fallback instant loading.

2. **Discrete Admin Authentication (5-Tap Gesture)**:
   - In the Settings/About screen, tapping the Version card ("HsStore v1.3.0") 5 times within 2.5 seconds triggers a subtle haptic vibration and reveals an authentic **Cupertino Admin Login Sheet**.
   - Input fields for Admin Email and Password with smooth validation feedback.
   - Upon successful `signInWithEmailAndPassword`, a spring-animated confirmation toast displays, unlocking the **Floating Cupertino Admin Badge** and the **Admin Studio** settings row.

3. **In-App Admin Studio (`AdminPanelScreen.kt`)**:
   - Divided into 3 segmented Cupertino views:
     - **Apps Manager**: List of all catalog apps with status badges, search/filter, and a "New App" floating button. Tap any app to edit or delete with confirmation.
     - **Local APK Auto-Parser**: Admin picks an `.apk` file from device storage via Android Storage Access Framework (`ActivityResultContracts.GetContent`). The engine parses the APK archive info via `PackageManager.getPackageArchiveInfo()`, populates title, package name, version, versionCode, file size, and computes the cryptographic SHA-256 hash.
     - **News & Broadcasts**: Real-time editor to draft, pin, or delete editorial news posts and emergency breaking banners into `/news`.
     - **Store Remote Config**: Adjust minimum required version, latest versionCode, and update download URL to push live update notifications to active users.
   - **Sign Out**: Clean action button to invalidate the admin session and return the app to general user view.

### Visual Identity & Theme
- **Cupertino Glass Polish**: Admin floating badge and modal sheets match iOS 17/18 visual language with rounded corners (24–32.dp), subtle border strokes (`Color.White.copy(alpha = 0.15f)`), and SF-style typography.
- **Semantic Accents**:
  - Primary Action / GET: `iOSSystemBlue` (#007AFF)
  - Admin / Verified: `VerifiedBadgeGreen` (#34C759)
  - Destructive / Delete: `iOSSystemRed` (#FF3B30)
  - Broadcast Banner: `iOSSystemOrange` (#FF9500)

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Direct Firestore Snapshot Streams vs. Polling / Pastebin
- **Chosen Approach**: Transition `CatalogRepository` and `NewsRepository` to observe Firestore collections (`appsCollection.addSnapshotListener` and `newsCollection.addSnapshotListener`).
- **Why**: Eliminates HTTP polling, network rate-limits, and broken external paste links. Catalog updates appear on all user devices within sub-seconds.
- **Alternatives Considered**: Keeping a periodic HTTP poll against a backend endpoint was rejected due to latency, battery drain, and stale caches.

### Decision 2: Local APK Metadata Auto-Parser
- **Chosen Approach**: In `AdminViewModel`, utilize Android's built-in `packageManager.getPackageArchiveInfo(apkFile.absolutePath, ...)` and a streaming `MessageDigest.getInstance("SHA-256")` pipeline.
- **Why**: Admins don't have to manually type complex package names, version codes, or compute 64-character SHA-256 signatures; selecting the APK populates the form in milliseconds.

### Decision 3: Public Read / Admin-Only Write Firestore Security Rules
- **Chosen Approach**: Public read for `/apps/{appId}`, `/news/{newsId}`, `/config/{configDoc}` with write access guarded by `isAdmin()` (checking Firebase Auth authentication status).
- **Why**: General users need zero barrier to browse and download mods, while catalog integrity is strictly protected against unauthorized writes.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HSSTORE CLIENT APP                             │
│                                                                        │
│   ┌─────────────────────┐               ┌──────────────────────────┐   │
│   │   General Users     │               │    Admin (5-Tap Unlock)  │   │
│   │  (Zero Auth Read)   │               │   (FirebaseAuth Session) │   │
│   └──────────┬──────────┘               └─────────────┬────────────┘   │
│              │                                        │                │
│              ▼                                        ▼                │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  Jetpack Compose Screens & Navigation                          │   │
│   │  - DashboardScreen / AppsScreen / TodayNewsScreen              │   │
│   │  - AboutScreen (5-Tap Gesture Trigger)                         │   │
│   │  - Floating Cupertino Admin Badge                              │   │
│   │  - AdminPanelScreen (App Form, APK Parser, News, Config)      │   │
│   └──────────────────────────────┬─────────────────────────────────┘   │
│                                  │                                     │
│                                  ▼                                     │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  ViewModels Layer                                              │   │
│   │  - StoreViewModel (Apps StateFlow, News StateFlow)             │   │
│   │  - AdminViewModel (Auth, Save App, Delete App, Parse APK)      │   │
│   └──────────────────────────────┬─────────────────────────────────┘   │
│                                  │                                     │
│                                  ▼                                     │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  Repositories Layer                                            │   │
│   │  - CatalogRepository (Firestore /apps Stream + Room Cache)     │   │
│   │  - NewsRepository (Firestore /news Stream)                     │   │
│   │  - UpdateRepository (Firestore /config/store Stream)           │   │
│   └──────────────────────────────┬─────────────────────────────────┘   │
└──────────────────────────────────┼─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      GOOGLE CLOUD FIREBASE                             │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  Cloud Firestore                                               │   │
│   │  - /apps/{appId}           (Public Read, Admin Write)          │   │
│   │  - /news/{newsId}          (Public Read, Admin Write)          │   │
│   │  - /config/store           (Public Read, Admin Write)          │   │
│   │  - /users/{userId}/...     (User-Scoped Bookmarks/Downloads)   │   │
│   └────────────────────────────────────────────────────────────────┘   │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  Firebase Authentication (Email/Password for Admins)           │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Collections

1. **`/apps/{appId}`** (`ModApp`):
   - `id`: Document ID
   - `name`: App Title (e.g., "SoundWave Studio Mod")
   - `packageName`: Android package name (e.g., "com.vibe.soundwave")
   - `category`: Category string ("Apps", "Games", "Tools", etc.)
   - `version`: Version string ("v9.9.5")
   - `versionCode`: Integer version code
   - `sizeDisplay`: Formatted size ("40.0 MB")
   - `downloadUrl`: Direct APK download endpoint
   - `iconUrl`: Web URL or asset path for app icon
   - `sha256`: Checksum hash for installation verification
   - `description`: Extended overview
   - `modFeatures`: List of modification bullet points
   - `rating`: Float rating (e.g., 4.9)
   - `updatedDate`: Epoch millisecond or ISO timestamp string

2. **`/news/{newsId}`** (`NewsPost`):
   - `id`: Document ID
   - `title`, `subtitle`, `content`: Editorial copy
   - `type`: `BANNER`, `TEXT_ONLY`, or `PATCH_NOTES`
   - `bannerUrl`: Hero image URL
   - `bulletPoints`: Key updates list
   - `date`: Formatted publication date

3. **`/config/store`** (`AppUpdateConfig`):
   - `storeLatestVersion`: Target version string (e.g., "1.3.1")
   - `storeLatestVersionCode`: Version code integer
   - `mandatory`: Boolean flag for force update
   - `downloadUrl`: Update APK download link
   - `announcementMessage`: Store announcement banner

---

## 5. Execution Steps

1. **Firestore Rules & Backend Configuration**:
   - Update `firestore.rules` granting public read and admin write privileges across `/apps`, `/news`, and `/config`.
   - Update `firebase-blueprint.json` schema documentation.
   - Run JavaScript and Kotlin security rules tests locally; deploy via `deploy_firebase`.

2. **Repository Refactoring (Eliminate Pastebin)**:
   - Deprecate and remove `RemoteStoreService.kt` and references in `RemoteConfigRepository.kt`.
   - Rewrite `CatalogRepository.kt` to use Firestore `addSnapshotListener` with automatic Room persistence and first-launch asset seeding.
   - Rewrite `NewsRepository.kt` to use Firestore `/news` snapshot listener with fallback to bundled news.
   - Update `UpdateRepository.kt` to observe `/config/store`.

3. **Admin ViewModel & APK Parsing Engine**:
   - Implement `AdminViewModel.kt` with Firebase Auth email/password authentication, app persistence (`saveApp`, `deleteApp`), news broadcasting, and `parseLocalApk` using `PackageManager.getPackageArchiveInfo` and `MessageDigest`.

4. **UI Layer - Discrete Access & Admin Studio**:
   - Update `AboutScreen.kt` with 5-tap gesture detection on the version row to open the Cupertino Admin Login sheet.
   - Build `AdminPanelScreen.kt` replacing `RemoteControlScreen.kt` with tabbed App Form, APK Parser, News Publisher, and Store Update Configurator.
   - Add floating Cupertino Admin Badge in `MainActivity.kt` when `isAdminLoggedIn == true`.

5. **Verification & Testing**:
   - Compile applet with `compile_applet`.
   - Execute local unit and Robolectric tests (`gradle :app:testDebugUnitTest`).
