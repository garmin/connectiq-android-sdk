/**
 * Copyright (C) 2025 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceMessageViewModel : ViewModel() {
    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage
    private val _showAlertDialog = MutableStateFlow(false)
    val showAlertDialog: StateFlow<Boolean> = _showAlertDialog

    fun resetAlertDialog() {
        _showAlertDialog.value = false
    }

    fun setAlertMessage(messageData: List<Any>,
                        alertTitle: String) {
        // We know from our Comm sample widget that it will only ever send us strings, but in case
        // we get something else, we are simply going to do a toString() on each object in the
        // message list.
        val builder = StringBuilder()
        if (messageData.isNotEmpty()) {
            for (o in messageData) {
                if (o == null) {
                    builder.append("0")
                } else {
                    builder.append(o.toString())
                }
                builder.append("\r\n")
            }
        } else {
            builder.append("Received an empty message from the application")
        }
        _alertMessage.value = "$alertTitle $builder"
        _showAlertDialog.value = true
    }

    fun setAlertMessage(message: String) {
        _alertMessage.value = message
        _showAlertDialog.value = true
    }
}