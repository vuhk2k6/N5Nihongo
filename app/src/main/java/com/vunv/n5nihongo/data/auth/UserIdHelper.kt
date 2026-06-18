package com.vunv.n5nihongo.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Returns the current active user ID for use in UserProgress queries.
 * Priority: Firebase UID > Guest UID > empty string.
 */
fun getCurrentUserId(context: Context? = null): String {
    // 1. Check Firebase user
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
    if (!firebaseUid.isNullOrBlank()) return firebaseUid

    // 2. Check Guest user
    if (context != null) {
        val prefs = context.getSharedPreferences("nihongo_guest_prefs", Context.MODE_PRIVATE)
        val guestActive = prefs.getBoolean("guest_active", false)
        if (guestActive) {
            val guestUid = prefs.getString("guest_uid", null)
            if (!guestUid.isNullOrBlank()) return guestUid
        }
    }

    return ""
}
