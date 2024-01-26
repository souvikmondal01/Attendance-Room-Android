package com.kivous.attendanceroom.ui.viewmodels.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateClassViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    private val _classJoinCode = MutableStateFlow("")
    val classJoinCode get() = _classJoinCode
    fun generateClassJoinCode() = repository.generateClassJoinCode {
        _classJoinCode.value = it
    }

    private val _isClassRoomCreated = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRoomCreated get() = _isClassRoomCreated
    fun createClassRoom(classRoom: ClassRoom, code: String) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.createClassRoom(classRoom, code).collect {
                _isClassRoomCreated.value = it
            }
        }

    fun addCodeToFirestoreCodeList(code: String) = repository.addCodeToFirestoreCodeList(code)


}