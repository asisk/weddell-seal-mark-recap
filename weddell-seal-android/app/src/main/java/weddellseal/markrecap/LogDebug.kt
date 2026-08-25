package weddellseal.markrecap

import android.util.Log

/** Debug-only logs. Release sets [BuildConfig.DEBUG] false and R8 strips the body. */
inline fun logDebug(tag: String, message: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, message())
    }
}
