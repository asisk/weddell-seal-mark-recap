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
    // Create an activity result launcher using permissions contract & handle the result
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.i(TAG, "permissions result:\n${results.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }}")
        val allGranted = results.values.all { it }
        Log.i(TAG, "All permissions granted: $allGranted")
        if (allGranted) {
            Log.i(TAG, "Calling onCompleted(true)")
            onCompleted(true)
        } else {
            Log.i(TAG, "Calling onCompleted(false)")
            onCompleted(false)
        }
    }
    // Request permissions
    LaunchedEffect(Unit) {
        Log.i(TAG, "Launching permission request...")
        activityResultLauncher.launch(permissions.toTypedArray())
    }
}