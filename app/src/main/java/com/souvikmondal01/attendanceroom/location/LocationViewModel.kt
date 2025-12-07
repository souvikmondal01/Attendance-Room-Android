package com.souvikmondal01.attendanceroom.location

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.Location
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(private val listenLocation: ListenLocation) :
    ViewModel() {

    fun startLocationUpdates(context: Context) = listenLocation.startLocationUpdates(context)
    fun stopLocationUpdates() = listenLocation.stopLocationUpdates()

    private val _getLocation = MutableStateFlow(Location())
    val getLocation get() = _getLocation
    fun getLocation(activity: Activity) = viewModelScope.launch {
        listenLocation.getLocation(activity).collectLatest {
            _getLocation.value = it
        }
    }

    private val _isInClassRoom = MutableStateFlow<Response<Boolean>>(Response.Error(""))
    val isInClassRoom get() = _isInClassRoom
    fun isInClassRoom(
        context: Context, classLatitude: Double, classLongitude: Double, radius: Double
    ) = viewModelScope.launch {
        listenLocation.isInClassRoom(context, classLatitude, classLongitude, radius).collectLatest {
            _isInClassRoom.value = it
        }
    }


}