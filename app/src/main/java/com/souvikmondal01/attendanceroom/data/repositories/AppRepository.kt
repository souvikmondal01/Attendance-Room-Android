package com.souvikmondal01.attendanceroom.data.repositories

import android.app.Activity
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.data.models.Chat
import com.souvikmondal01.attendanceroom.data.models.ClassRoom
import com.souvikmondal01.attendanceroom.data.models.Location
import com.souvikmondal01.attendanceroom.data.models.User
import com.souvikmondal01.attendanceroom.utils.Response
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun setUserDetailsToFirestore(): Flow<Response<String>>

    fun generateClassJoinCode(code: (String) -> Unit)

    fun isCodeAlreadyExists(code: String, isExist: (Boolean) -> Unit)

    fun createClassRoom(classRoom: ClassRoom, code: String): Flow<Response<String>>

    fun addCodeToFirestoreCodeList(code: String)

    fun getClassRoomFirestoreRecyclerOptions(isArchived: Boolean): Flow<Response<FirestoreRecyclerOptions<ClassRoom>>>

    fun getStudentsFirestoreRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<User>>>

    fun getChatFirestoreRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<Chat>>>

    fun joinClassWithCode(code: String): Flow<Response<String>>

    fun isClassListEmpty(isArchived: Boolean): Flow<Boolean>

    fun archiveClassroom(code: String): Flow<Response<String>>

    fun restoreClassroom(code: String): Flow<Response<String>>

    fun deleteClassroom(code: String): Flow<Response<String>>

    fun unEnrollFromClass(code: String): Flow<Response<String>>

    fun getClassRoomDetails(code: String): Flow<Response<ClassRoom>>

    fun editClassroomDetails(map: Map<String, Any>, code: String): Flow<Response<String>>

    fun setLocationToFirestore(location: Location): Flow<Response<String>>

    fun removeStudentFromClassRoomStudents(code: String): Flow<Response<String>>

    fun isStudentListEmpty(code: String): Flow<Boolean>

    fun removeStudentFromClassRoomByTeacher(code: String, user: User): Flow<Response<String>>

    fun createAttendanceCard(attendance: Attendance, code: String): Flow<Response<String>>

    fun getAttendanceCardTeacherRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<Attendance>>>

    fun isAttendanceCardListEmpty(code: String): Flow<Boolean>

    fun deleteAttendanceCard(code: String, id: String): Flow<Response<String>>

    fun toggleLocationCheck(code: String, id: String): Flow<Response<String>>

    fun toggleSingleDeviceSingleResponse(code: String, id: String): Flow<Response<String>>

    fun doneTakingAttendance(code: String, id: String): Flow<Response<String>>

    fun getLocationCheckStatus(code: String, id: String): Flow<Boolean>

    fun getSingleDeviceSingleResponseStatus(code: String, id: String): Flow<Boolean>

    fun giveAttendanceResponse(
        androidId: String, classCode: String, attendanceId: String
    ): Flow<Response<String>>

    fun getResponseFirestoreRecyclerOptions(
        classCode: String, attendanceId: String
    ): Flow<Response<FirestoreRecyclerOptions<User>>>

    fun getResponseCount(classCode: String, attendanceId: String): Flow<Int>

    fun setFinalResponseList(classCode: String, attendanceId: String): Flow<Response<String>>

    fun getIsResponseAlreadyGivenFromThisDevice(
        activity: Activity, code: String, attendanceId: String
    ): Flow<Boolean>

    fun getStudentList(classCode: String): Flow<Response<List<User>>>

    fun setManualResponseList(classCode: String, attendance: Attendance): Flow<Response<String>>

    fun getAttendanceHistory(classCode: String): Flow<Response<List<Attendance>>>

    fun getFinalResponseList(classCode: String, attendanceId: String): Flow<Response<List<User>>>

    fun setAttendanceHistoryStudent(
        classCode: String,
        attendance: Attendance
    ): Flow<Response<String>>

    fun getStudentAttendanceHistoryRecyclerOptions(classCode: String): Flow<Response<FirestoreRecyclerOptions<Attendance>>>

    fun getStudentAttendanceHistory(classCode: String): Flow<Response<List<Attendance>>>

    fun deleteAttendanceHistory(classCode: String, attendanceId: String): Flow<Response<String>>

    fun setChatToFirestore(classCode: String, chat: Chat): Flow<Response<String>>

    fun deleteChat(classCode: String, chatId: String): Flow<Response<String>>
}

