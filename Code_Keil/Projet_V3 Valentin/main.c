#include <stm32f2xx.h>
#include <math.h>   // Pour la fonction sin()
#define PI 3.14159265359f // Définition de PI pour les calculs de sinusoïde

// --- Définitions pour le DAC ---
#define DAC_MAX_VALUE 4095      // Valeur maximale pour un DAC 12-bit
#define DAC_CENTER_VALUE 2047   // Valeur centrale pour un signal bipolaire sur 12-bit

// --- Paramètres de la sinusoïde ---
#define SINE_TABLE_SIZE 256     // Nombre de points dans la table de la sinusoïde
#define SINE_AMPLITUDE 2000     // Amplitude du signal (max 2047, min 0). Gardons un peu de marge.

// Déclaration de la table de sinusoïde (elle sera remplie au démarrage)
unsigned short sine_table[SINE_TABLE_SIZE];

// Déclaration de l'index global pour la table sinusoïdale
volatile unsigned int sine_index = 0;

// --- Fonction pour remplir la table de sinusoïde ---
void generateSineTable(void) {
    int i; // Déclaration de 'i' avant la boucle for
    for (i = 0; i < SINE_TABLE_SIZE; i++) {
        // Calcule la valeur sinusoïdale, la met à l'échelle et la décale pour le DAC
        // (sin() renvoie une valeur entre -1 et 1)
        // Multiplie par l'amplitude, puis ajoute la valeur centrale pour la rendre unipolaire.
        sine_table[i] = (unsigned short)(DAC_CENTER_VALUE + SINE_AMPLITUDE * sin((float)i * 2.0f * PI / SINE_TABLE_SIZE));
    }
}

// --- Nouvelle fonction pour générer la sinusoïde (envoie l'échantillon au DAC) ---
void generateSine(void) {
    // Écrit la valeur courante de la table au registre du DAC
    DAC->DHR12R1 = sine_table[sine_index];

    // Incrémente l'index pour le prochain échantillon
    sine_index++;

    // Boucle l'index si la fin de la table est atteinte
    if (sine_index >= SINE_TABLE_SIZE) {
        sine_index = 0;
    }
}

// --- Gestionnaire d'interruption du Timer 6 ---
void TIM6_DAC_IRQHandler(void) {
    // Vérifie et efface le drapeau d'interruption de mise à jour du Timer 6
    if (TIM6->SR & TIM_SR_UIF) { // Utilisation de la définition de bit pour la clarté
        TIM6->SR &= ~TIM_SR_UIF; // Effacer le drapeau

        // Appelle la fonction pour générer le signal sinusoïdal
        generateSine();
    }
}

// --- Fonction principale ---
int main(void) {
    // --- Initialisation des périphériques et des horloges ---

    // Activation de l'horloge pour le périphérique DAC (bit 29 du RCC_APB1ENR)
    // On met le 29ème bit à '1'
    RCC->APB1ENR |= (1 << 29);
    // Activation de l'horloge pour le port GPIOA (où se trouve la sortie DAC, PA4) (bit 0 du RCC_AHB1ENR)
    // On met le 0ème bit à '1'
    RCC->AHB1ENR |= (1 << 0);
    // Activation de l'horloge pour le Timer 6 (bit 4 du RCC_APB1ENR)
    // On met le 4ème bit à '1'
    RCC->APB1ENR |= (1 << 4);

    // Configuration de la broche PA4 en mode analogique
    // Met les bits 8 et 9 à '1' pour le mode analogique pour PA4
    // On met le 8ème bit à '1' ET le 9ème bit à '1'
    GPIOA->MODER |= ((1 << 8) | (1 << 9));

    // --- Configuration du DAC ---
    // Activation du canal 1 du DAC
    // On met le 0ème bit à '1' (pour le DAC Channel 1 Enable)
    DAC->CR |= (1 << 0);

    // --- Configuration du Timer 6 ---
    // Configuration du prescaler du Timer 6. PSC = 0 signifie pas de division.
    TIM6->PSC = 0;
    // Configuration de la valeur de rechargement automatique du Timer 6.
    // Pour une fréquence d'échantillonnage d'environ 3.84 MHz (pour obtenir 15 kHz avec 256 points):
    // Fréquence TIM_CLK (16 MHz)
    // ARR = (F_TIM_CLK / F_ech) - 1
    // ARR = (16,000,000 / 3,840,000) - 1 = 4.1666... - 1 = 3.1666...
    TIM6->ARR = 30; // <--- C'est la ligne modifiée pour 15 kHz avec TIM6 à 16 MHz

    // Active l'interruption de mise à jour du Timer 6
    TIM6->DIER |= TIM_DIER_UIE;

    // Configuration de la priorité de l'interruption du Timer 6 et son activation dans le NVIC
    NVIC_SetPriority(TIM6_DAC_IRQn, 0); // Priorité 0
    NVIC_EnableIRQ(TIM6_DAC_IRQn);       // Active l'interruption

    // --- Remplir la table de sinusoïde une fois au démarrage ---
    generateSineTable();

    // --- Démarrage du Timer 6 ---
    TIM6->CR1 |= TIM_CR1_CEN; // CEN (Counter Enable)

    // --- Boucle infinie ---
    while(1) {
        // Le microcontrôleur exécute cette boucle pendant que le timer et le DAC
        // génèrent le signal en arrière-plan.
    }

    return 0;
}