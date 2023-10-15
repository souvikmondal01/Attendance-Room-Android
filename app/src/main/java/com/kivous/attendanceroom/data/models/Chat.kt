package com.kivous.attendanceroom.data.models

import java.util.Date

data class Chat(
    var id: String? = null,
    var userId: String? = null,
    var name: String? = null,
    var email: String? = null,
    var photoUrl: String? = null,
    var message: String? = null,
    var currentDate: String? = null,
    var currentTime: String? = null,
    var timestamp: Long? = null,
    var date: Date? = null,
)
