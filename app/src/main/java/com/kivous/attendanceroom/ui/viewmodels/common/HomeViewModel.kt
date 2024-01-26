package com.kivous.attendanceroom.ui.viewmodels.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kivous.attendanceroom.data.models.Location
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {

    fun getClassRoomFirestoreRecyclerOptions() =
        repository.getClassRoomFirestoreRecyclerOptions(false)

    private val _isClassArchived = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassArchived get() = _isClassArchived
    fun archiveClassroom(code: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.archiveClassroom(code).collect {
            _isClassArchived.value = it
        }
    }

    private val _isUnEnrolledFromClass = MutableStateFlow<Response<String>>(Response.Error(""))
    val isUnEnrolledFromClass get() = _isUnEnrolledFromClass
    fun unEnrollFromClass(code: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.unEnrollFromClass(code).collect {
            _isUnEnrolledFromClass.value = it
        }
    }

    fun isClassListEmpty() = repository.isClassListEmpty(false)

    fun setLocationToFirestore(location: Location) = viewModelScope.launch(Dispatchers.IO) {
        repository.setLocationToFirestore(location)
    }

}