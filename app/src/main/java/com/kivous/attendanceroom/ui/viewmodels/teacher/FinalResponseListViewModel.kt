package com.kivous.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FinalResponseListViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getFinalResponseList(classCode: String, attendanceId: String) =
        repository.getFinalResponseList(classCode, attendanceId)

}