package com.souvikmondal01.attendanceroom.data.models

data class Attendance(
    var id: String? = null,
    var creatorName: String? = null,
    var creatorEmail: String? = null,
    var creatorPhotoUrl: String? = null,
    var createdAt: String? = null,
    var date: String? = null,
    var code: String? = null,
    var notes: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radius: String? = null,
    var active: Boolean? = null,
    var locationCheck: Boolean? = null,
    var participantEmailList: ArrayList<String>? = arrayListOf(),
    val androidIdList: ArrayList<String>? = arrayListOf(),
    var attendanceType: String? = null,
    var finalResponseList: ArrayList<User>? = arrayListOf(),
    var singleDeviceSingleResponse: Boolean? = null,
    var timestamp: Long? = null,
)