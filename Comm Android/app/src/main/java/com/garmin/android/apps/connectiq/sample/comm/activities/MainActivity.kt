/**
 * Copyright (C) 2015 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garmin.android.apps.connectiq.sample.comm.Device
import com.garmin.android.apps.connectiq.sample.comm.R
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.apps.connectiq.sample.comm.DeviceListViewModel
import com.garmin.android.apps.connectiq.sample.comm.DeviceMessageViewModel
import com.garmin.android.apps.connectiq.sample.comm.SnackbarViewModel

class MainActivity : ComponentActivity() {

    private lateinit var connectIQ: ConnectIQ
    private val deviceListViewModel: DeviceListViewModel by viewModels()
    private val deviceMessageViewModel: DeviceMessageViewModel by viewModels()
    private var isSdkReady by mutableStateOf(false)

    private val connectIQListener: ConnectIQ.ConnectIQListener =
        object : ConnectIQ.ConnectIQListener {
            override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
                deviceListViewModel.emptyStateMessage = getString(R.string.initialization_error) + ": " + errStatus.name
                isSdkReady = false
            }

            override fun onSdkReady() {
                loadDevices()
                isSdkReady = true
            }

            override fun onSdkShutDown() {
                isSdkReady = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val snackbarViewModel = viewModel<SnackbarViewModel>()

            MainScreen(
                onDeviceClick = { onItemClick(it.iqDevice) },
                snackbarHostState = snackbarViewModel.snackbarHostState
            )
            setupConnectIQSdk()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSdkReady) {
            loadDevices()
        }
    }

    override fun onPause() {
        super.onPause()
        deviceListViewModel.fetchDevices().forEach {
            connectIQ.unregisterForApplicationEvents(it.iqDevice, IQApp(COMM_WATCH_ID))
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseConnectIQSdk()
    }

    private fun releaseConnectIQSdk() {
        try {
            // It is a good idea to unregister everything and shut things down to
            // release resources and prevent unwanted callbacks.
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(this)
        } catch (_: InvalidStateException) {
            // This is usually because the SDK was already shut down
            // so no worries.
        }
    }

    private fun onItemClick(device: IQDevice) {
        startActivity(DeviceActivity.getIntent(this, device/*, usingBindingService*/))
    }

    private fun setupConnectIQSdk() {
        // Here we are specifying that we want to use a WIRELESS bluetooth connection.
        // We could have just called getInstance() which would by default create a version
        // for WIRELESS, unless we had previously gotten an instance passing TETHERED
        // as the connection type.
        connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)

        // Initialize the SDK
        connectIQ.initialize(this, true, connectIQListener)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.load_devices -> {
                loadDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadDevices() {
        deviceListViewModel.loadDevices(connectIQ)
        deviceListViewModel.fetchDevices().forEach {
            listenByMyAppEvents(it.iqDevice)
        }
    }

    // Let's register to receive messages from our application on the device.
    private fun listenByMyAppEvents(device: IQDevice) {
        try {
            connectIQ.registerForAppEvents(device, IQApp(COMM_WATCH_ID)) { commDevice, _, message, _ ->
                val alertTitle = getTitleMessage(commDevice.deviceIdentifier)
                deviceMessageViewModel.setAlertMessage(message, alertTitle)
            }
        } catch (_: InvalidStateException) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTitleMessage(deviceId: Long): String {
        val storedDevice = deviceListViewModel.getDevice(deviceId)
        return if (storedDevice != null) {
            getString(R.string.received_message) + " " + storedDevice.iqDevice.friendlyName
        } else {
            getString(R.string.received_message)
        }
    }
}

@Composable
fun MainScreen(
    onDeviceClick: (Device) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val deviceListViewModel = viewModel<DeviceListViewModel>()
    val deviceMessageViewModel = viewModel<DeviceMessageViewModel>()
    val showAlertDialog by deviceMessageViewModel.showAlertDialog.collectAsState()
    val alertDialogMessage by deviceMessageViewModel.alertMessage.collectAsState()
    val devices = deviceListViewModel.devicesList
    val emptyStateMessage = deviceListViewModel.emptyStateMessage
    val okString = LocalContext.current.getString(android.R.string.ok)

    LaunchedEffect(showAlertDialog) {
        if (showAlertDialog) {
            snackbarHostState.showSnackbar(message = alertDialogMessage,
                                           actionLabel = okString)
            deviceMessageViewModel.resetAlertDialog()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Main Activity") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (devices.isEmpty()) {
                Text(
                    text = emptyStateMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn {
                    items(devices) { device ->
                        DeviceItem(device = device, onClick = { onDeviceClick(device) })
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: Device, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = device.iqDevice.friendlyName, style = MaterialTheme.typography.h6)
            Text(text = device.iqDevice.status?.name ?: "Unknown")
        }
    }
}