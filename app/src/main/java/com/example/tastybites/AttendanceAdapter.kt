package com.example.tastybites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(private val attendanceList: List<Attendance>, private val selectedMeal: String) :
    RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.textViewDate)
        val status: TextView = itemView.findViewById(R.id.textViewStatus)
        val meal: TextView = itemView.findViewById(R.id.textViewMeal)

        fun bind(attendance: Attendance) {
            // Bind data to TextViews
            val statusText = getStatusText(attendance)
            if (statusText.isNotEmpty()) {
                date.text = attendance.date
                status.text = statusText
                meal.text = selectedMeal
            }
        }

        fun getStatusText(attendance: Attendance): String {
            return when (selectedMeal) {
                "Breakfast" -> if (attendance.breakfast == 1) "Present" else if (attendance.breakfast == 0) "Absent" else ""
                "Lunch" -> if (attendance.lunch == 1) "Present" else if (attendance.lunch == 0) "Absent" else ""
                "Dinner" -> if (attendance.dinner == 1) "Present" else if (attendance.dinner == 0) "Absent" else ""
                else -> ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.attendance_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendance = attendanceList.getOrNull(position)

        if (attendance == null || attendanceList.isEmpty()) {
            // Handle the case where the list is empty (no data to bind)
            // Set default values or show a message
            holder.itemView.layoutParams.height = 0
        } else {
            // Bind data to the ViewHolder
            holder.bind(attendance)
            // Set height based on statusText
            val statusText = holder.getStatusText(attendance)
            holder.itemView.layoutParams.height = if (statusText.isNotEmpty()) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        }
    }
}
