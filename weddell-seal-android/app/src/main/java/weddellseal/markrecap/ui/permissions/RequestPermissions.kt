package weddellseal.markrecap.ui.permissions


import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

private const val TAG = "RequestPermissions"

@Composable
fun RequestPermissions(
    permissions: List<String>,
    onCompleted: (Boolean) -> Unit,
) {
    Log.i(TAG, "requesting permissions:\n${permissions.joinToString(separator = "\n")}")
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.i(
            TAG,
            "permissions result:\n${results.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }}",
        )
        // Prefer "any requested permission granted" over values.all: an empty map or a
        // partial FINE/COARSE pair should not be treated as a hard deny by itself.
        // Callers that need a precise check should re-query PackageManager.
        val granted = results.isNotEmpty() && results.values.any { it }
        Log.i(TAG, "Reporting onCompleted($granted)")
        onCompleted(granted)
    }
    LaunchedEffect(Unit) {
        Log.i(TAG, "Launching permission request...")
        activityResultLauncher.launch(permissions.toTypedArray())
    }
}
