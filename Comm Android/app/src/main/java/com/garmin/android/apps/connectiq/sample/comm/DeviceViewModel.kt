/**
 * Copyright (C) 2025 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceViewModel(private val iqDevice: IQDevice, private val iqApp: IQApp): ViewModel() {
    private val _appIsOpen = MutableStateFlow(false)
    val appIsOpen: StateFlow<Boolean> = _appIsOpen
    private val _appIsInstalled = MutableStateFlow(true)
    val appIsInstalled: StateFlow<Boolean> = _appIsInstalled

    fun handleGetAppInfoResponse(device: IQDevice, fromApp: IQApp, status: ConnectIQ.IQOpenApplicationStatus) {
        if (device == iqDevice && fromApp.applicationId == iqApp.applicationId) {
            _appIsOpen.value = status == ConnectIQ.IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING
        }
    }

    fun handleAppInstalledResponse(app: IQApp) {
        if (app.applicationId == iqApp.applicationId && app.status == IQApp.IQAppStatus.INSTALLED) {
            _appIsInstalled.value = true
        }
    }

    fun handleAppNotInstalledResponse(applicationId: String?) {
        if (applicationId == iqApp.applicationId) {
            _appIsInstalled.value = false
        }
    }
}

class DeviceViewModelFactory(private val iqDevice: IQDevice, private val iqApp: IQApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(iqDevice, iqApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}