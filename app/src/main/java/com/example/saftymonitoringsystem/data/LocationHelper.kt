package com.example.saftymonitoringsystem.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages GPS location tracking with continuous updates and reverse geocoding.
 *
 * Call [startTracking] to begin receiving location updates and [stopTracking] when done.
 * The latest location is exposed via [locationState].
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow<Location?>(null)
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    // ── One-shot ──────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun getLastLocation(onLocationReceived: (Location?) -> Unit) {
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            onLocationReceived(if (task.isSuccessful) task.result else null)
        }
    }

    // ── Continuous tracking ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                _locationState.value = result.lastLocation
            }
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, null)
    }

    fun stopTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Returns a "lat,lng" string or "Unknown". */
    fun locationToString(loc: Location?): String =
        if (loc != null) "%.6f, %.6f".format(loc.latitude, loc.longitude) else "Unknown"

    /** Returns a Google Maps URL for the given location. */
    fun locationToMapsUrl(loc: Location?): String =
        if (loc != null)
            "https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}"
        else ""

    /** Attempts reverse geocoding to produce a human-readable address (may return null). */
    @Suppress("Deprecation")
    fun getAddress(loc: Location?): String? {
        loc ?: return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (_: Exception) { null }
    }
}
