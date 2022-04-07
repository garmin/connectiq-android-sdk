/**
 * Copyright (C) 2015 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.garmin.android.apps.connectiq.sample.comm.Message

class MessagesAdapter(
    private val onItemClickListener: (Any) -> Unit
) : ListAdapter<Message, MessageViewHolder>(MessageItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MessageViewHolder(view, onItemClickListener)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }
}

private class MessageItemDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem == newItem

}

class MessageViewHolder(
    private val view: View,
    private val onItemClickListener: (Any) -> Unit
) : RecyclerView.ViewHolder(view) {

    fun bindTo(message: Message) {
        view.findViewById<TextView>(android.R.id.text1).text = message.text
        view.setOnClickListener {
            onItemClickListener(message.payload)
        }
    }
}