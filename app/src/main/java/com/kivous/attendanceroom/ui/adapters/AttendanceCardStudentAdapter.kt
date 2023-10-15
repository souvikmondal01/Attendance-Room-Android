package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.ListAttendanceCardStudentBinding
import kotlin.reflect.KFunction2

class AttendanceCardStudentAdapter(
    options: FirestoreRecyclerOptions<Attendance>, private val
    viewController: KFunction2<ViewHolder, Attendance, Unit>
) : FirestoreRecyclerAdapter<Attendance, AttendanceCardStudentAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListAttendanceCardStudentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListAttendanceCardStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Attendance) {
        viewController(holder, model)
    }

}