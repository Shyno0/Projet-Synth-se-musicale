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

class MainActivity : AppCompatActivity() {

    // === Fonction simulant l'envoi d'une note via Bluetooth ===
    // Ici, le message est simplement logué pour test/debug.
    private fun sendNoteOverBluetooth(note: String) {
        Log.d("Bluetooth", "Sending note: $note")
    }

    // Constante utilisée pour demander les permissions Bluetooth à l'utilisateur
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // === Vérifie dynamiquement les permissions Bluetooth (obligatoire à partir d'Android 12 / API 31) ===
    private fun checkAndRequestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }

            // Lance la requête de permissions si au moins une est manquante
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            }
        }
    }

    // === Fonction principale appelée au lancement de l'activité ===
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande les permissions Bluetooth si nécessaire
        checkAndRequestBluetoothPermissions()

        // === Création de la hiérarchie graphique (UI) ===

        // Layout racine (parent principal) en FrameLayout
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.DKGRAY) // Fond sombre pour simuler un piano
        }

        // Scroll vertical pour permettre de défiler le clavier si trop long
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Conteneur global du piano qui contient les touches blanches et noires superposées
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Conteneur vertical pour les touches blanches uniquement (une par ligne)
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL // Empile les touches verticalement
        }

        // Ajout de la structure hiérarchique dans le layout principal
        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
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
                text = noteName              // Affiche la note (ex: C4)
                textSize = 0f                // Mettre 16f pour affichage debug
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
                            sendNoteOverBluetooth("NOTE_ON:$noteName") // Envoi via Bluetooth simulé
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundResource(R.drawable.white_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:$noteName")
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
                val blackWidth = (keyWidth * 0.55).toInt()       // Largeur
                val blackHeight = (keyHeightPx * 0.55).toInt()   // Hauteur

                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.45).toInt() // Décalage horizontal pour centrer
                    topMargin = ((position - 1) * (keyHeightPx + 8)) + 95 // Placement vertical relatif
                }

                elevation = 12f // S'assure que la touche noire est au-dessus visuellement

                // Gestion des événements tactiles
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

            // Ajoute la touche noire par-dessus les touches blanches
            pianoContainer.addView(blackKey)
        }
    }
}
