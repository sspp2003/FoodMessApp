package com.example.tastybites


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StudentDatabase"
        private const val DATABASE_VERSION = 1

        // Table and column names
        private const val TABLE_STUDENTS = "students"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_PHONE_NUMBER = "phone_number"


        // Table and column names for attendance
        private const val TABLE_ATTENDANCE = "attendance"
        private const val COLUMN_ATTENDANCE_ID = "attendance_id"
        private const val COLUMN_STUDENT_ID = "student_id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_BREAKFAST = "breakfast"
        private const val COLUMN_LUNCH = "lunch"
        private const val COLUMN_DINNER = "dinner"

        const val TABLE_FEEDBACK = "feedback"
        private const val COLUMN_FEEDBACK_ID = "feedback_id"
        const val COLUMN_PHONE_NUMBER_FEEDBACK = "phone_number"
        const val COLUMN_FEEDBACK_TEXT = "feedback_text"




        // Create table SQL statement
        private const val CREATE_TABLE_STUDENTS =
            "CREATE TABLE $TABLE_STUDENTS ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_NAME TEXT, $COLUMN_PHONE_NUMBER TEXT)"

        // Create table  SQL statement for attendance
        private const val CREATE_TABLE_ATTENDANCE =
            "CREATE TABLE $TABLE_ATTENDANCE ($COLUMN_ATTENDANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_STUDENT_ID INTEGER, $COLUMN_DATE TEXT, $COLUMN_BREAKFAST INTEGER, " +
                    "$COLUMN_LUNCH INTEGER, $COLUMN_DINNER INTEGER)"

        // Create table SQL statement for feedback
        private const val CREATE_TABLE_FEEDBACK =
            "CREATE TABLE $TABLE_FEEDBACK ($COLUMN_FEEDBACK_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_PHONE_NUMBER_FEEDBACK TEXT, $COLUMN_FEEDBACK_TEXT TEXT)"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("DatabaseHelper2", "onCreate method called")
        db?.execSQL(CREATE_TABLE_STUDENTS)
        db?.execSQL(CREATE_TABLE_ATTENDANCE)
        db?.execSQL(CREATE_TABLE_FEEDBACK)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // You should handle database upgrades between versions here
        if (oldVersion < 2) {
            // Upgrade logic for version 1 to version 2
            // For example, you can add a new column or create a new table

            // Execute SQL statements to modify the database structure
            db?.execSQL("ALTER TABLE $TABLE_ATTENDANCE ADD COLUMN new_column INTEGER DEFAULT 0")

            // If you create a new table, you can do it like this:
            // db?.execSQL("CREATE TABLE new_table (_id INTEGER PRIMARY KEY, name TEXT)")

            // Update the version to reflect the changes
            // This is important to avoid the onUpgrade method being called again
            // when the application is restarted
            db?.execSQL("PRAGMA user_version = 2")
        }

        // Continue adding upgrade logic for other versions as needed
    }


    fun addStudent(student: Student): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME, student.name)
        values.put(COLUMN_PHONE_NUMBER, student.phoneNumber)

        // Inserting Row
        val id = db.insert(TABLE_STUDENTS, null, values)
        db.close()

        return id
    }

    //get all student details
    fun getAllStudents(): List<Student> {
        val students = mutableListOf<Student>()
        val selectQuery = "SELECT * FROM $TABLE_STUDENTS"

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                val phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER))

                students.add(Student(id, name, phoneNumber))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return students
    }

    // Delete a student from the database
    fun deleteStudent(studentId: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_STUDENTS, "$COLUMN_ID = ?", arrayOf(studentId.toString()))
    }
    // Get attendance details for a student on a specific date
    fun getAttendanceForDate(studentId: Int, date: String): List<Attendance> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ATTENDANCE,
            arrayOf(COLUMN_ATTENDANCE_ID, COLUMN_BREAKFAST, COLUMN_LUNCH, COLUMN_DINNER),
            "$COLUMN_STUDENT_ID = ? AND $COLUMN_DATE = ?",
            arrayOf(studentId.toString(), date),
            null,
            null,
            null,
            null
        )

        val attendanceList = mutableListOf<Attendance>()

        while (cursor.moveToNext()) {
            val attendanceId = cursor.getInt(cursor.getColumnIndex(COLUMN_ATTENDANCE_ID))
            val breakfast = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKFAST))
            val lunch = cursor.getInt(cursor.getColumnIndex(COLUMN_LUNCH))
            val dinner = cursor.getInt(cursor.getColumnIndex(COLUMN_DINNER))

            val attendance = Attendance(attendanceId, studentId, date, breakfast, lunch, dinner)
            attendanceList.add(attendance)
        }

        cursor.close()
        db.close()

        return attendanceList
    }


    // Inside DatabaseHelper class
    fun getStudentById(studentId: Int): Student? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_STUDENTS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_PHONE_NUMBER),
            "$COLUMN_ID = ?",
            arrayOf(studentId.toString()),
            null,
            null,
            null,
            null
        )

        var student: Student? = null

        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
            val phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER))

            student = Student(id, name, phoneNumber)
        }

        cursor.close()
        db.close()

        return student
    }


    // Add attendance record
    fun addAttendance(attendance: Attendance): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_STUDENT_ID, attendance.studentId)
        values.put(COLUMN_DATE, attendance.date)
        values.put(COLUMN_BREAKFAST, attendance.breakfast)
        values.put(COLUMN_LUNCH, attendance.lunch)
        values.put(COLUMN_DINNER, attendance.dinner)

        // Inserting Row
        val id = db.insert(TABLE_ATTENDANCE, null, values)
        db.close()

        return id
    }

    // Get attendance details for a student
    fun getAttendanceForStudent(studentId: Int): List<Attendance> {
        val attendanceList = mutableListOf<Attendance>()
        val selectQuery =
            "SELECT * FROM $TABLE_ATTENDANCE WHERE $COLUMN_STUDENT_ID = $studentId"

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val attendanceId = cursor.getInt(cursor.getColumnIndex(COLUMN_ATTENDANCE_ID))
                val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                val breakfast = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKFAST))
                val lunch = cursor.getInt(cursor.getColumnIndex(COLUMN_LUNCH))
                val dinner = cursor.getInt(cursor.getColumnIndex(COLUMN_DINNER))

                val attendance =
                    Attendance(attendanceId, studentId, date, breakfast, lunch, dinner)
                attendanceList.add(attendance)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return attendanceList
    }
    fun getAttendanceForDateAndMeal(studentId: Int, date: String, meal: String): Attendance? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ATTENDANCE,
            arrayOf(COLUMN_ATTENDANCE_ID, COLUMN_BREAKFAST, COLUMN_LUNCH, COLUMN_DINNER),
            "$COLUMN_STUDENT_ID = ? AND $COLUMN_DATE = ?",
            arrayOf(studentId.toString(), date),
            null,
            null,
            null,
            null
        )

        var attendance: Attendance? = null

        if (cursor.moveToFirst()) {
            val attendanceId = cursor.getInt(cursor.getColumnIndex(COLUMN_ATTENDANCE_ID))
            val breakfast = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKFAST))
            val lunch = cursor.getInt(cursor.getColumnIndex(COLUMN_LUNCH))
            val dinner = cursor.getInt(cursor.getColumnIndex(COLUMN_DINNER))

            attendance = Attendance(attendanceId, studentId, date, breakfast, lunch, dinner)
        }

        cursor.close()
        db.close()

        return attendance
    }
    // Update attendance information
    fun updateAttendance(attendance: Attendance): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_BREAKFAST, attendance.breakfast)
        values.put(COLUMN_LUNCH, attendance.lunch)
        values.put(COLUMN_DINNER, attendance.dinner)

        return db.update(
            TABLE_ATTENDANCE,
            values,
            "$COLUMN_ATTENDANCE_ID = ?",
            arrayOf(attendance.attendanceId.toString())
        )
    }

    fun updateAttendanceForMeal(attendance: Attendance, meal: String): Int {
        val db = this.writableDatabase
        val values = ContentValues()

        when (meal) {
            "Breakfast" -> values.put(COLUMN_BREAKFAST, attendance.breakfast)
            "Lunch" -> values.put(COLUMN_LUNCH, attendance.lunch)
            "Dinner" -> values.put(COLUMN_DINNER, attendance.dinner)
        }

        return db.update(
            TABLE_ATTENDANCE,
            values,
            "$COLUMN_ATTENDANCE_ID = ?",
            arrayOf(attendance.attendanceId.toString())
        )
    }

    // SMS sending functions
    fun getAllPhoneNumbers(): List<String> {
        val phoneNumbers = mutableListOf<String>()
        val selectQuery = "SELECT $COLUMN_PHONE_NUMBER FROM $TABLE_STUDENTS"

        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER))
                phoneNumbers.add(phoneNumber)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return phoneNumbers
    }

    fun addFeedback(phoneNumber: String?, feedbackText: String?): Long {
        if (phoneNumber != null && feedbackText != null) {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_PHONE_NUMBER_FEEDBACK, phoneNumber)
                put(COLUMN_FEEDBACK_TEXT, feedbackText)
            }

            val id = db.insert(TABLE_FEEDBACK, null, values)
            db.close()

            return id
        } else {
            Log.e("DatabaseHelper", "Phone number or feedback text is null")
            return -1
        }
    }

    fun deleteAttendanceForStudentAndMeal(studentId: Int, mealType: String): Int {
        val db = this.writableDatabase
        return db.delete(
            TABLE_ATTENDANCE,
            "$COLUMN_STUDENT_ID = ? AND " +
                    when (mealType) {
                        "Breakfast" -> "$COLUMN_BREAKFAST != -1"
                        "Lunch" -> "$COLUMN_LUNCH != -1"
                        "Dinner" -> "$COLUMN_DINNER != -1"
                        else -> ""
                    },
            arrayOf(studentId.toString())
        )
    }

    fun getAllFeedback(): List<String> {
        val feedbackList = mutableListOf<String>()
        val selectQuery = "SELECT * FROM $TABLE_FEEDBACK"

        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_FEEDBACK,
            null,
            null,
            null,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val phoneNumber =
                    cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER_FEEDBACK))
                val feedbackText = cursor.getString(cursor.getColumnIndex(COLUMN_FEEDBACK_TEXT))

                // Combine phone number and feedback text for display
                val feedback = "From: $phoneNumber\nFeedback: $feedbackText"
                feedbackList.add(feedback)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return feedbackList
    }
    fun getAttendanceSummary(studentId: Int, mealType: String): Pair<Int, Int> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ATTENDANCE,
            arrayOf(COLUMN_BREAKFAST, COLUMN_LUNCH, COLUMN_DINNER),
            "$COLUMN_STUDENT_ID = ?",
            arrayOf(studentId.toString()),
            null,
            null,
            null
        )

        var absentCount = 0
        var presentCount = 0

        if (cursor.moveToFirst()) {
            do {
                when (mealType) {
                    "Breakfast" -> {
                        val breakfast = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKFAST))
                        if (breakfast == 0) {
                            absentCount++
                        } else if (breakfast == 1) {
                            presentCount++
                        }
                    }
                    "Lunch" -> {
                        val lunch = cursor.getInt(cursor.getColumnIndex(COLUMN_LUNCH))
                        if (lunch == 0) {
                            absentCount++
                        } else if (lunch == 1) {
                            presentCount++
                        }
                    }
                    "Dinner" -> {
                        val dinner = cursor.getInt(cursor.getColumnIndex(COLUMN_DINNER))
                        if (dinner == 0) {
                            absentCount++
                        } else if (dinner == 1) {
                            presentCount++
                        }
                    }
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return Pair(absentCount, presentCount)
    }


}
