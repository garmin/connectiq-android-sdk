/**
 * Copyright (C) 2025 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import kotlinx.coroutines.launch

class DeviceListViewModel : ViewModel() {
    private val _devicesList = mutableStateListOf<Device>()
    val devicesList: SnapshotStateList<Device> = _devicesList

    var emptyStateMessage by mutableStateOf("")

    fun fetchDevices(): List<Device> = devicesList

    fun getDevice(deviceId: Long): Device? {
        return devicesList.firstOrNull { it.iqDevice.deviceIdentifier == deviceId }
    }

    fun loadDevices(connectIQ: ConnectIQ) {
        viewModelScope.launch {
            val iqDevices = try {
                // Retrieve the list of known devices.
                connectIQ.knownDevices ?: listOf()
                // OR You can use getConnectedDevices to retrieve the list of connected devices only.
                // val devices = connectIQ.connectedDevices ?: listOf()
                // OR You can use getLinkedCompanionDevices to retrieve the list of linked companion devices only.
                // val devices = connectIQ.linkedCompanionDevices ?: listOf()
            } catch (_: InvalidStateException) {
                // This generally means you forgot to call initialize(), but since
                // we are in the callback for initialize(), this should never happen
                listOf<IQDevice>()
            } catch (_: ServiceUnavailableException) {
                // This will happen if for some reason your app was not able to connect
                // to the ConnectIQ service running within Garmin Connect Mobile.  This
                // could be because Garmin Connect Mobile is not installed or needs to
                // be upgraded.
                emptyStateMessage = "ConnectIQ Service Unavailable"
                listOf<IQDevice>()
            }

            _devicesList.clear()
            _devicesList.addAll(iqDevices.map {
                it.status = connectIQ.getDeviceStatus(it)
                Device(it, connectIQ.getDevicePartNumber(it))
            })

            // Register for device events
            iqDevices.forEach {
                connectIQ.unregisterForDeviceEvents(it)
                connectIQ.registerForDeviceEvents(it) { device, status ->
                    updateDeviceStatus(device, status)
                }
            }
        }
    }

    internal fun updateDeviceStatus(device: IQDevice, newStatus: IQDevice.IQDeviceStatus) {
        val index = devicesList.indexOfFirst { it.iqDevice == device }
        if (index != -1) {
            val previousDevice = devicesList[index]
            _devicesList.remove(previousDevice)
            _devicesList.add(index, previousDevice.copy(iqDevice = IQDevice(previousDevice.iqDevice.deviceIdentifier,
                                                                           previousDevice.iqDevice.friendlyName).apply {
                copy(newStatus)
            }))
        }
    }

    private fun IQDevice.copy(status: IQDevice.IQDeviceStatus) {
        this.status = status
    }
}