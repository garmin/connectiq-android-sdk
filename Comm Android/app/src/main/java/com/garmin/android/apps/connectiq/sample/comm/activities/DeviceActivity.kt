/**
 * Copyright (C) 2015 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.garmin.android.apps.connectiq.sample.comm.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garmin.android.apps.connectiq.sample.comm.DeviceMessageViewModel
import com.garmin.android.apps.connectiq.sample.comm.DeviceViewModel
import com.garmin.android.apps.connectiq.sample.comm.DeviceViewModelFactory
import com.garmin.android.apps.connectiq.sample.comm.Message
import com.garmin.android.apps.connectiq.sample.comm.MessageFactory
import com.garmin.android.apps.connectiq.sample.comm.R
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import com.garmin.android.apps.connectiq.sample.comm.SnackbarViewModel

// TODO Add a valid store app id.
private const val STORE_APP_ID = ""
const val COMM_WATCH_ID = "a3421feed289106a538cb9547ab12095"

class DeviceActivity : ComponentActivity() {

    companion object {

        const val TAG = "DeviceActivity"
        private const val EXTRA_IQ_DEVICE = "IQDevice"

        fun getIntent(context: Context, device: IQDevice?): Intent {
            val intent = Intent(context, DeviceActivity::class.java)
            intent.putExtra(EXTRA_IQ_DEVICE, device)
            return intent
        }
    }

    private val connectIQ: ConnectIQ = ConnectIQ.getInstance()
    private lateinit var device: IQDevice
    private lateinit var myApp: IQApp
    private var startSendMessage = 0L

    private var deviceStatus by mutableStateOf(IQDevice.IQDeviceStatus.UNKNOWN)
    private val deviceMessageViewModel: DeviceMessageViewModel by viewModels()
    private val deviceViewModel: DeviceViewModel by viewModels {
        DeviceViewModelFactory(device, IQApp(COMM_WATCH_ID))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        device = intent.getParcelableExtra<Parcelable>(EXTRA_IQ_DEVICE) as IQDevice
        myApp = IQApp(COMM_WATCH_ID)
        deviceStatus = device.status

        setContent {
            val snackbarViewModel = viewModel<SnackbarViewModel>()

            DeviceScreen(
                deviceName = device.friendlyName,
                deviceStatus = deviceStatus,
                deviceViewModel = deviceViewModel,
                onOpenAppClick = { openMyApp() },
                onMessageClick = { onMessageClick(it) },
                snackbarHostState = snackbarViewModel.snackbarHostState
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        listenByDeviceEvents()
        listenByMyAppEvents()
        getMyAppStatus()
    }

    public override fun onPause() {
        super.onPause()

        // It is a good idea to unregister everything and shut things down to
        // release resources and prevent unwanted callbacks.
        try {
            connectIQ.unregisterForDeviceEvents(device)
            connectIQ.unregisterForApplicationEvents(device, myApp)
        } catch (_: InvalidStateException) {
        }
    }

    private fun openMyApp() {
        Toast.makeText(this, "Opening app...", Toast.LENGTH_SHORT).show()

        // Send a message to open the app
        try {
            connectIQ.openApplication(device, myApp) { _, _, status ->
                runOnUiThread {
                    // This runs on the UI thread so we can update our UI safely.
                    when (status) {
                        ConnectIQ.IQOpenApplicationStatus.PROMPT_SHOWN_ON_DEVICE -> {
                            Toast.makeText(this@DeviceActivity, "App opened successfully", Toast.LENGTH_SHORT).show()
                        }
                        ConnectIQ.IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING -> {
                            Toast.makeText(this@DeviceActivity, "App is already running ", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this@DeviceActivity, "Failed to open app: ${status.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                deviceViewModel.handleGetAppInfoResponse(device, myApp, status)
            }
        } catch (_: Exception) {
        }
    }

    private fun openStore() {
        Toast.makeText(this, "Opening ConnectIQ Store...", Toast.LENGTH_SHORT).show()

        // Send a message to open the store
        try {
            if (STORE_APP_ID.isBlank()) {
                deviceMessageViewModel.setAlertMessage("${getString(R.string.missing_store_id)}\n${getString(R.string.missing_store_id_message)}")
            } else {
                connectIQ.openStore(STORE_APP_ID)
            }
        } catch (_: Exception) {
        }
    }

    private fun listenByDeviceEvents() {
        // Get our instance of ConnectIQ. Since we initialized it
        // in our MainActivity, there is no need to do so here, we
        // can just get a reference to the one and only instance.
        try {
            connectIQ.registerForDeviceEvents(device) { _, status ->
                deviceStatus = status
            }
        } catch (_: InvalidStateException) {
            Log.wtf(TAG, "InvalidStateException:  We should not be here!")
        }
    }

    // Let's check the status of our application on the device.
    private fun getMyAppStatus() {
        try {
            connectIQ.getApplicationInfo(COMM_WATCH_ID, device, object :
                ConnectIQ.IQApplicationInfoListener {
                override fun onApplicationInfoReceived(app: IQApp) {
                    // This is a good thing. Now we can show our list of message options.
                    deviceViewModel.handleAppInstalledResponse(app)
                }

                override fun onApplicationNotInstalled(applicationId: String) {
                    // The Comm widget is not installed on the device so we have
                    // to let the user know to install it.
                    deviceViewModel.handleAppNotInstalledResponse(applicationId)
                }
            })
        } catch (_: InvalidStateException) {
        } catch (_: ServiceUnavailableException) {
        }
    }

    private fun onMessageClick(message: Message) {
        try {
            connectIQ.sendMessage(device, myApp, message.payload) { _, _, status ->
                val messageAckReceived = System.currentTimeMillis()
                runOnUiThread {
                    val toastText = status.name + " sent in " + (messageAckReceived - startSendMessage) + " ms"
                    Toast.makeText(this@DeviceActivity, toastText, Toast.LENGTH_SHORT).show()
                }
            }
            startSendMessage = System.currentTimeMillis()
        } catch (_: InvalidStateException) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        } catch (_: ServiceUnavailableException) {
            Toast.makeText(
                this,
                "ConnectIQ service is unavailable. Is Garmin Connect Mobile installed and running?",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Let's register to receive messages from our application on the device.
    private fun listenByMyAppEvents() {
        try {
            connectIQ.registerForAppEvents(device, IQApp(COMM_WATCH_ID)) { _, _, message, _ ->
                val alertTitle = getString(R.string.received_message) + device.friendlyName
                deviceMessageViewModel.setAlertMessage(message, alertTitle)
            }
        } catch (_: InvalidStateException) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun DeviceScreen(
    deviceName: String,
    deviceStatus: IQDevice.IQDeviceStatus,
    deviceViewModel: DeviceViewModel,
    onOpenAppClick: () -> Unit,
    onMessageClick: (Message) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val deviceMessageViewModel = viewModel<DeviceMessageViewModel>()
    val showAlertDialog by deviceMessageViewModel.showAlertDialog.collectAsState()
    val alertDialogMessage by deviceMessageViewModel.alertMessage.collectAsState()
    val appIsOpen by deviceViewModel.appIsOpen.collectAsState()
    val appIsInstalled by deviceViewModel.appIsInstalled.collectAsState()
    var messages: List<Message> = listOf()
    val okString = LocalContext.current.getString(android.R.string.ok)

    LaunchedEffect(showAlertDialog) {
        if (showAlertDialog) {
            snackbarHostState.showSnackbar(message = alertDialogMessage,
                                           actionLabel = okString)
            deviceMessageViewModel.resetAlertDialog()
        }
    }

    if (appIsInstalled) {
        messages.let {
            messages = MessageFactory.getMessages(LocalContext.current)
        }
    }
    val appNotInstalledAlert = LocalContext.current.getString(R.string.missing_widget) + "\n" +
                    LocalContext.current.getString(R.string.missing_widget_message)
    LaunchedEffect(appIsInstalled) {
        if(!appIsInstalled) {
            messages = listOf()
            // Show alert dialog that app is not installed
            snackbarHostState.showSnackbar(appNotInstalledAlert, okString)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Device Activity") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(text = "Device: $deviceName", style = MaterialTheme.typography.h6)
            Text(text = "Status: $deviceStatus", style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(16.dp))
            val connected = deviceStatus == IQDevice.IQDeviceStatus.CONNECTED
            Button(onClick = onOpenAppClick, enabled = connected) {
                Text(text = if (appIsOpen) "App is Open" else "Open App")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (connected) {
                LazyColumn {
                    items(messages) { message ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onMessageClick(message) },
                            elevation = 4.dp
                        ) {
                            Text(
                                text = message.text,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = LocalContext.current.getString(R.string.connect_to_send),
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}