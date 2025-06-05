package com.example.piano

// === Importations nécessaires pour Android ===
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// === Importations Bluetooth ===
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import android.app.AlertDialog

class MainActivity : AppCompatActivity() {


    // === Variables Bluetooth ===
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID (Universally Unique Identifier) pour votre service Bluetooth.
    // Il doit être le même côté client et serveur (l'appareil auquel vous vous connectez).
    // Ceci est un UUID standard pour le profil Serial Port Profile (SPP), souvent utilisé pour les microcontrôleurs.
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID SPP

    // Constantes pour les requêtes de permissions/activation Bluetooth
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private val REQUEST_ENABLE_BT = 2

    // === Fonction d'envoi d'une note via Bluetooth (réelle) ===
    private fun sendNoteOverBluetooth(note: String) {
        if (outputStream != null) {
            // L'envoi doit être fait sur un thread séparé pour ne pas bloquer l'interface utilisateur
            Thread {
                try {
                    val message = "$note\n" // Ajoute un retour à la ligne pour faciliter la lecture côté récepteur
                    outputStream?.write(message.toByteArray())
                    Log.d("Bluetooth", "Note envoyée: $note")
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Erreur lors de l'envoi de la note: $note", e)
                    runOnUiThread {
                        Toast.makeText(this, "Échec de l'envoi de la note: $note", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Log.w("Bluetooth", "Non connecté à un appareil Bluetooth. Note: $note non envoyée.")
            runOnUiThread {
                Toast.makeText(this, "Bluetooth non connecté. Veuillez connecter un appareil.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === Vérifie dynamiquement les permissions Bluetooth ===
    private fun checkAndRequestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                // Permissions déjà accordées, on peut tenter d'activer le Bluetooth
                enableBluetooth()
            }
        } else {
            // Pour les versions antérieures à Android 12, les permissions sont déjà dans le Manifest
            // (BLUETOOTH et BLUETOOTH_ADMIN) et ACCESS_FINE_LOCATION pour la découverte.
            // On peut tenter d'activer le Bluetooth directement.
            enableBluetooth()
        }
    }

    // Gère le résultat de la demande de permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                // Toutes les permissions sont accordées, tenter d'activer le Bluetooth
                enableBluetooth()
            } else {
                Toast.makeText(this, "Permissions Bluetooth refusées. Connexion impossible.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Active le Bluetooth si nécessaire
    private fun enableBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non pris en charge sur cet appareil", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // Bluetooth déjà activé, on peut proposer de connecter un appareil
            showPairedDevicesDialog()
        }
    }

    // Gère le résultat de l'activation du Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth activé", Toast.LENGTH_SHORT).show()
                    showPairedDevicesDialog() // Afficher les appareils appairés après activation
                } else {
                    Toast.makeText(this, "Bluetooth non activé", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Affiche une boîte de dialogue avec les appareils Bluetooth appairés
    private fun showPairedDevicesDialog() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth non disponible ou désactivé.", Toast.LENGTH_SHORT).show()
            return
        }

        // Vérifier la permission BLUETOOTH_CONNECT avant d'accéder aux appareils appairés
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            if (pairedDevices != null && pairedDevices.isNotEmpty()) {
                val devicesList = pairedDevices.map { it.name ?: "Nom inconnu" + "\n" + it.address }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Sélectionner un appareil Bluetooth")
                    .setItems(devicesList) { dialog, which ->
                        val selectedDeviceAddress = pairedDevices.elementAt(which).address
                        val selectedDevice = bluetoothAdapter?.getRemoteDevice(selectedDeviceAddress)
                        selectedDevice?.let { connectToDevice(it) }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            } else {
                Toast.makeText(this, "Aucun appareil Bluetooth appairé trouvé.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "La permission BLUETOOTH_CONNECT est requise pour voir les appareils appairés.", Toast.LENGTH_LONG).show()
            checkAndRequestBluetoothPermissions() // Redemander les permissions si nécessaire
        }
    }


    // Tente de se connecter à un appareil Bluetooth
    private fun connectToDevice(device: BluetoothDevice) {
        // La connexion doit se faire sur un thread séparé
        Thread {
            try {
                // Vérifier la permission BLUETOOTH_CONNECT avant de créer le socket
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(this, "Permission BLUETOOTH_CONNECT manquante pour la connexion.", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread // Sortir du thread si la permission est manquante
                }

                // Fermer un socket précédemment ouvert si il existe
                try {
                    bluetoothSocket?.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Erreur lors de la fermeture du socket précédent", e)
                }

                // Créer un socket RFCOMM
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect() // Tenter la connexion

                outputStream = bluetoothSocket?.outputStream // Obtenir le flux de sortie
                Log.d("Bluetooth", "Connecté à ${device.name}")
                runOnUiThread {
                    Toast.makeText(this, "Connecté à ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur de connexion à l'appareil: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Échec de la connexion à ${device.name}: ${e.message}", Toast.LENGTH_LONG).show()
                }
                // Tenter de fermer le socket en cas d'erreur
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    Log.e("Bluetooth", "Impossible de fermer le socket client", closeException)
                }
            }
        }.start()
    }

    // === Fonction principale appelée au lancement de l'activité ===
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande les permissions Bluetooth si nécessaire
        checkAndRequestBluetoothPermissions()

        // === Création de la hiérarchie graphique (UI) ===

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.DKGRAY) // Fond sombre pour simuler un piano
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL // Empile les touches verticalement
        }

        // Bouton de connexion Bluetooth
        val bluetoothConnectButton = Button(this).apply {
            text = "Bluetooth"
            setBackgroundColor(Color.BLUE)
            setTextColor(Color.WHITE)
            setOnClickListener {
                enableBluetooth() // Lancer le processus de connexion
            }
            // Positionner le bouton en haut à gauche
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Combinaison de Gravity.TOP et Gravity.START (ou Gravity.LEFT)
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = 10 // Marge par rapport au haut
                leftMargin = 10 // Marge par rapport à la gauche
            }
            layoutParams = params
        }

        // Ajout de la structure hiérarchique dans le layout principal
        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
        rootLayout.addView(bluetoothConnectButton) // Ajoutez le bouton à votre layout racine
        setContentView(rootLayout) // Affiche le layout complet à l'écran

        // === Dimensions des touches ===

        val totalWhiteKeys = 15 // Nombre de touches blanches : de C4 à C6 inclus

        // Convertit 75dp en pixels pour une taille cohérente sur tous les écrans
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 75f, resources.displayMetrics
        ).toInt()

