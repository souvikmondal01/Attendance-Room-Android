package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.databinding.ListClassroomBinding

class ClassRoomAdapter(
    options: FirestoreRecyclerOptions<ClassRoom>,
    private val classRoomViewController: (holder: ViewHolder, model: ClassRoom) -> Unit
) : FirestoreRecyclerAdapter<ClassRoom, ClassRoomAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListClassroomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListClassroomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: ClassRoom) {
        classRoomViewController(holder, model)
    }

}