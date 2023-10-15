package com.kivous.attendanceroom.data.models

data class Location(
    val area: String? = null,
    val locality: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val pin: String? = null,
    val fullAddress: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
)