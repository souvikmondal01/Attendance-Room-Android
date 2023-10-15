package com.kivous.attendanceroom.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.data.models.Chat
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.data.models.Location
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.data.repositories.AppRepositoryImp
import com.kivous.attendanceroom.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: AppRepositoryImp
) : ViewModel() {

    private val _sharedClassCode = MutableLiveData<String>()
    val sharedClassCode get() = _sharedClassCode
    fun shareClassCode(code: String) {
        _sharedClassCode.value = code
    }

    val classNameTemp = MutableLiveData<String>()
    val departmentTemp = MutableLiveData<String>()
    val batchTemp = MutableLiveData<String>()
    val subjectTemp = MutableLiveData<String>()

    val codeTemp = MutableLiveData<String>()
    val latitudeTemp = MutableLiveData<String>()
    val longitudeTemp = MutableLiveData<String>()
    val radiusTemp = MutableLiveData<String>()

    private val _isUserSetToFirestore = MutableStateFlow<Response<String>>(Response.Error(""))
    val isUserSetToFirestore get() = _isUserSetToFirestore

    fun setUserDetailsToFirestore() {
        viewModelScope.launch {
            repository.setUserDetailsToFirestore().collect {
                _isUserSetToFirestore.value = it
            }
        }
    }

    private val _code = MutableStateFlow("")
    val code get() = _code
    fun generateClassJoinCode() = repository.generateClassJoinCode {
        _code.value = it
    }

    private val _isClassRoomCreated = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRoomCreated get() = _isClassRoomCreated

    fun createClassRoom(classRoom: ClassRoom, code: String) {
        viewModelScope.launch {
            repository.createClassRoom(classRoom, code).collect {
                _isClassRoomCreated.value = it
            }
        }
    }

    fun addCodeToFirestoreCodeList(code: String) = repository.addCodeToFirestoreCodeList(code)

    private val _classRoomFirestoreRecyclerOptions =
        repository.getClassRoomFirestoreRecyclerOptions(false)
    val classRoomFirestoreRecyclerOptions get() = _classRoomFirestoreRecyclerOptions

    private val _archivedClassRoomFirestoreRecyclerOptions =
        repository.getClassRoomFirestoreRecyclerOptions(true)
    val archivedClassRoomFirestoreRecyclerOptions get() = _archivedClassRoomFirestoreRecyclerOptions

    private val _studentsFirestoreRecyclerOptions =
        MutableStateFlow<Response<FirestoreRecyclerOptions<User>>>(Response.Error(""))
    val studentsFirestoreRecyclerOptions get() = _studentsFirestoreRecyclerOptions

    fun getStudentsFirestoreRecyclerOptions(code: String) {
        viewModelScope.launch {
            repository.getStudentsFirestoreRecyclerOptions(code).collect {
                _studentsFirestoreRecyclerOptions.value = it
            }
        }

    }

    private val _isJoinedToClassWithCode = MutableStateFlow<Response<String>>(Response.Error(""))
    val isJoinedToClassWithCode get() = _isJoinedToClassWithCode

    fun joinClassWithCode(code: String) {
        viewModelScope.launch {
            repository.joinClassWithCode(code).collect {
                _isJoinedToClassWithCode.value = it
            }
        }
    }

    private val _isClassListEmpty = repository.isClassListEmpty(false)
    val isClassListEmpty get() = _isClassListEmpty

    private val _isArchiveClassListEmpty = repository.isClassListEmpty(true)
    val isArchiveClassListEmpty get() = _isArchiveClassListEmpty

    private val _isClassArchived = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassArchived get() = _isClassArchived
    fun archiveClassroom(code: String) {
        viewModelScope.launch {
            repository.archiveClassroom(code).collect {
                _isClassArchived.value = it
            }
        }
    }

    private val _isClassRestored = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRestored get() = _isClassRestored
    fun restoreClassroom(code: String) {
        viewModelScope.launch {
            repository.restoreClassroom(code).collect {
                _isClassRestored.value = it
            }
        }
    }

    private val _isClassDeleted = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassDeleted get() = _isClassDeleted
    fun deleteClassroom(code: String) {
        viewModelScope.launch {
            repository.deleteClassroom(code).collect {
                _isClassDeleted.value = it
            }
        }
    }

    private val _isUnEnrolledFromClass = MutableStateFlow<Response<String>>(Response.Error(""))
    val isUnEnrolledFromClass get() = _isUnEnrolledFromClass
    fun unEnrollFromClass(code: String) {
        viewModelScope.launch {
            repository.unEnrollFromClass(code).collect {
                _isUnEnrolledFromClass.value = it
            }
        }
    }

    private val _classRoomDetails = MutableStateFlow<Response<ClassRoom>>(Response.Error(""))
    val classRoomDetails get() = _classRoomDetails

    fun getClassRoomDetails(code: String) {
        viewModelScope.launch {
            repository.getClassRoomDetails(code).collect {
                _classRoomDetails.value = it
            }
        }
    }

    private val _isClassRoomEdited = MutableStateFlow<Response<String>>(Response.Error(""))
    val isClassRoomEdited get() = _isClassRoomEdited

    fun editClassroomDetails(map: Map<String, Any>, code: String) {
        viewModelScope.launch {
            repository.editClassroomDetails(map, code).collect {
                _isClassRoomEdited.value = it
            }
        }
    }

    fun setLocationToFirestore(location: Location) {
        viewModelScope.launch {
            repository.setLocationToFirestore(location).collect {}
        }
    }

    private val _isStudentListEmpty = MutableStateFlow<Boolean>(false)
    val isStudentListEmpty get() = _isStudentListEmpty
    fun isStudentListEmpty(code: String) {
        viewModelScope.launch {
            repository.isStudentListEmpty(code).collect {
                _isStudentListEmpty.value = it
            }
        }
    }

    private val _isStudentRemovedFromClassRoomByTeacher =
        MutableStateFlow<Response<String>>(Response.Error(""))
    val isStudentRemovedFromClassRoomByTeacher get() = _isStudentRemovedFromClassRoomByTeacher

    fun removeStudentFromClassRoomByTeacher(
        code: String, user: User
    ) {
        viewModelScope.launch {
            repository.removeStudentFromClassRoomByTeacher(code, user).collect {
                _isStudentRemovedFromClassRoomByTeacher.value = it
            }
        }
    }

    private val _isAttendanceCardCreated = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceCardCreated get() = _isAttendanceCardCreated

    fun createAttendanceCard(
        attendance: Attendance, code: String
    ) {
        viewModelScope.launch {
            repository.createAttendanceCard(attendance, code).collectLatest {
                _isAttendanceCardCreated.value = it
            }
        }

    }

    private val _attendanceCardTeacherRecyclerOptions =
        MutableStateFlow<Response<FirestoreRecyclerOptions<Attendance>>>(Response.Error(""))
    val attendanceCardTeacherRecyclerOptions get() = _attendanceCardTeacherRecyclerOptions

    fun getAttendanceCardTeacherRecyclerOptions(code: String) {
        viewModelScope.launch {
            repository.getAttendanceCardTeacherRecyclerOptions(code).collect {
                _attendanceCardTeacherRecyclerOptions.value = it
            }
        }
    }

    private val _isAttendanceCardListEmpty = MutableLiveData<Boolean>()
    val isAttendanceCardListEmpty get() = _isAttendanceCardListEmpty
    fun isAttendanceCardListEmpty(code: String) {
        viewModelScope.launch {
            repository.isAttendanceCardListEmpty(code).collect {
                _isAttendanceCardListEmpty.value = it
            }
        }
    }

    private val _isAttendanceCardDeleted = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceCardDeleted get() = _isAttendanceCardDeleted

    fun deleteAttendanceCard(code: String, id: String) {
        viewModelScope.launch {
            repository.deleteAttendanceCard(code, id).collect {
                _isAttendanceCardDeleted.value = it
            }
        }
    }

    private val _toggleLocationCheck = MutableStateFlow<Response<String>>(Response.Error(""))
    val toggleLocationCheck get() = _toggleLocationCheck

    fun toggleLocationCheck(code: String, id: String) {
        viewModelScope.launch {
            repository.toggleLocationCheck(code, id).collect {
                _toggleLocationCheck.value = it
            }
        }
    }

    private val _toggleSingleDeviceSingleResponse =
        MutableStateFlow<Response<String>>(Response.Error(""))
    val toggleSingleDeviceSingleResponse get() = _toggleSingleDeviceSingleResponse

    fun toggleSingleDeviceSingleResponse(
        code: String, id: String
    ) {
        viewModelScope.launch {
            repository.toggleSingleDeviceSingleResponse(code, id).collect {
                _toggleSingleDeviceSingleResponse.value = it
            }
        }

    }


    private val _isAttendanceTakingDone = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceTakingDone get() = _isAttendanceTakingDone

    fun doneTakingAttendance(code: String, id: String) {
        viewModelScope.launch {
            repository.doneTakingAttendance(code, id).collect {
                _isAttendanceTakingDone.value = it
            }
        }
    }

    private val _locationCheckStatus = MutableStateFlow(false)
    val locationCheckStatus get() = _locationCheckStatus
    fun getLocationCheckStatus(code: String, id: String) {
        viewModelScope.launch {
            repository.getLocationCheckStatus(code, id).collect {
                _locationCheckStatus.value = it

            }
        }
    }


    private val _singleDeviceSingleResponseStatus = MutableStateFlow(false)
    val singleDeviceSingleResponseStatus get() = _singleDeviceSingleResponseStatus
    fun getSingleDeviceSingleResponseStatus(code: String, id: String) {
        viewModelScope.launch {
            repository.getSingleDeviceSingleResponseStatus(code, id).collect {
                _singleDeviceSingleResponseStatus.value = it
            }
        }
    }


    private val _isAttendanceResponseGiven = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceResponseGiven get() = _isAttendanceResponseGiven
    fun giveAttendanceResponse(
        androidId: String, classCode: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.giveAttendanceResponse(androidId, classCode, attendanceId).collect {
                _isAttendanceResponseGiven.value = it
            }
        }

    }

    private val _responseFirestoreRecyclerOptions =
        MutableStateFlow<Response<FirestoreRecyclerOptions<User>>>(Response.Error(""))
    val responseFirestoreRecyclerOptions get() = _responseFirestoreRecyclerOptions

    fun getResponseFirestoreRecyclerOptions(
        classCode: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.getResponseFirestoreRecyclerOptions(classCode, attendanceId).collect {
                _responseFirestoreRecyclerOptions.value = it
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

    private val _responseCount = MutableStateFlow(-1)
    val responseCount get() = _responseCount

    fun getResponseCount(classCode: String, attendanceId: String) {
        viewModelScope.launch {
            repository.getResponseCount(classCode, attendanceId).collect {
                _responseCount.value = it
            }
        }
    }

    fun setFinalResponseList(
        classCode: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.setFinalResponseList(classCode, attendanceId).collect {}
        }
    }

    private val _isResponseAlreadyGivenFromThisDevice = MutableStateFlow(false)
    val isResponseAlreadyGivenFromThisDevice get() = _isResponseAlreadyGivenFromThisDevice
    fun getIsResponseAlreadyGivenFromThisDevice(
        activity: Activity, code: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.getIsResponseAlreadyGivenFromThisDevice(activity, code, attendanceId)
                .collect {
                    _isResponseAlreadyGivenFromThisDevice.value = it
                }
        }
    }

    private val _studentList = MutableStateFlow<Response<List<User>>>(Response.Error(""))
    val studentList get() = _studentList
    fun getStudentList(classCode: String) {
        viewModelScope.launch {
            repository.getStudentList(classCode).collect {
                _studentList.value = it
            }
        }
    }


    private val _isManualResponseSet = MutableStateFlow<Response<String>>(Response.Error(""))
    val isManualResponseSet get() = _isManualResponseSet
    fun setManualResponseList(classCode: String, attendance: Attendance) {
        viewModelScope.launch {
            repository.setManualResponseList(classCode, attendance).collect {
                _isManualResponseSet.value = it
            }
        }
    }


    private val _attendanceHistoryList =
        MutableStateFlow<Response<List<Attendance>>>(Response.Error(""))
    val attendanceHistoryList get() = _attendanceHistoryList
    fun getAttendanceHistory(classCode: String) {
        viewModelScope.launch {
            repository.getAttendanceHistory(classCode).collectLatest {
                _attendanceHistoryList.value = it
            }
        }
    }

    private val _finalResponseList = MutableStateFlow<Response<List<User>>>(Response.Error(""))
    val finalResponseList get() = _finalResponseList

    fun getFinalResponseList(
        classCode: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.getFinalResponseList(classCode, attendanceId).collect {
                _finalResponseList.value = it
            }
        }

    }

    fun setAttendanceHistoryStudent(classCode: String, attendance: Attendance) {
        viewModelScope.launch {
            repository.setAttendanceHistoryStudent(classCode, attendance).collect {}
        }
    }

    private val _studentAttendanceHistoryRecyclerOptions =
        MutableStateFlow<Response<FirestoreRecyclerOptions<Attendance>>>(Response.Error(""))
    val studentAttendanceHistoryRecyclerOptions get() = _studentAttendanceHistoryRecyclerOptions
    fun getStudentAttendanceHistoryRecyclerOptions(classCode: String) {
        viewModelScope.launch {
            repository.getStudentAttendanceHistoryRecyclerOptions(classCode).collect {
                _studentAttendanceHistoryRecyclerOptions.value = it
            }
        }
    }

    private val _studentAttendanceHistory =
        MutableStateFlow<Response<List<Attendance>>>(Response.Error(""))
    val studentAttendanceHistory get() = _studentAttendanceHistory
    fun getStudentAttendanceHistory(classCode: String) {
        viewModelScope.launch {
            repository.getStudentAttendanceHistory(classCode).collect {
                _studentAttendanceHistory.value = it
            }
        }
    }

    private val _isAttendanceHistoryDeleted = MutableStateFlow<Response<String>>(Response.Error(""))
    val isAttendanceHistoryDeleted get() = _isAttendanceHistoryDeleted
    fun deleteAttendanceHistory(
        classCode: String, attendanceId: String
    ) {
        viewModelScope.launch {
            repository.deleteAttendanceHistory(classCode, attendanceId).collect {
                _isAttendanceHistoryDeleted.value = it
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
