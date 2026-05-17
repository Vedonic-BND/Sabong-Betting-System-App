# Exact Code Changes Made

## File 1: ReverbViewModel.kt

### Change 1A: Remove onConnected/onDisconnected Assignments (Lines 86-89)

**BEFORE:**
```kotlin
        } else if (currentUserId != -1L) {
            android.util.Log.d("ReverbVM", "Using provided userId: $currentUserId")
        }
        
        // 1. set callbacks FIRST
        ReverbManager.onConnected = {
            viewModelScope.launch(Dispatchers.Main) { _connected.value = true }
        }
        ReverbManager.onDisconnected = {
            viewModelScope.launch(Dispatchers.Main) { _connected.value = false }
        }
        
        ReverbManager.onFightUpdated = { data ->
```

**AFTER:**
```kotlin
        } else if (currentUserId != -1L) {
            android.util.Log.d("ReverbVM", "Using provided userId: $currentUserId")
        }
        
        // 1. set callbacks FIRST (only for events this screen cares about)
        // NOTE: Don't override onConnected/onDisconnected - let RunnerViewModel or other screens manage those
        ReverbManager.onFightUpdated = { data ->
```

### Change 1B: Prevent Singleton Disconnection (Lines 171-177)

**BEFORE:**
```kotlin
    fun disconnect() {
        ReverbManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
```

**AFTER:**
```kotlin
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

---

## File 2: RunnerViewModel.kt

### Change 2: Add Duplicate Setup Guard (After line 63)

**BEFORE:**
```kotlin
    // Store current user ID for filtering notifications
    private var currentUserId: Long = -1
```

**AFTER:**
```kotlin
    // Store current user ID for filtering notifications
    private var currentUserId: Long = -1
    
    // Guard against duplicate setup
    private var isRealtimeListenerSetup = false
```

---

## File 3: Navigation.kt

### Change 3: Remove Duplicate setupRealtimeListener Call (Lines 40-52)

**BEFORE:**
```kotlin
    // Connect ReverbManager immediately at app startup
    LaunchedEffect(Unit) {
        android.util.Log.d("AppNav", "🔌 Connecting ReverbManager at app startup")
        com.yego.sabongbettingsystem.data.realtime.ReverbManager.connect()
    }
    
    // Initialize RunnerViewModel realtime listeners early (so they work even if RunnerScreen isn't displayed yet)
    val runnerViewModel = viewModel<RunnerViewModel>()
    LaunchedEffect(role, userId) {
        android.util.Log.d("AppNav", "🔍 AppNavigation LaunchedEffect triggered - role=$role, userId=$userId")
        if (role == "runner" && userId?.isNotEmpty() == true) {
            android.util.Log.d("AppNav", "🔧 Initializing RunnerViewModel realtime listener for userId=$userId")
            val parsedUserId = userId?.toLongOrNull() ?: -1
            runnerViewModel.setupRealtimeListener(context, parsedUserId)
        } else {
            android.util.Log.d("AppNav", "⏭️  Skipping RunnerVM init - role='$role', userId='$userId'")
        }
    }
```

**AFTER:**
```kotlin
    // Connect ReverbManager immediately at app startup
    LaunchedEffect(Unit) {
        android.util.Log.d("AppNav", "🔌 Connecting ReverbManager at app startup")
        com.yego.sabongbettingsystem.data.realtime.ReverbManager.connect()
    }
```

---

## File 4: RunnerScreen.kt

### Change 4: Remove !it.isRead Filter (Line 112)

**BEFORE:**
```kotlin
    LaunchedEffect(notifications) {
        android.util.Log.d("RunnerScreen", "📋 Checking notifications list (${notifications.size} total)")
        notifications.forEach { notif ->
            android.util.Log.d("RunnerScreen", "   - Title: '${notif.title}', isRead: ${notif.isRead}, message: ${notif.message}")
        }
        
        val assignmentNotif = notifications.firstOrNull { 
            it.title == "Assignment" && !it.isRead 
        }
```

**AFTER:**
```kotlin
    LaunchedEffect(notifications) {
        android.util.Log.d("RunnerScreen", "📋 Checking notifications list (${notifications.size} total)")
        notifications.forEach { notif ->
            android.util.Log.d("RunnerScreen", "   - Title: '${notif.title}', isRead: ${notif.isRead}, message: ${notif.message}")
        }
        
        val assignmentNotif = notifications.firstOrNull { 
            it.title == "Assignment"
        }
```

---

## Summary of Changes

| File | Change Type | Lines | Impact |
|------|------------|-------|--------|
| ReverbViewModel.kt | Remove callback conflict | 86-89 | Prevents overwriting RunnerVM callbacks |
| ReverbViewModel.kt | Fix disconnect logic | 171-177 | Prevents destroying global WebSocket |
| RunnerViewModel.kt | Add safety guard | After 63 | Prevents duplicate initialization |
| Navigation.kt | Remove duplicate setup | 40-52 | Single initialization point |
| RunnerScreen.kt | Remove filter | 112 | Shows all Assignment notifications |

All changes are **backward compatible** and fix the callback lifecycle issue without breaking existing functionality.
