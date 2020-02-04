package ilteoood.bluetoothpiconet

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import xdroid.toaster.Toaster
import java.util.*

class MainActivity : AppCompatActivity() {
    var deviceReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val broadcast = findViewById<View>(R.id.broadcast) as Button
        val discover = findViewById<View>(R.id.discover) as Button
        val send = findViewById<View>(R.id.send) as Button
        val message = findViewById<View>(R.id.message) as EditText
        val clientSpinner = findViewById<View>(R.id.clientSpinner) as Spinner
        val devices: MutableList<BluetoothDevice> = ArrayList()
        val updateHandler: Handler = object : Handler() {
            override fun handleMessage(message: Message) {
                val b = message.data
                val adapter: ArrayAdapter<String?> = ArrayAdapter(this@MainActivity,
                        android.R.layout.simple_spinner_item, b.getStringArrayList("devices")!!)
                clientSpinner.adapter = adapter
            }
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val manager = BluetoothManager(updateHandler, adapter)
        discover.setOnClickListener {
            if (!adapter.isDiscovering) {
                devices.clear()
                adapter.startDiscovery()
                Toaster.toast("Starting device discovery...")
            } else Toaster.toast("Discovering already started!")
        }
        deviceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action.toString()) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        devices.add(adapter.getRemoteDevice(device.address))
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Toaster.toast("Discovery finished!")
                        manager.start(devices)
                    }
                }
            }
        }
        registerReceiver(deviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(deviceReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        broadcast.setOnClickListener { manager.broadcast(message.text.toString()) }
        send.setOnClickListener { manager.sendMsg(clientSpinner.selectedItemPosition, message.text.toString()) }
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission()
    }

    override fun onDestroy() {
        deviceReceiver?.run { unregisterReceiver(this) }
        super.onDestroy()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                    Companion.REQUEST_COARSE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_COARSE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Fine location permission is not granted!")
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_COARSE_LOCATION = 42
        private const val TAG = "MainActivity"
    }
}