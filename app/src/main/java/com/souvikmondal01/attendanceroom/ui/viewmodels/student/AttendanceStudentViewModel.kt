package com.souvikmondal01.attendanceroom.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceStudentViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)

    fun getAttendanceCardTeacherRecyclerOptions(code: String) =
        repository.getAttendanceCardTeacherRecyclerOptions(code)

    private val _isAttendanceResponseGiven = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceResponseGiven get() = _isAttendanceResponseGiven
    fun giveAttendanceResponse(androidId: String, classCode: String, attendanceId: String) =
        viewModelScope.launch {
            repository.giveAttendanceResponse(androidId, classCode, attendanceId).collect {
                _isAttendanceResponseGiven.value = it
            }
        }

    fun setAttendanceHistoryStudent(classCode: String, attendance: Attendance) {
        viewModelScope.launch {
            repository.setAttendanceHistoryStudent(classCode, attendance).collect {}
        }
    }

    fun isAttendanceCardListEmpty(code: String) = repository.isStudentAttendanceCardListEmpty(code)

}