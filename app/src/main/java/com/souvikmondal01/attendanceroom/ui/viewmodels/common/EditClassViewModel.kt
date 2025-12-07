package com.souvikmondal01.attendanceroom.ui.viewmodels.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditClassViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    val classNameTemp = MutableLiveData<String>()
    val departmentTemp = MutableLiveData<String>()
    val batchTemp = MutableLiveData<String>()
    val subjectTemp = MutableLiveData<String>()

    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)

    private val _isClassRoomEdited = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRoomEdited get() = _isClassRoomEdited
    fun editClassroomDetails(map: Map<String, Any>, code: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.editClassroomDetails(map, code).collect {
                _isClassRoomEdited.value = it
            }
        }


}