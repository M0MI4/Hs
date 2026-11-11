# HsStore Polish, Reordering, Notifications & Performance Optimization

A comprehensive plan to introduce 3-line drag-and-drop custom sorting for apps and news, remove legacy Discord links, provide an Admin Studio broadcast notification system (with Android system notifications for users who grant permission), and make scrolling ultra-smooth by pausing background particles while scrolling.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned based on your responses:
> 1. **Edit Order Mode**: A dedicated "Edit Order" button in Apps and News will toggle the 3-line drag handles (`Icons.Default.DragHandle`), allowing you to hold for a moment and drag up/down to rearrange items, then tap "Done" to save the order.
> 2. **Notifications**: When an administrator broadcasts a notification message from the Admin Panel, the app will deliver a real Android System Notification (using `NotificationManagerCompat` with Android 13+ `POST_NOTIFICATIONS` permission support) as well as an in-app alert banner.
> 3. **Scrolling Performance**: Particle background will remain subtle but will automatically pause rendering whenever lists are scrolling (`isScrollInProgress`), and per-frame gradient allocations will be eliminated to ensure high-FPS smooth scrolling.
> 4. **Discord Removal**: The Discord Support Server button will be completely removed from the settings screen.

---

## 1. Overview & Core Concept

* **Interactive Reordering**: An intuitive drag handle (three horizontal lines) displayed when in "Edit Order" mode on Apps and News lists. Users hold for a moment and drag vertically to swap item positions. Custom order is persisted locally and in Firestore for administrator updates.
* **Administrator Notification Broadcast**: Admin Studio will include a "Broadcast User Notification" tool. When triggered, it publishes to Firestore `/config/notification`. The app observes this in real time, requests `POST_NOTIFICATIONS` permission gracefully on Android 13+, and displays a heads-up system notification with high priority.
* **Scroll Performance Polish**: Eliminates micro-stuttering across all lists by halting background canvas computations during scroll gestures, optimizing `ModAppCard` and Coil image caching, and eliminating overdraw.
* **Discord Removal**: Strips out Discord community references from `AboutScreen.kt` for a cleaner UI.

---

## 2. User Experience & Visual Design

### Apps & News 3-Line Drag Reordering
* Next to the section headers in Apps and News, an **"Edit Order"** action button with a sort icon appears.
* Tapping **"Edit Order"** unlocks the three horizontal lines drag handle (`Icons.Default.DragHandle`) on the right side of each item card.
* **Hold & Drag Gesture**: Pressing and holding the handle for 150ms elevates the card slightly with a subtle shadow and scale effect (`1.02f`). Dragging up or down swaps item indices in real time.
* Tapping **"Done"** locks the list and immediately persists the custom order.

### Admin Broadcast Notifications & Android System Alerts
* **Admin Studio View**: A new **"Send User Notification"** card in Admin Studio with inputs for *Notification Title*, *Notification Message*, and *Action / Mod Package Link*, plus **"Send Notification to Users"** and **"Clear Active Notification"** actions.
* **User Notification Flow**:
  1. If Android 13+ (API 33+), the app prompts the user with standard `POST_NOTIFICATIONS` permission launcher.
  2. When the admin sends a notification, an Android notification channel (`hsstore_announcements`) posts a system notification with an app icon badge, title, body, and click action.
  3. A dynamic Cupertino in-app banner also slides in at the top of the screen with a dismiss gesture.

### Silky Smooth Scrolling Optimization
* **Scroll-Aware Particle Suspension**: Reads `LazyListState.isScrollInProgress` and pauses the particle animation while the user flings or drags lists.
* **Zero Per-Frame Shader Reallocations**: Replaces continuous `Brush.radialGradient` re-creations inside `ParticleCanvas.kt` with pre-computed cached color values and simple circle draw operations.
* **Image Caching & Elevation Lightening**: Disables unnecessary multi-pass overdraw in `ModAppCard`.

---

## 3. Key Technical Decisions & Trade-Offs

### Custom Reorder Architecture
* **Approach**: State-driven index swapping using pointerInput `detectDragGesturesAfterLongPress` on the drag handle.
* **Rationale**: Gives full tactile control without interfering with standard vertical list scrolling or pull-to-refresh gestures.
* **Persistence**: Persists the list of ordered document IDs in `SharedPreferences` for instant client loading, and updates `sortOrder: Int` on Firestore `/apps` documents when an admin saves.

### Notification Delivery
* **Approach**: Combines a real-time Firestore listener on document `/config/notification` with Android `NotificationManagerCompat` and Compose in-app alert banners.
* **Rationale**: Ensures users receive both instant in-app feedback and native Android lock-screen / notification shade notifications when allowed.

```
┌─────────────────────────────────────────────────────────────┐
│                    HsStore System Flow                      │
└─────────────────────────────────────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
     ┌───────────────────┐           ┌───────────────────┐
     │   Admin Studio    │           │  General Users    │
     │                   │           │                   │
     │ • Reorder Catalog │           │ • Local Drag Sort │
     │ • Broadcast Alert │           │ • In-App Banner   │
     │ • Edit News/Apps  │           │ • System Push     │
     └─────────┬─────────┘           └─────────▲─────────┘
               │                               │
               ▼                               │
     ┌─────────────────────────────────────────┴─────────┐
     │           Cloud Firestore & Local Cache           │
     │                                                   │
     │ • /config/notification (Live Snapshot Listener)   │
     │ • /apps (Real-time catalog with sortOrder)        │
     │ • SharedPreferences (custom_sort_order)           │
     └───────────────────────────────────────────────────┘
```

---

## 4. Step-by-Step Implementation Strategy

1. **Remove Discord Button**:
   - In `AboutScreen.kt`, remove the Discord Support Server row and set `showDivider = false` on the Telegram row.
2. **Optimize Scroll Performance & Smoothness**:
   - In `ParticleCanvas.kt`, pause animation when scrolling, use lightweight circle draws without per-frame gradient allocations, and reduce particle density.
   - In `ModAppCard.kt`, ensure `InstallStatusHelper` uses purely non-blocking memory cache to prevent main-thread binder delays.
3. **Add 3-Line Reorder to Apps and News**:
   - In `CatalogRepository.kt` & `NewsRepository.kt`, add custom order index support and persistence.
   - In `AppsScreen.kt` & `TodayNewsScreen.kt`, add an "Edit Order" button and three-line drag handle (`Icons.Default.DragHandle`) with hold-and-drag gesture.
4. **Admin Notification Broadcast & System Notifications**:
   - Declare `POST_NOTIFICATIONS` in `AndroidManifest.xml` and add runtime permission request flow.
   - Create notification channel `hsstore_announcements` with `NotificationManagerCompat`.
   - In `AdminPanelScreen.kt`, add notification sender interface to publish to `/config/notification`.
   - In `MainActivity.kt`, observe notifications and trigger system notification and Cupertino in-app banner.
5. **Compilation & Test Verification**:
   - Run `compile_applet` and execute unit tests (`gradle :app:testDebugUnitTest`) to ensure zero regressions.
