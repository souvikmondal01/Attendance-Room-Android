package com.kivous.attendanceroom.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AttendanceHistoryStudentViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getStudentAttendanceHistoryRecyclerOptions(classCode: String) =
        repository.getStudentAttendanceHistoryRecyclerOptions(classCode)

    fun getStudentAttendanceHistory(classCode: String) =
        repository.getStudentAttendanceHistory(classCode)

}