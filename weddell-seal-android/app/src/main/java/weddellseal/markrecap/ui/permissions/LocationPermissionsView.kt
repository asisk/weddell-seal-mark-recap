package weddellseal.markrecap.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import weddellseal.markrecap.R

// In-app disclosure copy shown before the system location prompt.
internal const val LOCATION_DISCLOSURE_TITLE = "Precise location"
internal const val LOCATION_DISCLOSURE_INTRO =
    "Weddell Seal Mark Recap uses precise GPS (latitude and longitude) while you are in the app."
internal const val LOCATION_DISCLOSURE_STAMP =
    "Stamp each observation with where it was recorded"
internal const val LOCATION_DISCLOSURE_COLONY =
    "Detect which seal colony this tablet is in"
internal const val LOCATION_DISCLOSURE_RETENTION =
    "Location stays on this device until you export a CSV. The app cannot send it to a server."
internal const val LOCATION_DISCLOSURE_CONTINUE_HINT =
    "Continue asks Android for Fine and Coarse location."
internal const val LOCATION_DISCLOSURE_DENIED =
    "Location was not allowed. Enable it in Settings, or continue and choose a colony yourself."

/** Pre-prompt disclosure; [onLocationGranted] proceeds with GPS, [onSkip] continues without it. */
@Composable
fun LocationPermissionView(
    onLocationGranted: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnGranted by rememberUpdatedState(onLocationGranted)
    var requestPermissions by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }

    // If the user grants location in Settings, leave this screen on resume.
    // onDispose only removes the observer — it does not cancel location jobs.
    DisposableEffect(lifecycleOwner) {
        fun leaveIfGranted() {
            if (context.locationPermissionsGranted()) {
                currentOnGranted()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                leaveIfGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            leaveIfGranted()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // System permission dialog; stay here and show the denied UI if the user refuses.
    if (requestPermissions) {
        RequestPermissions(context.locationPermissions()) { granted ->
            requestPermissions = false
            if (granted) {
                onLocationGranted()
            } else {
                denied = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                painter = painterResource(R.drawable.ic_location_on),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = LOCATION_DISCLOSURE_TITLE,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = LOCATION_DISCLOSURE_INTRO,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "This tablet uses it to:",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "• $LOCATION_DISCLOSURE_STAMP",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "• $LOCATION_DISCLOSURE_COLONY",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = LOCATION_DISCLOSURE_RETENTION,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = if (denied) LOCATION_DISCLOSURE_DENIED else LOCATION_DISCLOSURE_CONTINUE_HINT,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (denied) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (denied) {
            // After a denial, Android will not show the prompt again; send the user to app Settings.
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            ) {
                Text("Open Settings", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = onSkip
            ) {
                Text("Continue without location", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            // First pass: Continue shows the system prompt; Not now skips location entirely.
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !requestPermissions,
                onClick = { requestPermissions = true }
            ) {
                Text("Continue", style = MaterialTheme.typography.titleLarge)
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !requestPermissions,
                onClick = onSkip
            ) {
                Text("Not now", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
