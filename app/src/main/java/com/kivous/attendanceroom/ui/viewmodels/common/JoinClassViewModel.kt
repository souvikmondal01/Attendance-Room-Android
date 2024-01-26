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
class JoinClassViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {

    private val _isJoinedToClassWithCode = MutableStateFlow<Response<String>>(Response.Error(""))
    val isJoinedToClassWithCode get() = _isJoinedToClassWithCode

    fun joinClassWithCode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.joinClassWithCode(code).collect {
                _isJoinedToClassWithCode.value = it
            }
        }
    }
}