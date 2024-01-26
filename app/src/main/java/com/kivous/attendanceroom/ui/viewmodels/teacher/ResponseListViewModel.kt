package com.kivous.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResponseListViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getResponseFirestoreRecyclerOptions(classCode: String, attendanceId: String) =
        repository.getResponseFirestoreRecyclerOptions(classCode, attendanceId)

    fun getResponseCount(classCode: String, attendanceId: String) =
        repository.getResponseCount(classCode, attendanceId)

}