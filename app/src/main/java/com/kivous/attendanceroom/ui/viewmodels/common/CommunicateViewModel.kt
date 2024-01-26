package com.kivous.attendanceroom.ui.viewmodels.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.Chat
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunicateViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {
    private val _classRoomDetails = MutableStateFlow<Response<ClassRoom>>(Response.Error(""))
    val classRoomDetails get() = _classRoomDetails

    fun getClassRoomDetails(code: String) {
        viewModelScope.launch {
            repository.getClassRoomDetails(code).collect {
                _classRoomDetails.value = it
            }
        }
    }

    private val _chatFirestoreRecyclerOptions =
        MutableStateFlow<Response<FirestoreRecyclerOptions<Chat>>>(Response.Error(""))
    val chatFirestoreRecyclerOptions get() = _chatFirestoreRecyclerOptions
    fun getChatFirestoreRecyclerOptions(code: String) {
        viewModelScope.launch {
            repository.getChatFirestoreRecyclerOptions(code).collect {
                _chatFirestoreRecyclerOptions.value = it
            }
        }
    }

    private val _isChatSet = MutableStateFlow<Response<String>>(Response.Error(""))
    val isChatSet get() = _isChatSet
    fun setChatToFirestore(classCode: String, chat: Chat) {
        viewModelScope.launch {
            repository.setChatToFirestore(classCode, chat).collect {
                _isChatSet.value = it
            }
        }
    }

    fun deleteChat(classCode: String, chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(classCode, chatId).collect {}
        }
    }
}
