package com.example.piano

// === Importations nécessaires pour Android ===
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue // Pour convertir des unités de dimension (dp en pixels)
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Pour accéder aux ressources de manière compatible

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
import android.view.Gravity // Importation ajoutée pour Gravity


class MainActivity : AppCompatActivity() {

    // === Variables Bluetooth ===
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID standard (Universally Unique Identifier), Il doit être le même côté client et serveur
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Constantes pour les requêtes de permissions/activation Bluetooth
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private val REQUEST_ENABLE_BT = 2

    // Référence au bouton Bluetooth pour pouvoir le mettre à jour
    private lateinit var bluetoothConnectButton: ImageButton // Utilisez ImageButton pour l'icône seule

    // === Fonction d'envoi d'une note via Bluetooth  ===
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
                        Toast.makeText(this, "Échec de l'envoi ", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Log.w("Bluetooth", "Non connecté à un appareil Bluetooth. Note: $note non envoyée.")
            runOnUiThread {
                Toast.makeText(this, "Bluetooth non connecté.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === Vérifie les permissions Bluetooth de AndroidManifest ===
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
            // On ajoute la vérification de ACCESS_FINE_LOCATION
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                enableBluetooth()
            }
        }
    }
    // === Vérifie les permissions Bluetooth de AndroidManifest ===

    // === Gère le résultat de la demande de permissions ===
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
    // === Gère le résultat de la demande de permissions et active ou non le Bluetooth==

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

                // Ferme un socket précédemment ouvert si il existe
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
                    // Si vous voulez indiquer le nom de l'appareil connecté via une Toast ou un autre élément UI, faites-le ici.
                    // Pour le bouton qui est juste une icône, il n'y a pas de texte à mettre à jour.
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur de connexion à l'appareil: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Échec de la connexion à ${device.name} ", Toast.LENGTH_LONG).show()
                    // Si la connexion échoue, vous pouvez changer l'icône du bouton pour indiquer un état non connecté si vous avez une icône "non connectée".
                    // Pour l'instant, on laisse l'icône par default.
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

        // === Création de la vue racine (layout principal) ===
        // FrameLayout est un conteneur qui permet de superposer des éléments il va servir à contenir toute l'interface du piano
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // Prend toute la largeur de l'écran
                FrameLayout.LayoutParams.MATCH_PARENT  // Prend toute la hauteur de l'écran
            )
            setBackgroundColor(Color.DKGRAY) // Couleur de fond sombre pour imiter l'aspect d'un piano
        }

        // === Création d'un ScrollView pour permettre le défilement vertical ===
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // === Conteneur pour accueillir les touches blanches et noires
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT // S'ajuste en hauteur selon le contenu (les touches)
            )
        }

        // Conteneur des touches blanches LinearLayout vertical qui empile les touches blanches les unes au-dessus des autres
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL // Les touches sont disposées verticalement
        }


        // Bouton de connexion Bluetooth
        val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt() // Taille de l'icône
        val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()   // Marge

        bluetoothConnectButton = ImageButton(this).apply {
            setImageResource(R.drawable.baseline_bluetooth_24) // Définit l'icône
            setBackgroundColor(Color.TRANSPARENT) // Rendre le fond transparent

            setOnClickListener {
                enableBluetooth() // Lancer le processus de connexion
            }
            // Positionner le bouton en haut à droite
            val params = FrameLayout.LayoutParams(
                iconSizePx,
                iconSizePx
            ).apply {
                gravity = Gravity.TOP or Gravity.END // CHANGEMENT : Place le bouton en haut à droite
                topMargin = marginPx
                rightMargin = marginPx // CHANGEMENT : Utilise rightMargin pour la marge droite
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

        // Convertit 75dp en pixels pour une taille cohérente sur tous les écrans
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 75f, resources.displayMetrics
        ).toInt()

        // La largeur d'une touche = largeur totale de l'écran (en mode vertical)
        val keyWidth = resources.displayMetrics.widthPixels

        // Convertit 3dp en pixels pour la marge des touches
        val keyMarginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics
        ).toInt()

        // Convertit 62dp en pixels pour le décalage vertical des touches noires (ajusté pour le bouton Bluetooth)
        // Note: l'offset pourrait nécessiter un ajustement fin si la taille du bouton icône diffère beaucoup de l'ancien bouton texte.
        val blackKeyVerticalOffsetPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 62f, resources.displayMetrics
        ).toInt()


        // === Liste des noms réels des notes (touches blanches) ===
        // Chaque touche blanche a une note correspondant à une octave réelle
        val whiteNotes = listOf(
            "C4", "D4", "E4", "F4", "G4", "A4", "B4",
            "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"
        )

        // === Liste des noms réels des notes (touches noires) ===
        // Chaque touche noires a une note correspondant à une octave réelle
        val blackNotes = mapOf(
            1 to "C#4", 2 to "D#4", 4 to "F#4", 5 to "G#4", 6 to "A#4",
            8 to "C#5", 9 to "D#5", 11 to "F#5", 12 to "G#5", 13 to "A#5"
        )

        // === Création des touches blanches (avec noms d'octaves) ===
        for ((index, noteName) in whiteNotes.withIndex()) {
            val whiteKey = Button(this).apply {
                text = noteName
                textSize = 16f // Affiche la valeur des touches pour le debug
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background) // Style personnalisé

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, keyMarginPx, 0, keyMarginPx) // Petite marge entre les touches
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
                textSize = 16f  // Affiche la valeur des touches pour le debug
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
                    topMargin = ((position - 1) * (keyHeightPx + (2 * keyMarginPx))) + blackKeyVerticalOffsetPx // Ajustement pour le bouton
                }

                elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics) // S'assure que la touche noire est au-dessus visuellement

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

            pianoContainer.addView(blackKey) // Ajoute la touche noire par-dessus les touches blanches
        }
    }

    // Assure de fermer le socket Bluetooth lorsque l'activité est détruite pour éviter les fuites de ressources
    override fun onDestroy() {
        super.onDestroy()
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            // Pas de texte à réinitialiser pour un ImageButton
        } catch (e: IOException) {
            Log.e("Bluetooth", "Erreur lors de la fermeture des flux/sockets Bluetooth", e)
        }
    }
}
