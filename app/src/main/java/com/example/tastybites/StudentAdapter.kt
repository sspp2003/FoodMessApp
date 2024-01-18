package com.example.tastybites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// StudentAdapter.kt

class StudentAdapter(
    private var students: List<Student>,
    private val onItemClick: (Student, Boolean) -> Unit
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    // ViewHolder class

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.student_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]

        // Bind data to views
        holder.textViewName.text = student.name
        holder.textViewPhoneNumber.text = student.phoneNumber

        // Set click listener for delete option
        holder.itemView.setOnClickListener {
            // Delegate the item click to the listener in MainActivity
            onItemClick.invoke(student, false)
        }

        // Set click listener for delete option
        holder.imageViewDelete.setOnClickListener {
            // Delegate the delete option click to the listener in MainActivity
            onItemClick.invoke(student, true)
        }
    }

    override fun getItemCount(): Int {
        return students.size
    }

    fun updateData(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewPhoneNumber: TextView = itemView.findViewById(R.id.textViewPhoneNumber)
        val imageViewDelete: ImageView = itemView.findViewById(R.id.imageViewDelete)
    }
}

