package weddellseal.markrecap.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import weddellseal.markrecap.models.WedCheckViewModel


@Composable
fun FilePermissionsAlertDialog(
    context: Context,
    requestCode: Int,
    onDismiss: () -> Unit
) {
    // prompt the user to grant necessary permissions
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Storage Permissions Required") },
        text = {
            Text("To upload files from various folders on your device, this app needs permission to access files.")
        },
        confirmButton = {
            Button(onClick = {
                enableFileAccessForAndroidVersion(context, requestCode)
                onDismiss() // Dismiss the dialog after clicking confirm
            }) {
                Text("Enable File Access Permissions")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        }
    )
}

fun enableFileAccessForAndroidVersion(context: Context, requestCode: Int) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        // For Android 10 and below, enable READ_EXTERNAL_STORAGE permissions
        val readPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            // Need to request READ and WRITE permissions
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }

    } else if (!Environment.isExternalStorageManager()) {
        // For Android 11 and above, request All Files Access permission
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}") // which app this permission request is for
        )
        context.startActivity(intent)
    }
}