package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.ListAttendanceHistoryTeacherBinding

class AttendanceHistoryAdapter(
    private val viewController: (ViewHolder, Attendance) -> Unit
) : ListAdapter<Attendance, AttendanceHistoryAdapter.ViewHolder>(DiffUtil()) {
    class ViewHolder(val binding: ListAttendanceHistoryTeacherBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListAttendanceHistoryTeacherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendanceData = getItem(position)
        viewController(holder, attendanceData)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<Attendance>() {
        override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
            return oldItem == newItem
        }
    }
}