package com.example.tastybites

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Locale

class AttendanceActivity : AppCompatActivity() {
    private var studentId: Int = -1
    private lateinit var selectedMeal: String
    private lateinit var student: Student
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerViewAttendance: RecyclerView
    private lateinit var buttonMarkPresent: Button
    private lateinit var buttonMarkAbsent: Button
    private lateinit var dbHelper: DatabaseHelper
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
        dbHelper = DatabaseHelper(this)

        studentId = intent.getIntExtra("STUDENT_ID", -1)
        selectedMeal = intent.getStringExtra("selectedMeal") ?: ""

        recyclerViewAttendance = findViewById(R.id.recyclerViewAttendance)

        val attendanceAdapter = AttendanceAdapter(getAttendanceListForMeal(studentId,selectedMeal), selectedMeal)
        recyclerViewAttendance.layoutManager = LinearLayoutManager(this)
        recyclerViewAttendance.adapter = attendanceAdapter

        // Update the RecyclerView initially
//        updateRecyclerView(studentId, selectedMeal, selectedDate,true)
        val calendar = Calendar.getInstance()
        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        // Define views
        calendarView = findViewById(R.id.calendarView)
        buttonMarkPresent = findViewById(R.id.buttonMarkPresent)
        buttonMarkAbsent = findViewById(R.id.buttonMarkAbsent)

        if (studentId != -1) {
            student = dbHelper.getStudentById(studentId)!!
        }

        buttonMarkPresent.setOnClickListener {
            markAttendance(student, selectedMeal, "Present")
            Toast.makeText(this,"attendance marked present on date $selectedDate",Toast.LENGTH_SHORT).show()
        }

        buttonMarkAbsent.setOnClickListener {
            markAttendance(student, selectedMeal, "Absent")
            Toast.makeText(this,"attendance marked absent on date $selectedDate",Toast.LENGTH_SHORT).show()
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            Log.d("AttendanceActivity", "Selected Date: $selectedDate")
        }
    }

    private fun markAttendance(student: Student, meal: String, status: String) {
        val selectedDate = getSelectedDateFromCalendar()
        val existingAttendanceList = dbHelper.getAttendanceForDate(student.id, selectedDate)

        val attendance = findAttendanceByMeal(existingAttendanceList, meal)

        if (attendance != null) {
            // Update existing attendance record for the selected meal
            updateAttendanceRecord(attendance, status)
        } else {
            // Create a new attendance record for the selected meal
            createNewAttendanceRecord(student, meal, status)
        }

        // Update the RecyclerView after marking attendance
        updateRecyclerView(student.id)
    }

    private fun findAttendanceByMeal(attendanceList: List<Attendance>, meal: String): Attendance? {
        for (attendance in attendanceList) {
            when (meal) {
                "Breakfast" -> if (attendance.breakfast == 1) return attendance
                "Lunch" -> if (attendance.lunch == 1) return attendance
                "Dinner" -> if (attendance.dinner == 1) return attendance
            }
        }
        return null
    }

    private fun updateAttendanceRecord(attendance: Attendance, status: String) {
        // Update the selected meal status
        when (selectedMeal) {
            "Breakfast" -> attendance.breakfast = if (status == "Present") 1 else 0
            "Lunch" -> attendance.lunch = if (status == "Present") 1 else 0
            "Dinner" -> attendance.dinner = if (status == "Present") 1 else 0
        }

        // Update the attendance record in the database
        dbHelper.updateAttendance(attendance)
//        updateRecyclerView(student.id, selectedMeal, selectedDate, update = true)
    }

    private fun createNewAttendanceRecord(student: Student, meal: String, status: String) {
        // Create a new attendance record for the selected meal
        val newAttendance:Attendance
        if(selectedMeal=="Breakfast"){
            newAttendance = Attendance(
                attendanceId = 0,
                studentId = student.id,
                date = getSelectedDateFromCalendar(),
                breakfast = if (status == "Present") 1 else 0
            )
        }
        else if(selectedMeal=="Lunch"){
            newAttendance = Attendance(
                attendanceId = 0,
                studentId = student.id,
                date = getSelectedDateFromCalendar(),
                lunch = if (status == "Present") 1 else 0
            )
        }
        else{
            newAttendance = Attendance(
                attendanceId = 0,
                studentId = student.id,
                date = getSelectedDateFromCalendar(),
                dinner = if (status == "Present") 1 else 0
            )
        }

        // Add the new attendance record to the database
        dbHelper.addAttendance(newAttendance)
//        updateRecyclerView(student.id, selectedMeal, selectedDate, update = true)
    }

    private fun getSelectedDateFromCalendar(): String {
        return selectedDate
    }

    private fun updateRecyclerView(studentId: Int) {
        Log.d("AttendanceActivity", "updateRecyclerView called")
        val attendanceAdapter = AttendanceAdapter(getAttendanceListForMeal(studentId,selectedMeal), selectedMeal)
        recyclerViewAttendance.layoutManager = LinearLayoutManager(this)
        recyclerViewAttendance.adapter = attendanceAdapter
    }

    // Function to determine the meal type for an attendance record
    private fun getMealType(attendance: Attendance, selectedMeal: String): Attendance {
        return when (selectedMeal) {
            "Breakfast" -> Attendance(
                attendanceId = attendance.attendanceId,
                studentId = attendance.studentId,
                date = attendance.date,
                breakfast = attendance.breakfast,
                lunch = -1,  // Set lunch to -1 for non-selected meal
                dinner = -1   // Set dinner to -1 for non-selected meal
            )
            "Lunch" -> Attendance(
                attendanceId = attendance.attendanceId,
                studentId = attendance.studentId,
                date = attendance.date,
                breakfast = -1,  // Set breakfast to -1 for non-selected meal
                lunch = attendance.lunch,
                dinner = -1   // Set dinner to -1 for non-selected meal
            )
            "Dinner" -> Attendance(
                attendanceId = attendance.attendanceId,
                studentId = attendance.studentId,
                date = attendance.date,
                breakfast = -1,  // Set breakfast to -1 for non-selected meal
                lunch = -1,  // Set lunch to -1 for non-selected meal
                dinner = attendance.dinner
            )
            else -> attendance  // Return original attendance for unknown meal
        }
    }


    private fun getInitialAttendanceData(studentId: Int): List<Attendance> {
        // Call a function to retrieve attendance records from the database
        // take a meal as input and retrieve the list of all existing attendance of the student
        // and if u mark attendance it should update the meal
        // add the new attendance to the list and use notifydatasetchanged
        // use is marked bool to adapter
        return dbHelper.getAttendanceForStudent(studentId).reversed()
    }

    private fun getAttendanceListForMeal(studentId: Int, selectedMeal: String): List<Attendance> {
        val attendanceList = dbHelper.getAttendanceForStudent(studentId)
        return attendanceList
            .map { getMealType(it, selectedMeal) }  // Map each item using getMealType
            .filter { it.breakfast != -1 || it.lunch != -1 || it.dinner != -1 }  // Filter out items with all meals set to -1
            .reversed()  // Reverse the list if needed
    }


}
