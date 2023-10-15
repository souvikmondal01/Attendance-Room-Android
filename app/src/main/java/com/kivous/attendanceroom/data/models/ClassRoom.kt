package com.kivous.attendanceroom.data.models

import java.util.Date

data class ClassRoom(
    val className: String? = null,
    val department: String? = null,
    val batch: String? = null,
    val subject: String? = null,
    var creatorName: String? = null,
    var creatorEmail: String? = null,
    var creatorPhotoUrl: String? = null,
    val code: String? = null,
    var teacherEmailList: ArrayList<String>? = null,
    var studentEmailList: ArrayList<String>? = null,
    var participantEmailList: ArrayList<String>? = null,
    var date: Date? = null,
    val archived: Boolean? = false,
    val canJoin: Boolean? = true,
    val latitude: String? = "",
    val longitude: String? = "",
    val radius: String? = "",
    var timestamp: Long? = null
)
