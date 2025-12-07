package com.souvikmondal01.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceHistoryTeacherViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    private val _attendanceHistoryList =
        MutableStateFlow<Response<List<Attendance>>>(Response.Error(""))
    val attendanceHistoryList get() = _attendanceHistoryList
    fun getAttendanceHistory(classCode: String) = viewModelScope.launch {
        repository.getAttendanceHistory(classCode).collectLatest {
            _attendanceHistoryList.value = it
        }
    }

    private val _isAttendanceHistoryDeleted = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceHistoryDeleted get() = _isAttendanceHistoryDeleted
    fun deleteAttendanceHistory(classCode: String, attendanceId: String) =
        viewModelScope.launch {
            repository.deleteAttendanceHistory(classCode, attendanceId).collect {
                _isAttendanceHistoryDeleted.value = it
            }
        }


}