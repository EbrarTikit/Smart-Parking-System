package com.example.smartparkingsystem.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.Message

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object{
        private const val VIEW_TYPE_USER_MESSAGE = 1
        private const val VIEW_TYPE_BOT_MESSAGE = 2
    }

    private val messages = mutableListOf<Message>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_bot, parent, false)
                BotMessageViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
        }
    }

    fun addMessage(message: Message){
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if(messages[position].isFromUser) {
            VIEW_TYPE_USER_MESSAGE
        } else {
            VIEW_TYPE_BOT_MESSAGE
        }
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)

        fun bind(message: Message) {
            tvUserMessage.text = message.text
        }
    }

    inner class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBotMessage: TextView = itemView.findViewById(R.id.tvBotMessage)

        fun bind(message: Message) {
            tvBotMessage.text = message.text
        }
    }

}