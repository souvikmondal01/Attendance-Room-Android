package com.souvikmondal01.attendanceroom.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.data.models.Chat

class ChatAdapter(
    val auth: FirebaseAuth,
    val context: Context,
    options: FirestoreRecyclerOptions<Chat>,
    private val chatAdapterViewController: (ViewHolder, Chat) -> Unit
) : FirestoreRecyclerAdapter<Chat, ChatAdapter.ViewHolder>(options) {


    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvCurrentDate: TextView = view.findViewById(R.id.tv_current_date)
        val tvCurrentTime: TextView = view.findViewById(R.id.tv_current_time)
        val ivProfile: ImageView = view.findViewById(R.id.iv_profile)
    }

    private val RIGHT = 0
    private val LEFT = 1

    override fun getItemViewType(position: Int): Int {
        val model = getItem(position)
        return if (model.userId == auth.currentUser!!.uid) {
            LEFT
        } else {
            RIGHT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == LEFT) {
            ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.list_chat_right, parent, false)
            )
        } else {
            ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.list_chat_left, parent, false)
            )
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Chat) {
        chatAdapterViewController(holder, model)
    }


}