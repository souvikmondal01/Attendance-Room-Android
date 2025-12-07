package com.souvikmondal01.attendanceroom.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.souvikmondal01.attendanceroom.data.models.User
import com.souvikmondal01.attendanceroom.databinding.ListResponseBinding

class FinalResponseAdapter(
    val viewController: (ViewHolder, User) -> Unit
) : ListAdapter<User, FinalResponseAdapter.ViewHolder>(DiffUtil()) {
    class ViewHolder(val binding: ListResponseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListResponseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userData = getItem(position)
        viewController(holder, userData)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}