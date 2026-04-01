/**
 * Copyright (C) 2025 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm

import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice

data class AppResponseResult(val iqDevice: IQDevice, val iqApp: IQApp, val status: ConnectIQ.IQOpenApplicationStatus)
