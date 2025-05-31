package weddellseal.markrecap.ui.utils

import android.content.Context
import android.provider.Settings

 fun getDeviceName(context: Context): String {
    return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        ?: "Unknown Device"
}