package com.souvikmondal01.attendanceroom.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    private val _sharedClassCode = MutableLiveData<String>()
    val sharedClassCode get() = _sharedClassCode
    fun shareClassCode(code: String) {
        _sharedClassCode.value = code
    }
}
