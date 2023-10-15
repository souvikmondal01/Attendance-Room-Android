package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.ListStudentsBinding
import kotlin.reflect.KFunction2

class StudentAdapter(
    options: FirestoreRecyclerOptions<User>,
    private val studentAdapterViewController: KFunction2<ViewHolder, User, Unit>
) : FirestoreRecyclerAdapter<User, StudentAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListStudentsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ListStudentsBinding = ListStudentsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: User) {
        studentAdapterViewController(holder, model)
    }

}