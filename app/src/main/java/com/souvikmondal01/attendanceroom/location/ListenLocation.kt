package com.souvikmondal01.attendanceroom.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.souvikmondal01.attendanceroom.data.models.Location
import com.souvikmondal01.attendanceroom.utils.Response
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlin.math.pow

class ListenLocation @Inject constructor() {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    fun startLocationUpdates(context: Context) {

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest!!, locationCallback!!, null
        )
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
    }

    @SuppressLint("MissingPermission")
    fun getLocation(activity: Activity): Flow<Location> = callbackFlow {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            it?.let {
                val location =
                    Location(latitude = it.latitude.toString(), longitude = it.longitude.toString())
                trySend(location)
            }
        }

        awaitClose {
            cancel()
        }
    }


    fun isInClassRoom(
        context: Context,
        classLatitude: Double,
        classLongitude: Double,
        radius: Double,
    ): Flow<Response<Boolean>> = callbackFlow {
        trySend(Response.Loading())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500).setIntervalMillis(500)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
            }

            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0.lastLocation?.let {
                    val lat = p0.lastLocation!!.latitude
                    val long = p0.lastLocation!!.longitude
                    val data = isInFence(classLatitude, classLongitude, lat, long, radius)
                    trySend(Response.Success(data))
                }
            }
        }

        awaitClose {
            cancel()
        }
    }


    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the earth
        val latDistance = Math.toRadians(kotlin.math.abs(lat2 - lat1))
        val lonDistance = Math.toRadians(kotlin.math.abs(lon2 - lon1))
        val a =
            (kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2) + (kotlin.math.cos(
                Math.toRadians(lat1)
            ) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(lonDistance / 2) * kotlin.math.sin(
                lonDistance / 2
            )))
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        var distance = r * c * 1000 // distance in meter
        distance = distance.pow(2.0)
        return kotlin.math.sqrt(distance)
    }

    /**
    Check is user in the geofence or not
     */
    fun isInFence(
        setLat: Double, setLong: Double, yourLat: Double, yourLong: Double, radius: Double
    ): Boolean = getDistance(setLat, setLong, yourLat, yourLong) <= radius

}