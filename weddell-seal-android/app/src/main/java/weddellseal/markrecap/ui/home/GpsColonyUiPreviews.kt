package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme

/**
 * Frozen Home GPS + colony states for Android Studio Split / Design view.
 * Last-known is often replaced by a live fix before you can look at it on a device.
 */
@Preview(
    name = "Last known — colony still waiting",
    showBackground = true,
    widthDp = 840,
    heightDp = 420,
)
@Composable
private fun PreviewLastKnownWhileColonyWaits() {
    GpsColonyPreviewScaffold {
        DeviceGPSRowContent(
            locationGranted = true,
            location = SAMPLE_LAST_KNOWN,
            onEnableLocation = {},
            onRefreshGps = {},
        )
        Spacer(Modifier.height(24.dp))
        ColonyPreviewRow(autoDetectedColony = null)
    }
}

@Preview(
    name = "Live GPS — colony detected",
    showBackground = true,
    widthDp = 840,
    heightDp = 420,
)
@Composable
private fun PreviewLiveGpsColonyDetected() {
    GpsColonyPreviewScaffold {
        DeviceGPSRowContent(
            locationGranted = true,
            location = SAMPLE_LIVE,
            onEnableLocation = {},
        )
        Spacer(Modifier.height(24.dp))
        ColonyPreviewRow(autoDetectedColony = SAMPLE_COLONY)
    }
}

@Preview(
    name = "Locating — no last known",
    showBackground = true,
    widthDp = 840,
    heightDp = 360,
)
@Composable
private fun PreviewLocatingNoCache() {
    GpsColonyPreviewScaffold {
        DeviceGPSRowContent(
            locationGranted = true,
            location = null,
            onEnableLocation = {},
        )
        Spacer(Modifier.height(24.dp))
        ColonyPreviewRow(autoDetectedColony = null)
    }
}

@Preview(
    name = "Live GPS — colony not detected",
    showBackground = true,
    widthDp = 840,
    heightDp = 420,
)
@Composable
private fun PreviewLiveGpsColonyNotDetected() {
    GpsColonyPreviewScaffold {
        DeviceGPSRowContent(
            locationGranted = true,
            location = SAMPLE_LIVE,
            onEnableLocation = {},
        )
        Spacer(Modifier.height(24.dp))
        ColonyPreviewRow(
            autoDetectedColony = SAMPLE_COLONY.copy(
                location = ColonyPopulation.NOT_DETECTED,
                adjLat = 0.0,
                adjLong = 0.0,
            )
        )
    }
}

@Preview(
    name = "Override confirm dialog",
    showBackground = true,
    widthDp = 640,
    heightDp = 400,
)
@Composable
private fun PreviewOverrideConfirmDialog() {
    WeddellSealMarkRecapTheme {
        ColonyOverrideConfirmDialog(onDismiss = {}, onConfirm = {})
    }
}

@Composable
private fun GpsColonyPreviewScaffold(content: @Composable () -> Unit) {
    WeddellSealMarkRecapTheme {
        Column(Modifier.padding(24.dp)) {
            Text("Home GPS + Colony (preview)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ColonyPreviewRow(autoDetectedColony: SealColony?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "Colony",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.width(28.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ColonyAutoDetectColumn(
                autoDetectedColony = autoDetectedColony,
                onOverrideClick = {},
            )
        }
    }
}

private val SAMPLE_LAST_KNOWN = GeoLocation(
    coordinates = Coordinates(latitude = -77.63004, longitude = 166.7941),
    updatedDate = "2026.09.20    22:14:03    -06:00  UTC",
    isLiveFix = false,
)

private val SAMPLE_LIVE = GeoLocation(
    coordinates = Coordinates(latitude = -77.63004, longitude = 166.7941),
    updatedDate = "2026.09.21    09:12:03    -06:00  UTC",
    accuracyMeters = 8f,
    isLiveFix = true,
)

private val SAMPLE_COLONY = SealColony(
    inOut = "in",
    location = "Hutton Cliffs",
    nLimit = -77.4,
    sLimit = -77.8,
    wLimit = 166.3,
    eLimit = 166.7,
    adjLat = -77.63004,
    adjLong = 166.7941,
    fileUploadId = 1L,
)
