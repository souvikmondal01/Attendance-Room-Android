package com.souvikmondal01.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.databinding.ListAttendanceHistoryStudentBinding

class StudentAttendanceHistoryAdapter(
    options: FirestoreRecyclerOptions<Attendance>,
    private val viewController: (ViewHolder, Attendance) -> Unit
) : FirestoreRecyclerAdapter<Attendance, StudentAttendanceHistoryAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListAttendanceHistoryStudentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListAttendanceHistoryStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Attendance) {
        viewController(holder, model)
    }

}