package weddellseal.markrecap.domain.location

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
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
 * Later live fixes are sampled: only the latest fix in each [samplePeriodMs] window is kept,
 * and it is emitted when that window expires (even if no further provider callbacks arrive).
 */
fun Flow<GeoLocation>.sampleAfterFirstLiveFix(
    samplePeriodMs: Long = LOCATION_SAMPLE_PERIOD_MS,
    nowMs: () -> Long = { System.currentTimeMillis() },
): Flow<GeoLocation> = channelFlow {
    coroutineScope {
        var hasEmittedLiveFix = false
        var lastEmitAt = 0L
        var pendingLive: GeoLocation? = null
        var flushJob: Job? = null

        suspend fun emitNow(location: GeoLocation) {
            flushJob?.cancel()
            flushJob?.join()
            flushJob = null
            pendingLive = null
            if (location.isLiveFix) hasEmittedLiveFix = true
            lastEmitAt = nowMs()
            send(location)
        }

        fun schedulePendingFlush() {
            if (flushJob?.isActive == true) return
            flushJob = launch {
                val remainingMs = (samplePeriodMs - (nowMs() - lastEmitAt)).coerceAtLeast(0L)
                delay(remainingMs)
                val toEmit = pendingLive ?: return@launch
                pendingLive = null
                flushJob = null
                if (toEmit.isLiveFix) hasEmittedLiveFix = true
                lastEmitAt = nowMs()
                send(toEmit)
            }
        }

        collect { location ->
            if (shouldEmitSampledLocation(
                    isLiveFix = location.isLiveFix,
                    hasEmittedLiveFix = hasEmittedLiveFix,
                    elapsedSinceLastEmitMs = nowMs() - lastEmitAt,
                    samplePeriodMs = samplePeriodMs,
                )
            ) {
                emitNow(location)
            } else {
                // Throttled live fix: keep the latest and emit when the sample window expires.
                pendingLive = location
                schedulePendingFlush()
            }
        }
    }
}
