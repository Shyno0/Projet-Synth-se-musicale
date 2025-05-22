package com.example.piano

// Importations nécessaires
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Fonction simulant l'envoi d'une note par Bluetooth (ici simplement loguée)
    private fun sendNoteOverBluetooth(note: String) {
        Log.d("Bluetooth", "Sending note: $note")
    }

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesListAdapter: ArrayAdapter<String>

    // Liste temporaire des périphériques trouvés
    private val discoveredDevices = mutableListOf<String>()

    // === BroadcastReceiver pour écouter les périphériques trouvés lors du scan ===
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val name = it.name ?: "Inconnu"
                        val address = it.address
                        val deviceInfo = "$name\n$address"

                        // Ajoute à la liste si pas déjà présent
                        if (!discoveredDevices.contains(deviceInfo)) {
                            discoveredDevices.add(deviceInfo)
                            devicesListAdapter.add(deviceInfo)
                            devicesListAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    // === Vérifie et demande dynamiquement les permissions Bluetooth ===
    private fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Pour Android 6 à 11 : besoin d'ACCESS_FINE_LOCATION pour scanner
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_BLUETOOTH_PERMISSIONS)
            }
        }
    }

    // === Lance le scan des appareils Bluetooth ===
    private fun startBluetoothScan() {
        discoveredDevices.clear()
        devicesListAdapter.clear()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Scan lancé...", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vérifie les permissions dès le lancement
        checkAndRequestBluetoothPermissions()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // === Création des vues ===
        val rootLayout = FrameLayout(this)
        val scrollView = ScrollView(this)
        val pianoContainer = FrameLayout(this)
        val whiteKeyContainer = LinearLayout(this)
        whiteKeyContainer.orientation = LinearLayout.VERTICAL

        // Ajout dans la hiérarchie
        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        // === Dimensions des touches ===
        val totalWhiteKeys = 15
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics
        ).toInt()
        val keyWidth = resources.displayMetrics.widthPixels
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // === Ajout des touches blanches ===
        for (note in 1..totalWhiteKeys) {
            val whiteKey = Button(this).apply {
                text = "Note $note"
                textSize = 0f
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, keyHeightPx
                ).apply { setMargins(0, 4, 0, 4) }

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.LTGRAY)
                            sendNoteOverBluetooth("NOTE_ON:$note")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundResource(R.drawable.white_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:$note")
                        }
                    }
                    true
                }
            }
            whiteKeyContainer.addView(whiteKey)
        }

        // === Ajout des touches noires ===
        for (note in blackNotePositions) {
            val blackKey = Button(this).apply {
                text = "Note ${note}#"
                textSize = 0f
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)
                val blackWidth = (keyWidth * 0.55).toInt()
                val blackHeight = (keyHeightPx * 0.55).toInt()
                layoutParams = FrameLayout.LayoutParams(blackWidth, blackHeight).apply {
                    leftMargin = (keyWidth * 0.45).toInt()
                    topMargin = ((note - 1) * (keyHeightPx + 8)) + 140
                }
                elevation = 12f
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.DKGRAY)
                            sendNoteOverBluetooth("NOTE_ON:${note}#")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.background = ContextCompat.getDrawable(context, R.drawable.black_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:${note}#")
                        }
                    }
                    true
                }
            }
            pianoContainer.addView(blackKey)
        }

        // === Ajout du bouton de scan Bluetooth ===
        val scanButton = ImageButton(this).apply {
            setImageResource(R.drawable.baseline_bluetooth_24)
            background = null // Pas de fond par défaut
            contentDescription = "Scanner les appareils Bluetooth"

            layoutParams = FrameLayout.LayoutParams(100, 100).apply {
                topMargin = 0
                leftMargin = 0
            }

            setOnClickListener {
                checkAndRequestBluetoothPermissions()
                startBluetoothScan()
            }
        }

        rootLayout.addView(scanButton)

        // === Ajout de la liste des appareils trouvés ===
        val listView = ListView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                600 // Hauteur fixe en px pour l'exemple
            ).apply {
                topMargin = 1400 // Décalage en bas (à adapter selon ton layout)
            }
        }

        // === Initialisation de l'adaptateur de liste ===
        devicesListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevices)
        listView.adapter = devicesListAdapter
        rootLayout.addView(listView)

        // === Enregistrement du BroadcastReceiver pour le scan ===
        registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Toujours désenregistrer le receiver pour éviter les fuites mémoire
        unregisterReceiver(bluetoothReceiver)
    }
}
