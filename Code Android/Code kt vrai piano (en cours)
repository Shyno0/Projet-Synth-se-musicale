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

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Référence au conteneur principal des touches du piano
        val pianoContainer = findViewById<LinearLayout>(R.id.pianoContainer)
        pianoContainer.setBackgroundColor(Color.BLACK) // Fond noir pour le piano

        // Positions des notes noires (dièses) sur le clavier
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // Création de 15 touches de piano
        for (note in 1..15) {
            // Conteneur pour chaque touche (permet de superposer une touche noire sur une blanche)
            val noteFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    160
                ).apply {
                    setMargins(6, 6, 6, 6) // Marges entre les touches
                }
            }

            // === Création de la touche blanche ===
            val whiteButton = Button(this).apply {
                text = "Note $note"
                textSize = 16f
                setBackgroundResource(R.drawable.white_button_background) // Style personnalisé
                setTextColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    180
                )
                elevation = 1f // Légère élévation pour effet visuel

                // Événement tactile (appui/relâchement) pour envoyer les commandes MIDI
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            sendNoteOverBluetooth("NOTE_ON:$note")
                        }
                        MotionEvent.ACTION_UP -> {
                            sendNoteOverBluetooth("NOTE_OFF:$note")
                        }
                    }
                    false
                }
            }

            // Ajout de la touche blanche au conteneur
            noteFrame.addView(whiteButton)

            // === Création de la touche noire si applicable ===
            if (blackNotePositions.contains(note)) {
                val blackButton = Button(this).apply {
                    text = "Note ${note}#"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    layoutParams = FrameLayout.LayoutParams(
                        300, // Largeur réduite
                        110, // Hauteur réduite
                        Gravity.TOP or Gravity.END // Position en haut à droite de la touche blanche
                    ).apply {
                        marginEnd = 20
                        topMargin = 50
                    }
                    elevation = 10f // Plus grande élévation pour être au-dessus des touches blanches
                    background = ContextCompat.getDrawable(context, R.drawable.black_button_background) // Style personnalisé

                    // Gestion du toucher (NOTE_ON et NOTE_OFF pour notes dièses)
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

                // Ajout de la touche noire et mise au premier plan
                noteFrame.addView(blackButton)
                blackButton.bringToFront()
            }

            // Ajout du conteneur de touche (blanche + éventuellement noire) au layout principal
            pianoContainer.addView(noteFrame)
        }
    }

    // Fonction fictive pour simuler l'envoi de la note via Bluetooth
    private fun sendNoteOverBluetooth(note: String) {
        // TODO: À remplacer par l'envoi réel via un socket Bluetooth
        Log.d("Bluetooth", "Envoi de la note : $note")
    }
}
