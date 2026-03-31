/**
 * Copyright (C) 2025 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm

import androidx.compose.material.SnackbarHostState
import androidx.lifecycle.ViewModel

class SnackbarViewModel : ViewModel() {
    val snackbarHostState = SnackbarHostState()
}