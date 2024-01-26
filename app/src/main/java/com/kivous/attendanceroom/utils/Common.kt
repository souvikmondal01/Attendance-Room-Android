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

object Common {
    fun isDarkMode(context: Context): Boolean {
        val darkModeFlag =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }


    fun Fragment.hasLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = ContextCompat.checkSelfPermission(requireContext(), permission)
        return result == PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("SetTextI18n")
    fun Fragment.datePicker(editText: EditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
        datePicker.addOnPositiveButtonClickListener {
            val date = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(it)
            editText.setText(date)
        }
        datePicker.show(childFragmentManager, "")
    }


    fun date(): String = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())


    fun currentDate(): String = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())

    fun currentTime(): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    fun timeStamp(): Long = System.currentTimeMillis()

    @SuppressLint("HardwareIds")
    fun Fragment.androidId(): String = Settings.Secure.getString(
        requireActivity().contentResolver, Settings.Secure.ANDROID_ID
    )

}