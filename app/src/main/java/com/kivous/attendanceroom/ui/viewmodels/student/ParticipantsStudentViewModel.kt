package com.kivous.attendanceroom.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ParticipantsStudentViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)

    fun getStudentsFirestoreRecyclerOptions(code: String) =
        repository.getStudentsFirestoreRecyclerOptions(code)
}