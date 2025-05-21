package com.example.piano

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

    // Fonction simulant l'envoi d'une note via Bluetooth
    private fun sendNoteOverBluetooth(note: String) {
        Log.d("Bluetooth", "Sending note: $note")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout principal qui contient tout
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.DKGRAY) // Couleur de fond
        }

        // ScrollView vertical pour permettre le glissement des touches
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Conteneur principal pour les touches blanches et noires, scrollable
        val pianoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Conteneur vertical pour les touches blanches
        val whiteKeyContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        // Ajout de la hiérarchie des vues
        scrollView.addView(pianoContainer)
        pianoContainer.addView(whiteKeyContainer)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        // Configuration des dimensions des touches
        val totalWhiteKeys = 15
        val keyHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics
        ).toInt() // Hauteur d'une touche blanche en pixels
        val keyWidth = resources.displayMetrics.widthPixels // Largeur de l'écran

        // Positions des notes noires (les touches #)
        val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

        // Création des touches blanches
        for (note in 1..totalWhiteKeys) {
            val whiteKey = Button(this).apply {
                text = "Note $note"
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.white_button_background)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    keyHeightPx
                ).apply {
                    setMargins(0, 4, 0, 4) // Marge entre les touches
                }

                // Événement tactile : envoie la note au toucher
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> sendNoteOverBluetooth("NOTE_ON:$note")
                        MotionEvent.ACTION_UP -> sendNoteOverBluetooth("NOTE_OFF:$note")
                    }
                    false
                }
            }

            whiteKeyContainer.addView(whiteKey)
        }

        // Création des touches noires superposées aux blanches
        for (note in blackNotePositions) {
            val blackKey = Button(this).apply {
                text = "Note ${note}#"
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                // Dimensions de la touche noire (plus petite que la blanche)
                val blackWidth = (keyWidth * 0.6).toInt()
                val blackHeight = (keyHeightPx * 0.6).toInt()

                // Positionnement de la touche noire
                layoutParams = FrameLayout.LayoutParams(
                    blackWidth,
                    blackHeight
                ).apply {
                    leftMargin = (keyWidth * 0.4).toInt() // Centrée sur la touche blanche
                    topMargin = ((note - 1) * (keyHeightPx + 8)) + 170 // Alignée verticalement
                }

                elevation = 12f // Pour être au-dessus des touches blanches

                // Événement tactile : envoie la note #
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> sendNoteOverBluetooth("NOTE_ON:${note}#")
                        MotionEvent.ACTION_UP -> sendNoteOverBluetooth("NOTE_OFF:${note}#")
                    }
                    false
                }
            }

            // Ajout des touches noires dans le même conteneur que les blanches
            pianoContainer.addView(blackKey)
        }
    }
}

