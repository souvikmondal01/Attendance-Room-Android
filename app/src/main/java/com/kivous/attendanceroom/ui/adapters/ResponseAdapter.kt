package com.kivous.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.ListResponseBinding

class ResponseAdapter(
    options: FirestoreRecyclerOptions<User>, private val viewController: (ViewHolder, User) -> Unit
) : FirestoreRecyclerAdapter<User, ResponseAdapter.ViewHolder>(options) {
    class ViewHolder(val binding: ListResponseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListResponseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: User) {
        viewController(holder, model)
    }
}