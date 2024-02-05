package com.example.tastybites

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// Import statements

class MainActivity : AppCompatActivity() {


    private lateinit var dbHelper: DatabaseHelper
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var selectedMeal:String
    private lateinit var feedbackButton: Button
    private lateinit var checkFeedback:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feedbackButton = findViewById(R.id.buttonSendFeedback)
        checkFeedback = findViewById(R.id.checkFeedback)

        dbHelper = DatabaseHelper(this)

        //define radioGroup
        val radioGroupMealOptions:RadioGroup = findViewById(R.id.radioGroupMealType)


        feedbackButton.setOnClickListener {
            // Check for SMS permission before sending feedback request
            val i = Intent(this,FeedbackActivity::class.java)
            startActivity(i)
        }


        checkFeedback.setOnClickListener {
            val j = Intent(this@MainActivity,CheckFeedbackActivity::class.java)
            startActivity(j)
        }
        // Setup RecyclerView for student management
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(getStudentsFromDatabase()) { student, isLongClick ->
            if (isLongClick) {
                showDeleteConfirmationDialog(student)
            } else {
                // Short click on the entire item: Open AttendanceActivity
                // Get the latest selected meal at this moment
                selectedMeal = when (radioGroupMealOptions.checkedRadioButtonId) {
                    R.id.radioButtonBreakfast -> "Breakfast"
                    R.id.radioButtonLunch -> "Lunch"
                    R.id.radioButtonDinner -> "Dinner"
                    else -> ""
                }

                val intent = Intent(this, AttendanceActivity::class.java)
                intent.putExtra("STUDENT_ID", student.id)
                intent.putExtra("selectedMeal", selectedMeal)
                startActivity(intent)
            }
        }
        recyclerView.adapter = studentAdapter

        // Setup FloatingActionButton
        val fabAddStudent: FloatingActionButton = findViewById(R.id.floatingActionButton2)
        fabAddStudent.setOnClickListener {
            showAddStudentDialog()
        }
    }



    private fun showAddStudentDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Student")

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL

        val inputName = EditText(this)
        inputName.hint = "Name"
        inputLayout.addView(inputName)

        val inputPhoneNumber = EditText(this)
        inputPhoneNumber.hint = "Phone Number"
        inputLayout.addView(inputPhoneNumber)

        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { _, _ ->
            val name = inputName.text.toString().trim()
            val phoneNumber = inputPhoneNumber.text.toString().trim()

            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                val student = Student(name = name, phoneNumber = phoneNumber)
                val id = dbHelper.addStudent(student)

                if (id != -1L) {
                    // Student added successfully
                    Toast.makeText(this, "Student added", Toast.LENGTH_SHORT).show()

                    // Refresh the RecyclerView
                    studentAdapter.updateData(getStudentsFromDatabase())
                } else {
                    Toast.makeText(this, "Failed to add student", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter both name and phone number", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showDeleteConfirmationDialog(student: Student) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Are you sure you want to delete ${student.name}?")

        builder.setPositiveButton("Yes") { _, _ ->
            // Delete the student from the database
            val rowsAffected = dbHelper.deleteStudent(student.id)

            if (rowsAffected > 0) {
                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show()

                // Refresh the RecyclerView
                studentAdapter.updateData(getStudentsFromDatabase())
            } else {
                Toast.makeText(this, "Failed to delete student", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun getStudentsFromDatabase(): List<Student> {
        return dbHelper.getAllStudents()
    }
}
