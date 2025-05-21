package com.example.piano

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat

// Activité principale de l'application
class MainActivity : AppCompatActivity() {

    // Supprime les avertissements concernant le texte et l'accessibilité tactile
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Charge le layout de l'activité

        // Récupère le conteneur principal des touches du piano
        val pianoContainer = findViewById<LinearLayout>(R.id.pianoContainer)
        pianoContainer.setBackgroundColor(Color.DKGRAY) // Applique un fond gris foncé

        // Définit les positions des touches noires (sur une gamme chromatique)
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // Boucle sur 15 notes pour créer les touches du piano
        for (note in 1..15) {
            // Crée un FrameLayout pour empiler une touche blanche et éventuellement une noire
            val noteFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    160 // Hauteur de chaque ligne/touche
                ).apply {
                    setMargins(6, 6, 6, 6) // Marge autour de chaque FrameLayout
                }
            }

            // === Touche blanche ===
            val whiteButton = Button(this).apply {
                text = "Note $note" // Libellé de la touche
                textSize = 0f       // Mettre a 16f pour afficher la valeur des touches
                setBackgroundResource(R.drawable.white_button_background) // Fond personnalisé
                setTextColor(Color.BLACK) // Couleur du texte

                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    180 // Hauteur de la touche blanche
                ).apply {
                    // Si une touche noire suit cette touche, on réduit sa largeur visuelle
                    marginEnd = if (blackNotePositions.contains(note)) 320 else 0
                }

                elevation = 1f // Ombre légère pour effet de relief

                // Gère les événements tactiles : envoie les signaux NOTE_ON et NOTE_OFF
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            sendNoteOverBluetooth("NOTE_ON:$note")
                        }
                        MotionEvent.ACTION_UP -> {
                            sendNoteOverBluetooth("NOTE_OFF:$note")
                        }
                    }
                    false // Ne consomme pas l'événement pour laisser le système le gérer
                }
            }

            noteFrame.addView(whiteButton) // Ajoute la touche blanche au FrameLayout

            // === Touche noire (si applicable) ===
            if (blackNotePositions.contains(note)) {
                val blackButton = Button(this).apply {
                    text = "Note ${note}#" // Libellé pour la touche noire
                    textSize = 0f // Mettre a 16f pour afficher la valeur des touches
                    setTextColor(Color.WHITE)

                    layoutParams = FrameLayout.LayoutParams(
                        300, // Largeur fixe plus petite
                        180, // Même hauteur que la blanche
                        Gravity.TOP or Gravity.END // Positionnée en haut à droite
                    ).apply {
                        marginEnd = 0
                        topMargin = 0
                    }

                    elevation = 10f // Plus de relief pour apparaître au-dessus
                    background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                    // Événements tactiles pour touche noire
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                sendNoteOverBluetooth("NOTE_ON:${note}#")
                            }
                            MotionEvent.ACTION_UP -> {
                                sendNoteOverBluetooth("NOTE_OFF:${note}#")
                            }
                        }
                        false
                    }
                }

                noteFrame.addView(blackButton) // Ajoute la touche noire
            }

            // Ajoute le FrameLayout (touche blanche + éventuellement noire) au conteneur
            pianoContainer.addView(noteFrame)
        }
    }

    // Fonction simulant l'envoi d'une note via Bluetooth (à remplacer par ton implémentation réelle)
    private fun sendNoteOverBluetooth(note: String) {
        // TODO: Remplacer cette ligne par l'envoi Bluetooth réel via votre socket
        Log.d("Bluetooth", "Envoi de la note : $note")
    }
}
