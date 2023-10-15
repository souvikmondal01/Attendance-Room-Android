package com.kivous.attendanceroom.location

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
import com.kivous.attendanceroom.data.models.Location
import com.kivous.attendanceroom.utils.Common.isInFence
import com.kivous.attendanceroom.utils.Response
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ListenLocation() {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    fun startLocationUpdates(context: Context) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!, null
        )
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
    }

    @SuppressLint("MissingPermission")
    fun getLocation(activity: Activity): Flow<Location> = callbackFlow {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(activity)

        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            if (it != null) {
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

}