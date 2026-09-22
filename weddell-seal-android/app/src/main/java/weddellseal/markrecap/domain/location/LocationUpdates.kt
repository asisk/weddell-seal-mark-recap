package weddellseal.markrecap.domain.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import weddellseal.markrecap.domain.location.data.GeoLocation

internal const val LOCATION_SAMPLE_PERIOD_MS = 2000L

/** Colony miss ("not detected") is only shown once the live fix is this accurate. */
internal const val COLONY_DETECT_MAX_ACCURACY_METERS = 50f

internal const val LOCATION_EQUIVALENCE_METERS = 0.5

fun areLocationsEquivalentForUi(old: GeoLocation, new: GeoLocation): Boolean {
    if (old.isLiveFix != new.isLiveFix) return false
    if (isAccurateEnoughForColonyMiss(old.accuracyMeters) !=
        isAccurateEnoughForColonyMiss(new.accuracyMeters)
    ) return false
    return old.coordinates.distanceTo(new.coordinates) < LOCATION_EQUIVALENCE_METERS
}

fun shouldEmitSampledLocation(
    isLiveFix: Boolean,
    hasEmittedLiveFix: Boolean,
    elapsedSinceLastEmitMs: Long,
    samplePeriodMs: Long = LOCATION_SAMPLE_PERIOD_MS,
): Boolean {
    if (!isLiveFix) return true
    if (!hasEmittedLiveFix) return true
    return elapsedSinceLastEmitMs >= samplePeriodMs
}

fun shouldUseIncomingLocation(
    currentIsLive: Boolean,
    incomingIsLive: Boolean,
): Boolean = incomingIsLive || !currentIsLive

fun isAccurateEnoughForColonyMiss(
    accuracyMeters: Float?,
    maxAccuracyMeters: Float = COLONY_DETECT_MAX_ACCURACY_METERS,
): Boolean = accuracyMeters == null || accuracyMeters <= maxAccuracyMeters

/**
 * Cached locations and the first live fix are emitted immediately.
 * Later live fixes are sampled so the UI is not flooded.
 */
fun Flow<GeoLocation>.sampleAfterFirstLiveFix(
    samplePeriodMs: Long = LOCATION_SAMPLE_PERIOD_MS,
    nowMs: () -> Long = { System.currentTimeMillis() },
): Flow<GeoLocation> = flow {
    var hasEmittedLiveFix = false
    var lastEmitAt = 0L
    collect { location ->
        val elapsed = nowMs() - lastEmitAt
        if (shouldEmitSampledLocation(
                isLiveFix = location.isLiveFix,
                hasEmittedLiveFix = hasEmittedLiveFix,
                elapsedSinceLastEmitMs = elapsed,
                samplePeriodMs = samplePeriodMs,
            )
        ) {
            if (location.isLiveFix) hasEmittedLiveFix = true
            lastEmitAt = nowMs()
            emit(location)
        }
    }
}
