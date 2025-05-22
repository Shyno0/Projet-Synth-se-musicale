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

    // Liste des noms de notes blanches avec octave (du C4 au C6)
    private val whiteNotes = listOf(
        "C4", "D4", "E4", "F4", "G4", "A4", "B4",
        "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"
    )

    // Map des positions de touches noires et leur nom réel
    private val blackNotesMap = mapOf(
        0 to "C#4", 1 to "D#4",
        3 to "F#4", 4 to "G#4", 5 to "A#4",
        7 to "C#5", 8 to "D#5",
        10 to "F#5", 11 to "G#5", 12 to "A#5"
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // === Création de la hiérarchie des vues ===

        // Root Layout (FrameLayout) qui contient tout l'interface utilisateur (UI)
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // Prend toute la largeur de l'écran
                FrameLayout.LayoutParams.MATCH_PARENT  // Prend toute la hauteur de l'écran
            )
            setBackgroundColor(Color.DKGRAY) // Fond sombre pour ressembler à un vrai piano
        }

        // ScrollView vertical pour pouvoir faire défiler les touches du piano
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // ScrollView prend toute la largeur
                FrameLayout.LayoutParams.MATCH_PARENT  // ScrollView prend toute la hauteur
            )
        }

        // Conteneur principal du piano (touches blanches + touches noires superposées)
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // Largeur complète
                FrameLayout.LayoutParams.WRAP_CONTENT  // Hauteur ajustée selon les touches
            )
        }

        // Conteneur vertical pour les touches blanches uniquement
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // Largeur complète
                FrameLayout.LayoutParams.WRAP_CONTENT  // Hauteur dépend du nombre de touches
            )
            orientation = LinearLayout.VERTICAL // Les touches seront empilées verticalement
        }

        // Ajout des conteneurs dans la hiérarchie (ordre important)
        scrollView.addView(pianoContainer)         // Le piano est scrollable
        pianoContainer.addView(whiteKeyContainer)  // Les touches blanches sont dans le piano
        rootLayout.addView(scrollView)             // ScrollView dans le layout principal
        setContentView(rootLayout)                 // Affiche le tout à l'écran

        // === Configuration des dimensions des touches ===

        val totalWhiteKeys = 15 // Nombre total de touches blanches

        // Convertit 100dp en pixels pour la hauteur des touches blanches
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 75f, resources.displayMetrics
        ).toInt()

        val keyWidth = resources.displayMetrics.widthPixels // Largeur d'une touche = largeur écran

        // Ensemble contenant les positions où une touche noire (#) doit être dessinée
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // === Création des touches blanches ===
        for (i in 0 until totalWhiteKeys) {
            val noteName = whiteNotes.getOrNull(i) ?: "Note $i" // Nom de la note réelle

            val whiteKey = Button(this).apply {
                text = noteName  // Nom de la note réelle (ex: C4)
                textSize = 0f    // 0f pour ne pas afficher le texte. Remplacer par 16f pour debug
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background) // Style personnalisé

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, 4, 0, 4) // Petite marge entre les touches
                }

                // Gère l'événement tactile pour envoyer une note (avec multitouch)
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.LTGRAY) // Animation d'appui visuel (modif)
                            sendNoteOverBluetooth("ON:$noteName") // Envoie lors de l'appui
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_CANCEL -> { // ← Gère les interruptions comme le scroll (ajout)
                            v.setBackgroundResource(R.drawable.white_button_background) // Reset visuel (modif)
                            sendNoteOverBluetooth("OFF:$noteName") // Envoie lors du relâchement ou annulation
                        }
                    }
                    true // IMPORTANT : permet la gestion du multitouch (modif)
                }
            }

            whiteKeyContainer.addView(whiteKey) // Ajoute la touche blanche dans la colonne
        }

        // === Création des touches noires (superposées aux blanches) ===
        for ((position, noteName) in blackNotesMap) {
            val blackKey = Button(this).apply {
                text = noteName  // Nom de la note avec dièse
                textSize = 0f    // 0f pour ne pas afficher le texte. Remplacer par 16f pour debug
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                // Taille plus petite que les touches blanches
                val blackWidth = (keyWidth * 0.55).toInt()      // longueur touche noire
                val blackHeight = (keyHeightPx * 0.55).toInt() // largeur touche noire

                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.45).toInt()             // Centrée horizontalement
                    topMargin = ((position) * (keyHeightPx + 8)) + 130 // Décalée verticalement selon note
                }

                elevation = 12f // Plus élevée que les touches blanches pour apparaître au-dessus

                // Gère l'envoi Bluetooth au toucher (avec multitouch)
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            v.setBackgroundColor(Color.DKGRAY) // Animation d'appui visuel (modif)
                            sendNoteOverBluetooth("NOTE_ON:$noteName") // Envoie lors de l'appui
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_CANCEL -> { // ← Gère les interruptions comme le scroll (ajout)
                            v.background = ContextCompat.getDrawable(context, R.drawable.black_button_background)
                            sendNoteOverBluetooth("NOTE_OFF:$noteName") // Envoie lors du relâchement ou annulation
                        }
                    }
                    true // IMPORTANT : permet la gestion du multitouch (modif)
                }
            }

            pianoContainer.addView(blackKey) // Ajoute la touche noire par-dessus
        }
    }
}
