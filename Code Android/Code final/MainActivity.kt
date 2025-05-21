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

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // === Création de la hiérarchie des vues ===

            // Root Layout (FrameLayout) qui contient tout l'UI
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
                TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics
            ).toInt()

            val keyWidth = resources.displayMetrics.widthPixels // Largeur d'une touche = largeur écran

            // Ensemble contenant les positions où une touche noire (#) doit être dessinée
            val blackNotePositions = setOf(1, 2, 4, 5, 6, 8, 9, 11, 12, 13)

            // === Création des touches blanches ===
            for (note in 1..totalWhiteKeys) {
                val whiteKey = Button(this).apply {
                    text = "Note $note"  // Nom de la note
                    textSize = 0f        // 0f pour ne pas afficher le texte. Remplacer par 16f pour debug
                    setTextColor(Color.BLACK)
                    setBackgroundResource(R.drawable.white_button_background) // Style personnalisé

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        keyHeightPx
                    ).apply {
                        setMargins(0, 4, 0, 4) // Petite marge entre les touches
                    }

                    // Gère l'événement tactile pour envoyer une note
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> sendNoteOverBluetooth("NOTE_ON:$note") // envoie lors de l'appuie
                            MotionEvent.ACTION_UP -> sendNoteOverBluetooth("NOTE_OFF:$note")  // envoie lors du relachement
                        }
                        false // Laisse l'événement continuer (effet visuel par défaut)
                    }
                }

                whiteKeyContainer.addView(whiteKey) // Ajoute la touche blanche dans la colonne
            }

            // === Création des touches noires (superposées aux blanches) ===
            for (note in blackNotePositions) {
                val blackKey = Button(this).apply {
                    text = "Note ${note}#"  // Nom de la note avec dièse
                    textSize = 0f           // 0f pour ne pas afficher le texte. Remplacer par 16f pour debug
                    setTextColor(Color.WHITE)
                    background = ContextCompat.getDrawable(context, R.drawable.black_button_background)

                    // Taille plus petite que les touches blanches
                    val blackWidth = (keyWidth * 0.55).toInt()    // longueur touche noire
                    val blackHeight = (keyHeightPx * 0.5).toInt() // largeur touche noire

                    layoutParams = FrameLayout.LayoutParams(
                        blackWidth,
                        blackHeight
                    ).apply {
                        leftMargin = (keyWidth * 0.45).toInt()             // Centrée horizontalement
                        topMargin = ((note - 1) * (keyHeightPx + 8)) + 170 // Décalée verticalement selon note
                    }

                    elevation = 12f // Plus élevée que les touches blanches pour apparaître au-dessus

                    // Gère l'envoi Bluetooth au toucher
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> sendNoteOverBluetooth("NOTE_ON:${note}#") // envoie lors de l'appuie
                            MotionEvent.ACTION_UP -> sendNoteOverBluetooth("NOTE_OFF:${note}#")  // envoie lors du relachement
                        }
                        false
                    }
                }

                pianoContainer.addView(blackKey) // Ajoute la touche noire par-dessus
            }
        }
    }
