
package com.example.scooterxpro

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private lateinit var scanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var connectedGatt: BluetoothGatt? = null
    private lateinit var deviceListView: ListView
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var statusText: TextView
    private val sharedPrefs by lazy { getSharedPreferences("BLE_PREFS", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        statusText = TextView(this).apply { text = "Status: Rozłączony" }
        val scanButton = Button(this).apply {
            text = "Skanuj urządzenia BLE"
            setOnClickListener { startScan() }
        }
        val connectButton = Button(this).apply {
            text = "Połącz automatycznie"
            setOnClickListener { autoConnectLastDevice() }
        }
        val driveButton = Button(this).apply {
            text = "Tryb Drive (22 km/h)"
            setOnClickListener { sendCommand("MODE_DRIVE") }
        }
        val sportButton = Button(this).apply {
            text = "Tryb Sport (37 km/h)"
            setOnClickListener { sendCommand("MODE_SPORT") }
        }
        val diagButton = Button(this).apply {
            text = "Diagnostyka"
            setOnClickListener { sendCommand("DIAGNOSTIC") }
        }

        deviceListView = ListView(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        deviceListView.adapter = adapter
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            sharedPrefs.edit().putString("LAST_DEVICE", device.address).apply()
            connectToDevice(device)
        }

        layout.addView(statusText)
        layout.addView(scanButton)
        layout.addView(connectButton)
        layout.addView(driveButton)
        layout.addView(sportButton)
        layout.addView(diagButton)
        layout.addView(deviceListView)

        setContentView(layout)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        scanner = bluetoothAdapter.bluetoothLeScanner

        requestPermissions()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ), 1
        )
    }

    private fun startScan() {
        deviceList.clear()
        adapter.clear()
        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (!deviceList.contains(device)) {
                    deviceList.add(device)
                    adapter.add("${device.name ?: "Nieznane"} (${device.address})")
                    adapter.notifyDataSetChanged()
                }
            }
        })
        Toast.makeText(this, "Skanowanie...", Toast.LENGTH_SHORT).show()
    }

    private fun autoConnectLastDevice() {
        val lastAddress = sharedPrefs.getString("LAST_DEVICE", null)
        if (lastAddress == null) {
            Toast.makeText(this, "Brak zapisanego urządzenia", Toast.LENGTH_SHORT).show()
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(lastAddress)
        connectToDevice(device)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        statusText.text = "Łączenie z ${device.address}..."
        connectedGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread {
                        statusText.text = "Połączono z ${device.address}"
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    runOnUiThread {
                        statusText.text = "Rozłączono"
                    }
                }
            }
        })
    }

    private fun sendCommand(cmd: String) {
        Toast.makeText(this, "Wysyłam komendę: $cmd", Toast.LENGTH_SHORT).show()
        // Tu można dodać np. wysyłanie przez GATT characteristic do hulajnogi
    }
}
