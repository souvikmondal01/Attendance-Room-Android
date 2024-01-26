package com.kivous.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceTeacherViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)

    fun getAttendanceCardTeacherRecyclerOptions(code: String) =
        repository.getAttendanceCardTeacherRecyclerOptions(code)

    fun isAttendanceCardListEmpty(code: String) = repository.isTeacherAttendanceCardListEmpty(code)

    fun getResponseCount(classCode: String, attendanceId: String) =
        repository.getResponseCount(classCode, attendanceId)

    fun doneTakingAttendance(code: String, id: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.doneTakingAttendance(code, id).collect {}
    }

    fun setFinalResponseList(classCode: String, attendanceId: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFinalResponseList(classCode, attendanceId).collect {}
        }

    fun getLocationCheckStatus(code: String, id: String) =
        repository.getLocationCheckStatus(code, id)

    fun getSingleDeviceSingleResponseStatus(code: String, id: String) =
        repository.getSingleDeviceSingleResponseStatus(code, id)

    fun deleteAttendanceCard(code: String, id: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAttendanceCard(code, id).collect {}
    }

    fun toggleLocationCheck(code: String, id: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.toggleLocationCheck(code, id).collect {}
    }

    fun toggleSingleDeviceSingleResponse(code: String, id: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleSingleDeviceSingleResponse(code, id).collect {}
        }

}