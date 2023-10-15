package com.kivous.attendanceroom.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

object Common {
    fun isDarkMode(context: Context): Boolean {
        val darkModeFlag =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the earth
        val latDistance = Math.toRadians(kotlin.math.abs(lat2 - lat1))
        val lonDistance = Math.toRadians(kotlin.math.abs(lon2 - lon1))
        val a = (kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2)
                + (kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2))
                * kotlin.math.sin(lonDistance / 2) * kotlin.math.sin(lonDistance / 2)))
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        var distance = r * c * 1000 // distance in meter
        distance = distance.pow(2.0)
        return kotlin.math.sqrt(distance)
    }

    /**
    Check is user in the geofence or not
     */
    fun isInFence(
        setLat: Double,
        setLong: Double,
        yourLat: Double,
        yourLong: Double,
        radius: Double
    ): Boolean {
        return getDistance(setLat, setLong, yourLat, yourLong) <= radius
    }

    fun Fragment.hasLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = ContextCompat.checkSelfPermission(requireContext(), permission)
        return result == PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("SetTextI18n")
    fun Fragment.datePicker(editText: EditText) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
        datePicker.addOnPositiveButtonClickListener {
            val date = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(it)
            editText.setText(date)
        }
        datePicker.show(childFragmentManager, "")
    }


    fun date(): String {
        return SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())
    }

    fun currentDate(): String {
        return SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
    }

    fun currentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun timeStamp(): Long {
        return System.currentTimeMillis()
    }

    @SuppressLint("HardwareIds")
    fun Fragment.androidId(): String {
        return Settings.Secure.getString(
            requireActivity().contentResolver, Settings.Secure.ANDROID_ID
        )
    }

}