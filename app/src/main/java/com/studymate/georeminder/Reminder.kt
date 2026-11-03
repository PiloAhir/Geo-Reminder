package com.studymate.georeminder

data class Reminder(
    val id: String,
    val title: String,
    val message: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val active: Boolean = true
)
