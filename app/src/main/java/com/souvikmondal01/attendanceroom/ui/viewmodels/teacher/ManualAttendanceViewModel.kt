package com.souvikmondal01.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualAttendanceViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getStudentList(classCode: String) = repository.getStudentList(classCode)

    private val _isManualResponseSet = MutableStateFlow<Response<String>>(Response.Error(""))
    val isManualResponseSet get() = _isManualResponseSet
    fun setManualResponseList(classCode: String, attendance: Attendance) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.setManualResponseList(classCode, attendance).collect {
                _isManualResponseSet.value = it
            }
        }

}