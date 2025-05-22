package com.example.piano

// Importations nécessaires
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

    // Fonction simulant l'envoi d'une note par Bluetooth (ici simplement loguée)
    private fun sendNoteOverBluetooth(note: String) {
        Log.d("Bluetooth", "Sending note: $note")
    }

    // Constante utilisée pour la requête de permissions Bluetooth
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // === Fonction pour vérifier et demander dynamiquement les permissions Bluetooth (Android 12+) ===
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

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // === Vérifie et demande les permissions Bluetooth au lancement (Android 12+) ===
        checkAndRequestBluetoothPermissions()

        // === Création de la hiérarchie des vues ===

        // Root Layout (FrameLayout) qui contient tout l'UI
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.DKGRAY)
        }

        // ScrollView vertical pour pouvoir faire défiler les touches du piano
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Conteneur principal du piano (touches blanches + touches noires superposées)
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Conteneur vertical pour les touches blanches uniquement
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        // === Configuration des dimensions des touches ===

        val totalWhiteKeys = 15 // Nombre total de touches blanches (de C4 à C6 inclus)

        // Convertit 100dp en pixels pour la hauteur des touches blanches
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 75f, resources.displayMetrics
        ).toInt()

        val keyWidth = resources.displayMetrics.widthPixels // Largeur d'une touche = largeur écran

        // Ensemble contenant les positions où une touche noire (#) doit être dessinée
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // === Liste des vraies notes pour les touches blanches ===
        val whiteNotes = listOf(
            "C4", "D4", "E4", "F4", "G4", "A4", "B4",
            "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"
        )

        // === Liste des vraies notes pour les touches noires ===
        val blackNotes = mapOf(
            1 to "C#4", 2 to "D#4", 4 to "F#4", 5 to "G#4", 6 to "A#4",
            8 to "C#5", 9 to "D#5", 11 to "F#5", 12 to "G#5", 13 to "A#5"
        )

        // === Création des touches blanches ===
        for ((index, noteName) in whiteNotes.withIndex()) {
            val whiteKey = Button(this).apply {
                text = noteName // Affiche la note réelle (ex: C4)
                textSize = 0f   // 0f pour cacher, remplacer par 16f pour debug
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, 4, 0, 4)
                }

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.LTGRAY)
                            sendNoteOverBluetooth("NOTE_ON:$noteName")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundResource(R.drawable.white_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:$noteName")
                        }
                    }
                    true
                }
            }

            whiteKeyContainer.addView(whiteKey)
        }

        // === Création des touches noires ===
        for ((position, noteName) in blackNotes) {
            val blackKey = Button(this).apply {
                text = noteName  // Affiche la note réelle (ex: C#4)
                textSize = 0f    // 0f pour cacher, remplacer par 16f pour debug
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                val blackWidth = (keyWidth * 0.55).toInt()
                val blackHeight = (keyHeightPx * 0.55).toInt()

                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.45).toInt()
                    topMargin = ((position - 1) * (keyHeightPx + 8)) + 130
                }

                elevation = 12f

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
}
