package com.souvikmondal01.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.ClassRoom
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassRoomSettingViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    private val _classRoomDetails = MutableStateFlow<Response<ClassRoom>>(Response.Error(""))
    val classRoomDetails get() = _classRoomDetails

    fun getClassRoomDetails(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getClassRoomDetails(code).collect {
                _classRoomDetails.value = it
            }
        }
    }

    private val _isClassRoomEdited = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRoomEdited get() = _isClassRoomEdited
    fun editClassroomDetails(map: Map<String, Any>, code: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.editClassroomDetails(map, code).collect {
                _isClassRoomEdited.value = it
            }
        }

}