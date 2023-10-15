package com.kivous.attendanceroom.data.models

data class User(
    var id: String? = null,
    var name: String? = null,
    var email: String? = null,
    var photoUrl: String? = null,
    var lastLogin: String? = null,
    var location: Location? = null,
)
