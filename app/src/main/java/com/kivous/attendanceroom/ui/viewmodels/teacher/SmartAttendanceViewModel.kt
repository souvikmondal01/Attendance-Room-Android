package com.kivous.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartAttendanceViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    val codeTemp = MutableLiveData<String>()
    val latitudeTemp = MutableLiveData<String>()
    val longitudeTemp = MutableLiveData<String>()
    val radiusTemp = MutableLiveData<String>()

    fun isAttendanceCardListEmpty(code: String) = repository.isAttendanceCardListEmpty(code)

    private val _isAttendanceCardCreated = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceCardCreated get() = _isAttendanceCardCreated
    fun createAttendanceCard(attendance: Attendance, code: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.createAttendanceCard(attendance, code).collect {
                _isAttendanceCardCreated.value = it
            }
        }

    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)


}