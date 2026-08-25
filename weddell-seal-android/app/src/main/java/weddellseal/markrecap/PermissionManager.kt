package weddellseal.markrecap

/*
ObservationLog mainly accesses user data when we access the user's location
at the beginning of the AddObservationLogScreen load. This class tracks and manages
the permissions associated with the application.
*/
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager(private val context: Context) {
    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION
        )
    }

    data class State(
        val hasLocationAccess: Boolean
    ) {
        val hasAllAccess: Boolean
            get() = hasLocationAccess
    }

    private val _state = MutableStateFlow(
        State(
            hasLocationAccess = hasLocationAccess(),
        )
    )
    val state = _state.asStateFlow()
    val hasAllPermissions: Boolean
        get() = _state.value.hasAllAccess

    private fun hasAccess(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationAccess(): Boolean {
        return hasAccess(ACCESS_FINE_LOCATION) && hasAccess(ACCESS_COARSE_LOCATION)
    }

    fun onPermissionChange(permissions: Map<String, Boolean>) {
        val hasFine = permissions[ACCESS_FINE_LOCATION] ?: hasAccess(ACCESS_FINE_LOCATION)
        val hasCoarse = permissions[ACCESS_COARSE_LOCATION] ?: hasAccess(ACCESS_COARSE_LOCATION)
        _state.value = State(
            hasLocationAccess = hasFine && hasCoarse
        )
    }

    suspend fun checkPermissions() {
        _state.emit(State(hasLocationAccess = hasLocationAccess()))
    }

    fun createSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", context.packageName, null)
        }

        return intent
    }
}
