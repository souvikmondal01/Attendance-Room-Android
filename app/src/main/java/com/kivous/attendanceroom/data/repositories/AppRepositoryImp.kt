package com.kivous.attendanceroom.data.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.provider.Settings
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.data.models.Chat
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.data.models.Location
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.utils.Common.currentDate
import com.kivous.attendanceroom.utils.Common.currentTime
import com.kivous.attendanceroom.utils.Common.timeStamp
import com.kivous.attendanceroom.utils.Constant
import com.kivous.attendanceroom.utils.Constant.ACTIVE
import com.kivous.attendanceroom.utils.Constant.ALREADY_TEACHER
import com.kivous.attendanceroom.utils.Constant.ANDROID_ID_LIST
import com.kivous.attendanceroom.utils.Constant.ARCHIVED
import com.kivous.attendanceroom.utils.Constant.ATTENDANCE
import com.kivous.attendanceroom.utils.Constant.CAN_JOIN
import com.kivous.attendanceroom.utils.Constant.CHAT_LIST
import com.kivous.attendanceroom.utils.Constant.CLASS_JOIN_DISABLE
import com.kivous.attendanceroom.utils.Constant.CLASS_ROOM_COLLECTION_NAME
import com.kivous.attendanceroom.utils.Constant.CODE
import com.kivous.attendanceroom.utils.Constant.CODE_NOT_EXIST
import com.kivous.attendanceroom.utils.Constant.CREATED_AT
import com.kivous.attendanceroom.utils.Constant.CREATOR_PHOTO_URL
import com.kivous.attendanceroom.utils.Constant.DATE
import com.kivous.attendanceroom.utils.Constant.ERROR_MSG
import com.kivous.attendanceroom.utils.Constant.EXISTING_CODE_COLLECTION_NAME
import com.kivous.attendanceroom.utils.Constant.FAILURE_CODE
import com.kivous.attendanceroom.utils.Constant.FINAL_ATTENDANCE_LIST
import com.kivous.attendanceroom.utils.Constant.LOCATION
import com.kivous.attendanceroom.utils.Constant.LOCATION_CHECK
import com.kivous.attendanceroom.utils.Constant.MANUAL_ATTENDANCE
import com.kivous.attendanceroom.utils.Constant.NAME
import com.kivous.attendanceroom.utils.Constant.NOTES
import com.kivous.attendanceroom.utils.Constant.PARTICIPANT_EMAIL_LIST
import com.kivous.attendanceroom.utils.Constant.RESPONSE
import com.kivous.attendanceroom.utils.Constant.SINGLE_DEVICE_SINGLE_RESPONSE
import com.kivous.attendanceroom.utils.Constant.STUDENTS
import com.kivous.attendanceroom.utils.Constant.STUDENT_EMAIL_LIST
import com.kivous.attendanceroom.utils.Constant.SUCCESS_CODE
import com.kivous.attendanceroom.utils.Constant.TEACHER_EMAIL_LIST
import com.kivous.attendanceroom.utils.Constant.TIME_STAMP
import com.kivous.attendanceroom.utils.Constant.USER_COLLECTION_NAME
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
class AppRepositoryImp @Inject constructor(
    private val auth: FirebaseAuth, private val db: FirebaseFirestore
) : AppRepository {

    override fun setUserDetailsToFirestore(): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val id = auth.currentUser!!.uid
            val name = auth.currentUser!!.displayName
            val email = auth.currentUser!!.email
            val photoUrl = auth.currentUser!!.photoUrl.toString()
            val user = User(
                id = id,
                name = name,
                email = email,
                photoUrl = photoUrl,
                lastLogin = Date().toString()
            )
            db.collection(USER_COLLECTION_NAME).document(auth.currentUser!!.uid).set(user).await()
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun generateClassJoinCode(code: (String) -> Unit) {
        val characters = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val randomCode = (1..6).map { characters.random() }.joinToString("")
        isCodeAlreadyExists(randomCode) {
            if (it) {
                generateClassJoinCode(code)
            } else {
                code.invoke(randomCode)
            }
        }
    }

    override fun isCodeAlreadyExists(code: String, isExist: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = db.collection(EXISTING_CODE_COLLECTION_NAME).document(CODE).get().await()
            val codes = data.data?.get(CODE) as ArrayList<String>
            withContext(Dispatchers.Main) {
                if (code in codes) {
                    isExist.invoke(true)
                } else {
                    isExist.invoke(false)
                }
            }
        }
    }

    override fun createClassRoom(
        classRoom: ClassRoom, code: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            delay(500)
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).set(classRoom).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(FAILURE_CODE))
        }
    }.flowOn(Dispatchers.IO)

    override fun addCodeToFirestoreCodeList(code: String) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val dbRef = db.collection(EXISTING_CODE_COLLECTION_NAME).document(CODE)
                dbRef.update(
                    CODE, FieldValue.arrayUnion(code)
                ).await()
            }

        } catch (e: Exception) {
            logD(e.message ?: ERROR_MSG)
        }
    }

    override fun getClassRoomFirestoreRecyclerOptions(isArchived: Boolean): Flow<Response<FirestoreRecyclerOptions<ClassRoom>>> =
        flow {
            try {
                emit(Response.Loading())
                val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).whereArrayContains(
                    PARTICIPANT_EMAIL_LIST, Firebase.auth.currentUser?.email.toString()
                ).whereEqualTo(ARCHIVED, isArchived).orderBy(TIME_STAMP, Query.Direction.DESCENDING)

                val firestoreRecyclerOptions = FirestoreRecyclerOptions.Builder<ClassRoom>()
                    .setQuery(dbRef, ClassRoom::class.java).build()
                emit(Response.Success(firestoreRecyclerOptions))

            } catch (e: Exception) {
                emit(Response.Error(e.message ?: ERROR_MSG))
            }

        }.flowOn(Dispatchers.IO)

    override fun getStudentsFirestoreRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<User>>> =
        flow {
            try {
                emit(Response.Loading())
                val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(
                    STUDENTS
                ).orderBy(NAME)
                val firestoreRecyclerOptions =
                    FirestoreRecyclerOptions.Builder<User>().setQuery(dbRef, User::class.java)
                        .build()
                emit(Response.Success(firestoreRecyclerOptions))

            } catch (e: Exception) {
                emit(Response.Error(e.message ?: ERROR_MSG))
            }

        }.flowOn(Dispatchers.IO)

    override fun getChatFirestoreRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<Chat>>> =
        flow {
            try {
                emit(Response.Loading())
                val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(
                    CHAT_LIST
                ).orderBy(TIME_STAMP)
                val firestoreRecyclerOptions =
                    FirestoreRecyclerOptions.Builder<Chat>().setQuery(dbRef, Chat::class.java)
                        .build()
                emit(Response.Success(firestoreRecyclerOptions))

            } catch (e: Exception) {
                emit(Response.Error(e.message ?: ERROR_MSG))
            }
        }.flowOn(Dispatchers.IO)

    override fun joinClassWithCode(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            delay(500)
            val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code)
            val codeResult =
                db.collection(EXISTING_CODE_COLLECTION_NAME).document(CODE).get().await()
            val codeList: ArrayList<String> = codeResult.data?.get(CODE) as ArrayList<String>
            val result = dbRef.get().await()
            val canJoin: Boolean = result.data?.get(CAN_JOIN) as? Boolean == true
            val teacherList: ArrayList<String>? =
                result.data?.get(TEACHER_EMAIL_LIST) as? ArrayList<String>
            val studentList: ArrayList<String>? =
                result.data?.get(STUDENT_EMAIL_LIST) as? ArrayList<String>
            val participantEmailList: ArrayList<String>? =
                result.data?.get(PARTICIPANT_EMAIL_LIST) as? ArrayList<String>

            if (codeList.contains(code)) {
                if (teacherList?.contains(auth.currentUser?.email.toString()) == true) {
                    emit(Response.Error(ALREADY_TEACHER))
                } else {
                    if (canJoin) {
                        if (studentList?.contains(auth.currentUser?.email.toString()) == true) {
                            emit(Response.Error(Constant.ALREADY_STUDENT))
                        } else {
                            studentList.let {
                                participantEmailList.let {
                                    dbRef.update(
                                        STUDENT_EMAIL_LIST,
                                        FieldValue.arrayUnion(auth.currentUser?.email.toString())
                                    ).await()
                                    dbRef.update(
                                        PARTICIPANT_EMAIL_LIST,
                                        FieldValue.arrayUnion(auth.currentUser?.email.toString())
                                    ).await()

                                    val user = User(
                                        id = auth.currentUser?.uid,
                                        name = auth.currentUser?.displayName.toString(),
                                        email = auth.currentUser?.email.toString(),
                                        photoUrl = auth.currentUser?.photoUrl.toString(),
                                        lastLogin = Date().toString()
                                    )
                                    dbRef.collection(STUDENTS).document(auth.currentUser!!.uid)
                                        .set(user).await()
                                    emit(Response.Success("class join successful"))
                                }
                            }
                        }

                    } else {
                        emit(Response.Error(CLASS_JOIN_DISABLE))
                    }
                }

            } else {
                emit(Response.Error(CODE_NOT_EXIST))
            }

        } catch (e: Exception) {
            logD(e.message.toString())
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun isClassListEmpty(isArchived: Boolean): Flow<Boolean> = callbackFlow {
        val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).whereArrayContains(
            PARTICIPANT_EMAIL_LIST, auth.currentUser?.email.toString()
        ).whereEqualTo(ARCHIVED, isArchived)
        dbRef.addSnapshotListener { result, e ->
            e?.let {
                this.close(it)
            }
            result?.let {
                val classRoomData = it.toObjects(ClassRoom::class.java)
                this.trySend(classRoomData.isEmpty())
            }
        }
        awaitClose { this.cancel() }
    }.flowOn(Dispatchers.IO)

    override fun archiveClassroom(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).update(ARCHIVED, true).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun restoreClassroom(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).update(ARCHIVED, false).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteClassroom(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).delete().await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun unEnrollFromClass(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).get().await()
            val participantEmailList = result.data?.get(PARTICIPANT_EMAIL_LIST) as ArrayList<String>

            if (participantEmailList.contains(auth.currentUser?.email.toString())) {
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).update(
                    PARTICIPANT_EMAIL_LIST,
                    FieldValue.arrayRemove(auth.currentUser?.email.toString())
                ).await()
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).update(
                    STUDENT_EMAIL_LIST, FieldValue.arrayRemove(auth.currentUser?.email.toString())
                ).await()
                removeStudentFromClassRoomStudents(code)
                emit(Response.Success(SUCCESS_CODE))
            } else {
                emit(Response.Error(FAILURE_CODE))
            }

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getClassRoomDetails(code: String): Flow<Response<ClassRoom>> = callbackFlow {
        try {
            this.trySend(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code)
                .addSnapshotListener { result, e ->
                    e?.let {
                        close(it)
                    }
                    result?.let {
                        val classRoom = result.toObject(ClassRoom::class.java)
                        classRoom?.let {
                            trySend(Response.Success(classRoom))
                        }
                    }
                }

        } catch (e: Exception) {
            trySend(Response.Error(e.message ?: ERROR_MSG))
        }

        awaitClose {
            cancel()
        }
    }.flowOn(Dispatchers.IO)

    override fun editClassroomDetails(
        map: Map<String, Any>, code: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            delay(500)
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).update(map).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun setLocationToFirestore(location: Location): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(USER_COLLECTION_NAME).document(auth.currentUser!!.uid)
                .update(LOCATION, location)
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }

    override fun removeStudentFromClassRoomStudents(code: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(STUDENTS)
                .document(auth.currentUser!!.uid).delete().await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }

    }.flowOn(Dispatchers.IO)

    override fun isStudentListEmpty(code: String): Flow<Boolean> = callbackFlow {
        db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).addSnapshotListener { result, e ->
            e?.let {
                close(e)
            }
            result?.let {
                val studentEmailList = result.get(STUDENT_EMAIL_LIST) as ArrayList<String>
                trySend(studentEmailList.isEmpty())
            }
        }
        awaitClose {
            cancel()
        }
    }.flowOn(Dispatchers.IO)

    override fun removeStudentFromClassRoomByTeacher(
        code: String, user: User
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).get().await()
            val participantsList = result.data?.get(PARTICIPANT_EMAIL_LIST) as ArrayList<String>

            if (participantsList.contains(user.email)) {
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code)
                    .update(STUDENT_EMAIL_LIST, FieldValue.arrayRemove(user.email)).await()
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code)
                    .update(PARTICIPANT_EMAIL_LIST, FieldValue.arrayRemove(user.email)).await()
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(STUDENTS)
                    .document(user.id.toString()).delete().await()
                emit(Response.Success(SUCCESS_CODE))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun createAttendanceCard(
        attendance: Attendance, code: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            delay(500)
            val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).get().await()
            val participantsList = result.data?.get(PARTICIPANT_EMAIL_LIST) as ArrayList<String>
            val dbRef =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            val id = dbRef.document().id
            attendance.id = id
            attendance.creatorName = auth.currentUser!!.displayName.toString()
            attendance.creatorEmail = auth.currentUser!!.email.toString()
            attendance.creatorPhotoUrl = auth.currentUser!!.photoUrl.toString()
            attendance.createdAt = Date().toString()
            attendance.participantEmailList = participantsList
            attendance.active = true
            attendance.locationCheck = true
            attendance.singleDeviceSingleResponse = true
            attendance.timestamp = timeStamp()
            dbRef.document(id).set(attendance).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAttendanceCardTeacherRecyclerOptions(code: String): Flow<Response<FirestoreRecyclerOptions<Attendance>>> =
        flow {
            try {
                emit(Response.Loading())
                val query =
                    db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                        .whereEqualTo(
                            ACTIVE, true
                        ).whereArrayContains(
                            PARTICIPANT_EMAIL_LIST, auth.currentUser!!.email.toString()
                        )
                val recyclerOptions = FirestoreRecyclerOptions.Builder<Attendance>()
                    .setQuery(query, Attendance::class.java).build()
                emit(Response.Success(recyclerOptions))

            } catch (e: Exception) {
                emit(Response.Error(e.message ?: ERROR_MSG))
            }
        }.flowOn(Dispatchers.IO)

    override fun isAttendanceCardListEmpty(code: String): Flow<Boolean> = flow {
        val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            .whereEqualTo(
                ACTIVE, true
            )
        val result = dbRef.get().await()
        val attendanceCard = result.toObjects(Attendance::class.java)
        emit(attendanceCard.isEmpty())
    }.flowOn(Dispatchers.IO)

    fun isTeacherAttendanceCardListEmpty(code: String): Flow<Boolean> = callbackFlow {
        val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            .whereEqualTo(
                ACTIVE, true
            )
        dbRef.addSnapshotListener { result, e ->
            e?.let {
                close(it)
            }
            result?.let {
                val attendanceCard = it.toObjects(Attendance::class.java)
                trySend(attendanceCard.isEmpty())
            }
        }
        awaitClose { cancel() }
    }.flowOn(Dispatchers.IO)

    fun isStudentAttendanceCardListEmpty(code: String): Flow<Boolean> = callbackFlow {
        val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            .whereEqualTo(
                ACTIVE, true
            ).whereArrayContains(
                PARTICIPANT_EMAIL_LIST, auth.currentUser!!.email.toString()
            )
        dbRef.addSnapshotListener { result, e ->
            e?.let {
                close(it)
            }
            result?.let {
                val attendanceCard = it.toObjects(Attendance::class.java)
                trySend(attendanceCard.isEmpty())
            }
        }
        awaitClose { cancel() }
    }.flowOn(Dispatchers.IO)

    override fun deleteAttendanceCard(code: String, id: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                .document(id).delete().await()
            emit((Response.Success(SUCCESS_CODE)))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun toggleLocationCheck(code: String, id: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val result =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                    .document(id).get().await()
            val locationCheckStatus = result.data?.get(LOCATION_CHECK) as Boolean

            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                .document(id).update(LOCATION_CHECK, !locationCheckStatus).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun toggleSingleDeviceSingleResponse(
        code: String, id: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val result =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                    .document(id).get().await()
            val singleDeviceSingleResponse =
                result.data?.get(SINGLE_DEVICE_SINGLE_RESPONSE) as Boolean

            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                .document(id).update(SINGLE_DEVICE_SINGLE_RESPONSE, !singleDeviceSingleResponse)
                .await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun doneTakingAttendance(code: String, id: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                .document(id).update(ACTIVE, false).await()
            emit((Response.Success(SUCCESS_CODE)))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLocationCheckStatus(code: String, id: String): Flow<Boolean> = flow {
        val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            .document(id).get().await()
        val locationCheckStatus = result.data?.get(LOCATION_CHECK) as Boolean
        emit(locationCheckStatus)
    }.flowOn(Dispatchers.IO)

    override fun getSingleDeviceSingleResponseStatus(code: String, id: String): Flow<Boolean> =
        flow {
            val result =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
                    .document(id).get().await()
            val data = result.data?.get(SINGLE_DEVICE_SINGLE_RESPONSE) as Boolean
            emit(data)
        }.flowOn(Dispatchers.IO)

    @SuppressLint("HardwareIds")
    override fun giveAttendanceResponse(
        androidId: String, classCode: String, attendanceId: String
    ): Flow<Response<String>> = flow {
        try {
            val user = User(
                id = auth.currentUser!!.uid,
                name = auth.currentUser!!.displayName.toString(),
                email = auth.currentUser!!.email.toString(),
                photoUrl = auth.currentUser!!.photoUrl.toString(),
                lastLogin = Date().toString()
            )
            emit(Response.Loading())

            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(ATTENDANCE)
                .document(attendanceId).collection(RESPONSE).document(user.id.toString()).set(user)
                .await()
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(ATTENDANCE)
                .document(attendanceId).update(
                    PARTICIPANT_EMAIL_LIST,
                    FieldValue.arrayRemove(auth.currentUser!!.email.toString())
                ).await()
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(ATTENDANCE)
                .document(attendanceId).update(
                    ANDROID_ID_LIST, FieldValue.arrayUnion(androidId)
                ).await()
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }

    }

    override fun getResponseFirestoreRecyclerOptions(
        classCode: String, attendanceId: String
    ): Flow<Response<FirestoreRecyclerOptions<User>>> = flow {
        try {
            emit(Response.Loading())
            val query =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(ATTENDANCE)
                    .document(attendanceId).collection(RESPONSE).orderBy(NAME)

            val recyclerOptions =
                FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()

            emit(Response.Success(recyclerOptions))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getResponseCount(classCode: String, attendanceId: String): Flow<Int> =
        callbackFlow {
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(ATTENDANCE)
                .document(attendanceId).collection(
                    RESPONSE
                ).addSnapshotListener { result, e ->
                    e?.let {
                        close(e)
                    }
                    result?.let {
                        val data = result.toObjects(User::class.java)
                        val responseCount = data.size
                        trySend(responseCount)
                    }
                }

            awaitClose {
                cancel()
            }

        }.flowOn(Dispatchers.IO)

    override fun setFinalResponseList(
        classCode: String, attendanceId: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(
                ATTENDANCE
            ).document(attendanceId)
            val result = dbRef.get().await()
            val id = result.id
            val creatorName = result.data?.get(Constant.CREATOR_NAME).toString()
            val creatorEmail = result.data?.get(Constant.CREATOR_Email).toString()
            val creatorPhotoUrl = result.data?.get(CREATOR_PHOTO_URL).toString()
            val date = result.data?.get(DATE).toString()
            val notes = result.data?.get(NOTES).toString()
            val createdAt = result.data?.get(CREATED_AT).toString()
            val code = result.data?.get(CODE).toString()
            val attendanceType = result.data?.get(Constant.ATTENDANCE_TYPE).toString()
            val timestamp = timeStamp()

            val attendance = Attendance(
                id = id,
                creatorName = creatorName,
                creatorEmail = creatorEmail,
                creatorPhotoUrl = creatorPhotoUrl,
                date = date,
                notes = notes,
                createdAt = createdAt,
                code = code,
                attendanceType = attendanceType,
                timestamp = timestamp
            )
            val responseResult = dbRef.collection(RESPONSE).get().await()
            attendance.finalResponseList =
                responseResult.toObjects(User::class.java) as ArrayList<User>

            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode)
                .collection(FINAL_ATTENDANCE_LIST).document(attendanceId).set(attendance).await()
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("HardwareIds")
    override fun getIsResponseAlreadyGivenFromThisDevice(
        activity: Activity, code: String, attendanceId: String
    ): Flow<Boolean> = flow {
        val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(code).collection(ATTENDANCE)
            .document(attendanceId).get().await()
        val androidId = Settings.Secure.getString(
            activity.contentResolver, Settings.Secure.ANDROID_ID
        )

        val androidIdList = result.data?.get(ANDROID_ID_LIST) as ArrayList<String>

        if (androidIdList.contains(androidId.toString())) {
            emit(true)
        } else {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    override fun getStudentList(classCode: String): Flow<Response<List<User>>> = flow {
        try {
            emit(Response.Loading())
            val result =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(STUDENTS)
                    .orderBy(
                        NAME
                    ).get().await()
            val studentList = result.toObjects(User::class.java) as List<User>
            emit(Response.Success(studentList))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun setManualResponseList(
        classCode: String, attendance: Attendance
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            delay(500)
            val dbRef = db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(
                FINAL_ATTENDANCE_LIST
            )
            val id = dbRef.document().id
            attendance.id = id
            attendance.attendanceType = MANUAL_ATTENDANCE
            attendance.createdAt = Date().toString()
            attendance.creatorName = auth.currentUser!!.displayName
            attendance.creatorEmail = auth.currentUser!!.email
            attendance.creatorPhotoUrl = auth.currentUser!!.photoUrl.toString()
            attendance.timestamp = timeStamp()
            dbRef.document(id).set(attendance).await()
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAttendanceHistory(classCode: String): Flow<Response<List<Attendance>>> = flow {
        try {
            emit(Response.Loading())
            val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(
                FINAL_ATTENDANCE_LIST
            ).orderBy(TIME_STAMP, Query.Direction.DESCENDING).get().await()
            val finalAttendanceList = result.toObjects(Attendance::class.java) as List<Attendance>
            emit(Response.Success(finalAttendanceList))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun getFinalResponseList(
        classCode: String, attendanceId: String
    ): Flow<Response<List<User>>> = flow {
        try {
            emit(Response.Loading())
            val result = db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(
                FINAL_ATTENDANCE_LIST
            ).document(attendanceId).get().await()
            val attendance = result.toObject(Attendance::class.java)
            val finalResponseList = attendance?.finalResponseList as ArrayList<User>
            emit(Response.Success(finalResponseList))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }

    override fun setAttendanceHistoryStudent(
        classCode: String, attendance: Attendance
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(USER_COLLECTION_NAME).document(auth.currentUser!!.uid)
                .collection(classCode).document(attendance.id.toString()).set(attendance).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }

    override fun getStudentAttendanceHistoryRecyclerOptions(classCode: String): Flow<Response<FirestoreRecyclerOptions<Attendance>>> =
        flow {
            try {
                emit(Response.Loading())
                val query =
                    db.collection(USER_COLLECTION_NAME).document(auth.currentUser!!.uid).collection(
                        classCode
                    ).orderBy(TIME_STAMP, Query.Direction.DESCENDING)
                val recyclerOptions = FirestoreRecyclerOptions.Builder<Attendance>()
                    .setQuery(query, Attendance::class.java).build()
                emit(Response.Success(recyclerOptions))

            } catch (e: Exception) {
                emit(Response.Error(e.message ?: ERROR_MSG))
            }
        }.flowOn(Dispatchers.IO)

    override fun getStudentAttendanceHistory(classCode: String): Flow<Response<List<Attendance>>> =
        callbackFlow {
            trySend(Response.Loading())
            db.collection(USER_COLLECTION_NAME).document(auth.currentUser!!.uid)
                .collection(classCode).addSnapshotListener { result, e ->
                    result?.let {
                        val attendanceList =
                            it.toObjects(Attendance::class.java) as List<Attendance>
                        trySend(Response.Success(attendanceList))
                    }

                    e?.let {
                        trySend(Response.Error(e.message ?: ERROR_MSG))
                        close(it)
                    }

                }

            awaitClose {
                cancel()
            }

        }.flowOn(Dispatchers.IO)

    override fun deleteAttendanceHistory(
        classCode: String, attendanceId: String
    ): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(
                FINAL_ATTENDANCE_LIST
            ).document(attendanceId).delete().await()
            emit(Response.Success(SUCCESS_CODE))

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun setChatToFirestore(classCode: String, chat: Chat): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            val dfRef =
                db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(CHAT_LIST)
            val id = dfRef.document().id
            chat.id = id
            chat.userId = auth.currentUser!!.uid
            chat.name = auth.currentUser!!.displayName
            chat.email = auth.currentUser!!.email
            chat.photoUrl = auth.currentUser!!.photoUrl.toString()
            chat.currentDate = currentDate()
            chat.currentTime = currentTime()
            chat.timestamp = timeStamp()
            chat.date = Date()
            dfRef.document(id).set(chat).await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteChat(classCode: String, chatId: String): Flow<Response<String>> = flow {
        try {
            emit(Response.Loading())
            db.collection(CLASS_ROOM_COLLECTION_NAME).document(classCode).collection(CHAT_LIST)
                .document(chatId).delete().await()
            emit(Response.Success(SUCCESS_CODE))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: ERROR_MSG))
        }
    }.flowOn(Dispatchers.IO)


}