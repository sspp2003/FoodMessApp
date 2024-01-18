package com.example.tastybites

data class Attendance(
    val attendanceId: Int,
    val studentId: Int,
    val date: String,
    var breakfast: Int=-1,
    var lunch: Int=-1,
    var dinner: Int=-1
)

