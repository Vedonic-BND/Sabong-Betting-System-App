# Runner Popup Notification Fix - Complete Analysis & Solution

## 🎯 Problem Summary
Runner was receiving assignment notifications (saved to database, appeared in Alerts tab), but popup overlay was NOT displaying when owner assigned runner to teller.

**Symptom**: Logs showed `Callback status: onRunnerAssignedByOwner = NULL ❌`

## 🔍 Root Cause Analysis

### Issue #1: ReverbViewModel Overwriting RunnerViewModel Callbacks ⚠️

**The Problem:**
- `RunnerViewModel.setupRealtimeListener()` sets: `ReverbManager.onConnected`, `ReverbManager.onDisconnected`, `ReverbManager.onRunnerAssignedByOwner`
- When user navigates to `CashInScreen`, a NEW `ReverbViewModel` instance is created
- `ReverbViewModel.connect()` OVERWRITES: `ReverbManager.onConnected`, `ReverbManager.onDisconnected`
- This happens on a **shared global singleton** (ReverbManager)
- Result: RunnerViewModel's callbacks are superseded by ReverbViewModel's callbacks

**Impact:**
```
Timeline:
1. RunnerScreen loads → RunnerViewModel.setupRealtimeListener()
   - Sets onRunnerAssignedByOwner = { RunnerVM callback }
   - Sets onConnected = { RunnerVM callback }
   - Sets onDisconnected = { RunnerVM callback }

2. User navigates to CashInScreen
   - ReverbViewModel created
   - ReverbViewModel.connect() called
   - Overwrites: onConnected, onDisconnected with ReverbVM versions
   - onRunnerAssignedByOwner STILL SET (but callbacks are conflicting)

3. Event arrives: runner.assigned-by-owner
   - Event handler invokes onRunnerAssignedByOwner
   - But ReverbVM's lifecycle may have affected callback state
```

### Issue #2: ReverbViewModel Calling disconnect() on Global Singleton ❌

**The Problem:**
- `ReverbViewModel.onCleared()` → `disconnect()` → `ReverbManager.disconnect()`
- This DESTROYS the WebSocket when CashInScreen closes
- RunnerScreen still needs the connection!

**Code:**
```kotlin
// ReverbViewModel (BEFORE FIX)
override fun onCleared() {
    super.onCleared()
    disconnect()  // ❌ Destroys global WebSocket!
}

fun disconnect() {
    ReverbManager.disconnect()  // ❌ Sets pusher = null, destroys channels
}
```

### Issue #3: Duplicate setupRealtimeListener() Calls

**The Problem:**
- `Navigation.kt` called `runnerViewModel.setupRealtimeListener()` with `LaunchedEffect(role, userId)`
- `RunnerScreen.kt` called `runnerViewModel.setupRealtimeListener()` with `LaunchedEffect(Unit)`
- Second call could overwrite first call's setup

## ✅ Solutions Implemented

### Fix #1: Remove ReverbViewModel's Callback Management
**File:** `ReverbViewModel.kt`

```kotlin
// BEFORE (Lines 86-89)
ReverbManager.onConnected = {
    viewModelScope.launch(Dispatchers.Main) { _connected.value = true }
}
ReverbManager.onDisconnected = {
    viewModelScope.launch(Dispatchers.Main) { _connected.value = false }
}

// AFTER
// ✅ Don't override onConnected/onDisconnected
// Let RunnerViewModel or other screens manage these shared callbacks
```

**Why:** 
- `onConnected` and `onDisconnected` are global events that affect all screens
- Only ONE screen should manage them (RunnerViewModel)
- ReverbViewModel should only manage callbacks it explicitly needs (`onFightUpdated`, `onBetPlaced`, etc.)

### Fix #2: Prevent Singleton Disconnection
**File:** `ReverbViewModel.kt`

```kotlin
// BEFORE
fun disconnect() {
    ReverbManager.disconnect()
}

override fun onCleared() {
    super.onCleared()
    disconnect()  // ❌ Destroys global connection
}

// AFTER
fun disconnect() {
    // ⚠️ IMPORTANT: Don't disconnect the global ReverbManager singleton!
    // The WebSocket should remain connected for the entire app lifetime.
    // Multiple screens may need real-time events, so we cannot disconnect
    // when one screen closes.
    android.util.Log.d("ReverbVM", "⚠️  Skipping disconnect() - ReverbManager is global singleton")
}

override fun onCleared() {
    super.onCleared()
    // Don't call disconnect() - see comment above
    android.util.Log.d("ReverbVM", "ReverbVM cleared (no disconnect)")
}
```

**Why:**
- ReverbManager is a **global singleton** used by multiple screens
- Runner notifications work even when RunnerScreen is not active
- Disconnecting when one screen closes breaks real-time notifications for other screens

### Fix #3: Remove Duplicate Navigation Setup
**File:** `Navigation.kt`

```kotlin
// BEFORE
LaunchedEffect(role, userId) {
    if (role == "runner" && userId?.isNotEmpty() == true) {
        runnerViewModel.setupRealtimeListener(context, parsedUserId)
    }
}

// AFTER
// ✅ Removed - RunnerScreen will handle this
```

**Why:**
- RunnerScreen already calls `setupRealtimeListener()` in its LaunchedEffect
- Navigation.kt's LaunchedEffect may not have userId loaded yet
- Better to set up listeners only when the screen is actually displayed

### Fix #4: Add Duplicate Setup Guard
**File:** `RunnerViewModel.kt`

