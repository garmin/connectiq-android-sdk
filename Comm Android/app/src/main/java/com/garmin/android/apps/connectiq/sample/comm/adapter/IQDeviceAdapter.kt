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
import com.garmin.android.apps.connectiq.sample.comm.Device
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus

class IQDeviceAdapter(
    private val onItemClickListener: (Device) -> Unit
) : ListAdapter<Device, IQDeviceViewHolder>(IQDeviceItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IQDeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return IQDeviceViewHolder(view, onItemClickListener)
    }

    override fun onBindViewHolder(holder: IQDeviceViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    fun updateDeviceStatus(device: Device, status: IQDeviceStatus?) {
        val index = currentList
            .indexOfFirst { it.iqDevice.deviceIdentifier == device.iqDevice.deviceIdentifier }
        currentList[index].iqDevice.status = status
        notifyItemChanged(index)
    }
}

private class IQDeviceItemDiffCallback : DiffUtil.ItemCallback<Device>() {
    override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean =
        oldItem.iqDevice.deviceIdentifier == newItem.iqDevice.deviceIdentifier

    override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean =
        oldItem.iqDevice.deviceIdentifier == newItem.iqDevice.deviceIdentifier
            && oldItem.iqDevice.friendlyName == newItem.iqDevice.friendlyName
            && oldItem.iqDevice.status == newItem.iqDevice.status

}

class IQDeviceViewHolder(
    private val view: View,
    private val onItemClickListener: (Device) -> Unit
) : RecyclerView.ViewHolder(view) {

    fun bindTo(device: Device) {
        var deviceName = when (device.iqDevice.friendlyName) {
            null -> device.iqDevice.deviceIdentifier.toString()
            else -> device.iqDevice.friendlyName
        }

        if (!device.partNumber.isNullOrBlank()) {
            deviceName += " (${device.partNumber})"
        }

        view.findViewById<TextView>(android.R.id.text1).text = deviceName
        view.findViewById<TextView>(android.R.id.text2).text = device.iqDevice.status?.name

        view.setOnClickListener {
            onItemClickListener(device)
        }
    }
}