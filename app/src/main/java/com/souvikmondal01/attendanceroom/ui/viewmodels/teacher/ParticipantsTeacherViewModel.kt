package com.souvikmondal01.attendanceroom.ui.viewmodels.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souvikmondal01.attendanceroom.data.models.User
import com.souvikmondal01.attendanceroom.data.repositories.AppRepositoryImp
import com.souvikmondal01.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParticipantsTeacherViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    fun getClassRoomDetails(code: String) = repository.getClassRoomDetails(code)

    fun getStudentsFirestoreRecyclerOptions(code: String) =
        repository.getStudentsFirestoreRecyclerOptions(code)

    fun isStudentListEmpty(code: String) = repository.isStudentListEmpty(code)

    private val _isStudentRemovedFromClassRoomByTeacher =
        MutableStateFlow<Response<String>>(Response.Error(""))

    val isStudentRemovedFromClassRoomByTeacher get() = _isStudentRemovedFromClassRoomByTeacher

    fun removeStudentFromClassRoomByTeacher(code: String, user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeStudentFromClassRoomByTeacher(code, user).collect {
                _isStudentRemovedFromClassRoomByTeacher.value = it
            }
        }

}