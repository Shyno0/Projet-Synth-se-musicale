package com.example.piano

// === Importations nécessaires pour le Bluetooth, l'UI et autres fonctionnalités Android ===
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    // === Déclarations liées au Bluetooth ===
    private var bluetoothAdapter: BluetoothAdapter? = null                // Adaptateur Bluetooth de l'appareil
    private var bluetoothSocket: BluetoothSocket? = null                  // Socket pour la communication
    private var outputStream: OutputStream? = null                        // Flux de sortie pour envoyer les données
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID standard SPP (Serial Port Profile)

    // === Fonction pour envoyer une note via Bluetooth ===
    private fun sendNoteOverBluetooth(note: String) {
        try {
            outputStream?.write((note + "\n").toByteArray()) // Envoie la note suivie d’un retour à la ligne
            Log.d("Bluetooth", "Envoyé : $note")              // Log d'envoi
        } catch (e: Exception) {
            Log.e("Bluetooth", "Erreur d'envoi", e)
            Toast.makeText(this, "Erreur d'envoi Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // === Liste des noms de notes blanches (do, ré, mi...) de C4 à C6 ===
    private val whiteNotes = listOf(
        "C4", "D4", "E4", "F4", "G4", "A4", "B4",
        "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"
    )

    // === Mappage de certaines positions à leurs notes noires correspondantes ===
    private val blackNotesMap = mapOf(
        0 to "C#4", 1 to "D#4",
        3 to "F#4", 4 to "G#4", 5 to "A#4",
        7 to "C#5", 8 to "D#5",
        10 to "F#5", 11 to "G#5", 12 to "A#5"
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // === Création de la structure visuelle de l'application ===

        // Conteneur racine (piano + bouton + fond)
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.DKGRAY) // Fond général sombre
        }

        // ScrollView vertical pour permettre le défilement du piano (utile si on a beaucoup de touches)
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Conteneur principal du piano (superpose touches blanches et noires)
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Conteneur des touches blanches (empilées verticalement)
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        // Ajout des conteneurs à la hiérarchie
        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        // === Bouton pour sélectionner un module Bluetooth ===
        val selectDeviceButton = Button(this).apply {
            text = "Choisir un module Bluetooth"
            setBackgroundColor(Color.BLUE)
            setTextColor(Color.WHITE)
            setOnClickListener {
                showDeviceSelectionDialog()
            }
        }
        rootLayout.addView(selectDeviceButton) // Ajout du bouton dans l’interface

        // === Demande des permissions Bluetooth si nécessaire (Android 12+) ===
        requestBluetoothPermissions()

        // === Dimensions et nombre de touches blanches ===
        val totalWhiteKeys = 15

        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 75f, resources.displayMetrics
        ).toInt() // Hauteur d'une touche blanche

        val keyWidth = resources.displayMetrics.widthPixels // Largeur de l’écran

        // === Création des touches blanches ===
        for (i in 0 until totalWhiteKeys) {
            val noteName = whiteNotes.getOrNull(i) ?: "Note $i"

            val whiteKey = Button(this).apply {
                text = noteName
                textSize = 0f // Cache le texte (note visible uniquement via Bluetooth)
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, 4, 0, 4)
                }

                // Gère le toucher des touches blanches
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.LTGRAY) // Effet visuel
                            sendNoteOverBluetooth("ON:$noteName") // Envoi ON
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundResource(R.drawable.white_button_background)
                            sendNoteOverBluetooth("OFF:$noteName") // Envoi OFF
                        }
                    }
                    true
                }
            }

            whiteKeyContainer.addView(whiteKey)
        }

        // === Création des touches noires (superposées par-dessus les touches blanches) ===
        for ((position, noteName) in blackNotesMap) {
            val blackKey = Button(this).apply {
                text = noteName
                textSize = 0f
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                val blackWidth = (keyWidth * 0.55).toInt()
                val blackHeight = (keyHeightPx * 0.55).toInt()

                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.45).toInt() // Centrage approximatif
                    topMargin = ((position) * (keyHeightPx + 8)) + 130 // Placement vertical par rapport aux blanches
                }

                elevation = 12f // Effet de surélévation visuelle

                // Gère le toucher des touches noires
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.DKGRAY)
                            sendNoteOverBluetooth("NOTE_ON:$noteName")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.background = ContextCompat.getDrawable(context, R.drawable.black_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:$noteName")
                        }
                    }
                    true
                }
            }

            pianoContainer.addView(blackKey)
        }
    }

    // === Demande de permissions Bluetooth (obligatoire depuis Android 12) ===
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val missingPermissions = permissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
            }
        }
    }

    // === Boîte de dialogue pour choisir un module Bluetooth déjà appairé ===
    private fun showDeviceSelectionDialog() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth désactivé", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter!!.bondedDevices.toList()

        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Aucun appareil appairé trouvé", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = pairedDevices.map { it.name }

        // Affiche une liste de noms pour sélectionner un module
        AlertDialog.Builder(this)
            .setTitle("Sélectionnez un module Bluetooth")
            .setItems(deviceNames.toTypedArray()) { _, which ->
                val device = pairedDevices[which]
                connectToBluetoothDevice(device)
            }
            .show()
    }

    // === Connexion au module Bluetooth sélectionné ===
    private fun connectToBluetoothDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket?.close() // Ferme l'ancienne connexion si existante
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect() // Connexion
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Connecté à ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de connexion à ${device.name}", Toast.LENGTH_LONG).show()
            Log.e("Bluetooth", "Erreur de connexion", e)
        }
    }
}
