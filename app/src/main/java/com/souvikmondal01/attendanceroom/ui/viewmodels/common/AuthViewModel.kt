package com.souvikmondal01.attendanceroom.ui.viewmodels.common

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
class AuthViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    private val _isUserSetToFirestore = MutableStateFlow<Response<String>>(Response.Error(""))
    val isUserSetToFirestore get() = _isUserSetToFirestore
    fun setUserDetailsToFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setUserDetailsToFirestore().collect {
                _isUserSetToFirestore.value = it
            }
        }
    }

}