```kotlin
// Added flag
private var isRealtimeListenerSetup = false

// Could be used to prevent multiple setups:
fun setupRealtimeListener(context: Context, userId: Long = -1) {
    if (isRealtimeListenerSetup) {
        android.util.Log.d("RunnerVM", "⏭️  Skipping - realtime listener already setup")
        return
    }
    isRealtimeListenerSetup = true
    // ... rest of setup
}
```

**Why:**
- Extra safety against accidental duplicate registrations
- Makes intent explicit in code

### Fix #5: Remove Notification Read Status Filter
**File:** `RunnerScreen.kt` (Line 112)

```kotlin
// BEFORE
val assignmentNotif = notifications.firstOrNull { 
    it.title == "Assignment" && !it.isRead  // ❌ Filtering by read status
}

// AFTER
val assignmentNotif = notifications.firstOrNull { 
    it.title == "Assignment"  // ✅ Show all Assignment notifications
}
```

**Why:**
- Notification might be marked as read from database
- But should still trigger popup on first appearance
- Better to show ALL assignments and let user dismiss them

## 🔄 Expected Event Flow (After Fixes)

```
1. App starts
   ↓
2. AppNavigation → ReverbManager.connect() [global WebSocket connection]
   ↓
3. RunnerScreen loads → LaunchedEffect(Unit) calls setupRealtimeListener()
   ↓
4. RunnerViewModel sets ReverbManager callbacks:
   - onConnected
   - onDisconnected  
   - onTellerCashUpdated
   - onCashRequested
   - onRunnerAccepted
   - onRunnerAssignedByOwner ← TARGET CALLBACK ✅
   ↓
5. User optionally navigates to CashInScreen
   ↓
6. CashInScreen creates ReverbViewModel
   ↓
7. ReverbViewModel.connect() sets ONLY its callbacks:
   - onFightUpdated
   - onBetPlaced
   - onWinnerDeclared
   - onTellerCashUpdated (from cash-status channel)
   - onRunnerAccepted
   - onRunnerDeclined
   ↓
8. ReverbManager.connect() early-returns (already connected) ✅
   ↓
9. Owner assigns runner → backend broadcasts runner.assigned-by-owner
   ↓
10. Pusher receives event on cash-requests channel
    ↓
11. cashRequestChannel.bind("runner.assigned-by-owner") handler invokes:
    onRunnerAssignedByOwner?.invoke(payload) ← CALLBACK IS SET ✅
    ↓
12. RunnerViewModel callback executes:
    - Filters by runner_id
    - Creates notification with title="Assignment"
    - Adds to _notifications StateFlow
    ↓
13. RunnerScreen's LaunchedEffect(notifications) detects new notification
    ↓
14. Finds Assignment notification (read status doesn't matter now)
    ↓
15. Sets runnerAssignment state
    ↓
16. Popup displays with sound/vibration 🎉
    ↓
17. Auto-dismiss LaunchedEffect clears after 5 seconds
```

## 📊 Callback Management Strategy

### Before (Broken)
```
RunnerViewModel         ReverbViewModel
    ↓                       ↓
    └─ onConnected ←────────┘ (conflict)
    └─ onDisconnected ←──────┘ (conflict)
    └─ onRunnerAssignedByOwner (could be affected)
```

### After (Fixed)
```
RunnerViewModel
    ├─ onConnected
    ├─ onDisconnected
    ├─ onTellerCashUpdated
    ├─ onCashRequested
    ├─ onRunnerAccepted
    └─ onRunnerAssignedByOwner ← ONLY RunnerVM manages this

ReverbViewModel
    ├─ onFightUpdated
    ├─ onBetPlaced
    ├─ onWinnerDeclared
    ├─ onTellerCashUpdated (separate from RunnerVM's)
    ├─ onRunnerAccepted (complement to RunnerVM's)
    └─ onRunnerDeclined
```

## 🧪 Testing Steps

1. **Start app** → Runner logs in
2. **Navigate to RunnerScreen** → wait for "Setup realtime listener" log
3. **Verify in logcat:**
   ```
   ✅ onRunnerAssignedByOwner callback registered!
   ```
4. **Switch to CashInScreen** (optional) → verify logs:
   ```
   ⚠️  Skipping disconnect() - ReverbManager is global singleton
   ```
5. **Return to RunnerScreen** (if switched)
6. **Open Admin/Owner panel** → assign Runner to Teller
7. **Verify logs:**
   ```
   🎯 runner.assigned-by-owner: {...}
   Callback status: onRunnerAssignedByOwner = SET ✅
   🎯 Invoking onRunnerAssignedByOwner callback with payload: {...}
   🎯🎯🎯 [CALLBACK] onRunnerAssignedByOwner TRIGGERED!
   ✅ Adding assignment notification - Assigned to: [Teller Name]
   📝 addNotification called - title: Assignment
   📋 Checking notifications list
   ✅ Found Assignment notification
   🎯 Assignment notification triggered
   ```
8. **Verify on Runner device:**
   - ✅ Popup appears with assignment message
   - ✅ Sound plays
   - ✅ Device vibrates
   - ✅ Auto-dismisses after 5 seconds

## 🚀 Key Takeaway

**The issue was callback lifecycle management on a shared singleton:**
- Multiple ViewModels were fighting over control of global ReverbManager callbacks
- Singleton lifecycle (connect/disconnect) was being tied to individual screen lifecycles
- **Solution:** Clear separation of concerns - only RunnerViewModel manages shared callbacks, only one place controls WebSocket lifetime

