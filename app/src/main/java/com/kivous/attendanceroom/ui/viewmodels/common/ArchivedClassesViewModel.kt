package com.kivous.attendanceroom.ui.viewmodels.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchivedClassesViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun archivedClassRoomFirestoreRecyclerOptions() =
        repository.getClassRoomFirestoreRecyclerOptions(true)

    private val _isClassRestored = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRestored get() = _isClassRestored
    fun restoreClassroom(code: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.restoreClassroom(code).collect {
            _isClassRestored.value = it
        }
    }

    private val _isClassDeleted = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassDeleted get() = _isClassDeleted
    fun deleteClassroom(code: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteClassroom(code).collect {
            _isClassDeleted.value = it
        }
    }

    fun isArchiveClassListEmpty() = repository.isClassListEmpty(true)


}