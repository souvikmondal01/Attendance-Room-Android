package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.databinding.ListClassroomBinding
import kotlin.reflect.KFunction2

class ClassRoomAdapter(
    options: FirestoreRecyclerOptions<ClassRoom>,
    private val classRoomViewController: KFunction2<ViewHolder, ClassRoom, Unit>
) : FirestoreRecyclerAdapter<ClassRoom, ClassRoomAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListClassroomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ListClassroomBinding = ListClassroomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: ClassRoom) {
        classRoomViewController(holder, model)
    }

}