        // La largeur d'une touche = largeur totale de l'écran (en mode vertical)
        val keyWidth = resources.displayMetrics.widthPixels

        // Positions spécifiques où dessiner une touche noire (entre les touches blanches)
        // Ces indices correspondent à des positions verticales
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // === Liste des noms réels des notes (touches blanches) ===
        // Chaque touche blanche a une note correspondant à une octave réelle
        val whiteNotes = listOf(
            "C4", "D4", "E4", "F4", "G4", "A4", "B4",
            "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"
        )

        // === Map des touches noires avec leur position et nom réel ===
        val blackNotes = mapOf(
            1 to "C#4", 2 to "D#4", 4 to "F#4", 5 to "G#4", 6 to "A#4",
            8 to "C#5", 9 to "D#5", 11 to "F#5", 12 to "G#5", 13 to "A#5"
        )

        // === Création des touches blanches (avec noms d'octaves) ===
        for ((index, noteName) in whiteNotes.withIndex()) {
            val whiteKey = Button(this).apply {
                text = noteName
                textSize = 0f
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background) // Style personnalisé

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, 4, 0, 4) // Petite marge entre les touches
                }

                // Gestion des événements tactiles (appui et relâchement)
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.LTGRAY) // Indique visuellement l'appui
                            sendNoteOverBluetooth("N:$noteName") // Envoi via Bluetooth
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundResource(R.drawable.white_button_background)
                            sendNoteOverBluetooth("F:$noteName")
                        }
                    }
                    true // Active le multitouch
                }
            }

            // Ajoute la touche blanche au conteneur vertical
            whiteKeyContainer.addView(whiteKey)
        }

        // === Création des touches noires (superposées) ===
        for ((position, noteName) in blackNotes) {
            val blackKey = Button(this).apply {
                text = noteName // Affiche la note noire (ex: C#4)
                textSize = 0f
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                // Taille des touches noires plus petite que les blanches
                val blackWidth = (keyWidth * 0.55).toInt()
                val blackHeight = (keyHeightPx * 0.55).toInt()

                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.45).toInt() // Décalage horizontal pour centrer
                    // topMargin doit être ajusté pour éviter le bouton de connexion Bluetooth
                    // Le bouton de connexion occupe les 20dp de marge + sa hauteur.
                    // Estimation simple : 100dp pour le bouton + marge, si le bouton est en haut.
                    topMargin = ((position - 1) * (keyHeightPx + 8)) +130 // Ajustement pour le bouton
                }

                elevation = 12f // S'assure que la touche noire est au-dessus visuellement

                // Gestion des événements tactiles
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.DKGRAY)
                            sendNoteOverBluetooth("N:$noteName")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.background = ContextCompat.getDrawable(context, R.drawable.black_button_background)
                            sendNoteOverBluetooth("F:$noteName")
                        }
                    }
                    true
                }
            }

            // Ajoute la touche noire par-dessus les touches blanches
            pianoContainer.addView(blackKey)
        }
    }

    // Assurez-vous de fermer le socket Bluetooth lorsque l'activité est détruite pour éviter les fuites de ressources
    override fun onDestroy() {
        super.onDestroy()
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Erreur lors de la fermeture des flux/sockets Bluetooth", e)
        }
    }
}
