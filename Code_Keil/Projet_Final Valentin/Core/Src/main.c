#include "main.h"       // Inclusion du fichier d'en-tête principal du projet (généré par STM32CubeMX)
#include <string.h>     // Inclusion de la bibliothèque pour les fonctions de manipulation de chaînes (ex: strlen, strcmp)
#include <stdlib.h>     // Inclusion de la bibliothèque standard (ex: pour d'éventuelles fonctions utilitaires)
#include <math.h>       // Inclusion de la bibliothèque mathématique pour les fonctions comme sinf

#ifndef M_PI            // Si M_PI n'est pas déjà défini
#define M_PI 3.14159265358979323846 // Définit la constante PI
#endif

#define SINE_TABLE_SIZE 128     // Définit la taille de la table d'onde sinusoïdale (nombre d'échantillons)
uint16_t sine_table[SINE_TABLE_SIZE]; // Déclare un tableau pour stocker les échantillons de l'onde sinusoïdale

// Déclarations des handles pour les périphériques STM32
UART_HandleTypeDef huart1;      // Handle pour la communication UART1
DAC_HandleTypeDef hdac;         // Handle pour le convertisseur Numérique-Analogique (DAC)
DMA_HandleTypeDef hdma_dac1;    // Handle pour le contrôleur d'Accès Direct Mémoire (DMA) du DAC1
TIM_HandleTypeDef htim6;        // Handle pour le Timer 6

char uart_rx_buffer[8];         // Tampon de réception UART pour stocker les commandes (ex: "N:C#4\n")
volatile uint8_t uart_rx_ready = 0; // Drapeau volatile pour indiquer qu'une réception UART est terminée

// Structure pour associer une note à sa fréquence
typedef struct {
  const char *note;             // Nom de la note (ex: "C4")
  float frequency;              // Fréquence correspondante en Hz
} NoteFrequency;

// Tableau de structures définissant les notes de l'octave 4 et leurs fréquences
NoteFrequency notes_octave4[] = {
  {"C4", 261.63f}, {"C#4", 277.18f}, {"D4", 293.66f},
  {"D#4", 311.13f}, {"E4", 329.63f}, {"F4", 349.23f},
  {"F#4", 369.99f}, {"G4", 392.00f}, {"G#4", 415.30f},
  {"A4", 440.00f}, {"A#4", 466.16f}, {"B4", 493.88f}
};

// Fonction pour générer les échantillons de l'onde sinusoïdale
void Generate_Sine_Table(void) {
  for (int i = 0; i < SINE_TABLE_SIZE; i++) {
    // Calcule l'angle pour chaque échantillon (de 0 à 2*PI)
    float angle = 2.0f * M_PI * i / SINE_TABLE_SIZE;
    // Calcule la valeur de l'échantillon et la met à l'échelle pour le DAC (0-4095 pour 12 bits)
    sine_table[i] = (uint16_t)(2047 + 2047 * sinf(angle));
  }
}

// Fonction pour définir la fréquence de l'onde sinusoïdale de sortie
void Set_Sine_Frequency(float freq_hz) {
  if (freq_hz <= 0.0f) return; // Ne fait rien si la fréquence est nulle ou négative

  float timer_clock = 60000000.0f; // Fréquence de l'horloge du timer (60 MHz)
  float sample_rate = SINE_TABLE_SIZE * freq_hz; // Calcule le taux d'échantillonnage requis
  // Calcule la période du timer pour obtenir le taux d'échantillonnage souhaité
  uint32_t period = (uint32_t)((timer_clock / sample_rate) - 1);

  if (period < 10) period = 10;     // Assure une période minimale pour éviter des fréquences trop élevées
  if (period > 0xFFFF) period = 0xFFFF; // Clampe la période à la valeur maximale pour un timer 16 bits

  __HAL_TIM_DISABLE(&htim6);            // Désactive le Timer 6
  __HAL_TIM_SET_AUTORELOAD(&htim6, period); // Définit la nouvelle période du timer
  __HAL_TIM_SET_COUNTER(&htim6, 0);     // Réinitialise le compteur du timer
  __HAL_TIM_ENABLE(&htim6);             // Active le Timer 6

  // Redémarre le transfert DMA du DAC avec la table sinusoïdale
  HAL_DAC_Start_DMA(&hdac, DAC_CHANNEL_1, (uint32_t*)sine_table, SINE_TABLE_SIZE, DAC_ALIGN_12B_R);
}

// Fonction pour arrêter la lecture de l'onde sinusoïdale
void Stop_Sine(void) {
  HAL_DAC_Stop_DMA(&hdac, DAC_CHANNEL_1); // Arrête le transfert DMA du DAC
  // Met la sortie du DAC à 0 (silence)
  HAL_DAC_SetValue(&hdac, DAC_CHANNEL_1, DAC_ALIGN_12B_R, 0);
}

// Callback appelée lorsque la réception UART est complète
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart) {
  uart_rx_ready = 1; // Positionne le drapeau pour indiquer que des données sont prêtes
  // Relance la réception UART en mode interruption pour la prochaine commande
  HAL_UART_Receive_IT(&huart1, (uint8_t *)uart_rx_buffer, sizeof(uart_rx_buffer));
}

// Fonction pour traiter la commande de note reçue via UART
void Process_Note_Command(void) {
  uart_rx_buffer[7] = '\0'; // Ajoute un terminateur nul pour la sécurité de la chaîne

  if (strlen(uart_rx_buffer) < 4) return; // Vérifie la longueur minimale de la commande (ex: "N:C4\n")

  char action = uart_rx_buffer[0]; // Extrait l'action (N pour Note, S pour Stop)
  if (uart_rx_buffer[1] != ':') return; // Vérifie le séparateur ':'

  // Extrait la note entre ':' et '\n'
  char clean_note[4] = {0}; // Tampon pour stocker la note nettoyée
  int j = 0;
  for (int i = 2; i < 7 && j < 3; i++) {
    // S'arrête si un caractère de fin de ligne ou de chaîne est rencontré
    if (uart_rx_buffer[i] == '\n' || uart_rx_buffer[i] == '\r' || uart_rx_buffer[i] == '\0')
      break;
    clean_note[j++] = uart_rx_buffer[i]; // Copie le caractère de la note
  }

  // Parcourt le tableau des notes pour trouver la correspondance
  for (int i = 0; i < sizeof(notes_octave4)/sizeof(NoteFrequency); i++) {
    if (strcmp(clean_note, notes_octave4[i].note) == 0) { // Si la note correspond
      if (action == 'N') { // Si l'action est 'N' (Note)
        Set_Sine_Frequency(notes_octave4[i].frequency); // Joue la fréquence de la note
      } else { // Si l'action n'est pas 'N' (supposément 'S' pour Stop ou autre)
        Stop_Sine(); // Arrête la lecture de l'onde
      }
      break; // Sort de la boucle une fois la note traitée
    }
  }
}

/* Prototypes */
void SystemClock_Config(void);      // Prototype de la fonction de configuration de l'horloge système
static void MX_GPIO_Init(void);     // Prototype de la fonction d'initialisation des GPIO
static void MX_DMA_Init(void);      // Prototype de la fonction d'initialisation du DMA
static void MX_DAC_Init(void);      // Prototype de la fonction d'initialisation du DAC
static void MX_TIM6_Init(void);     // Prototype de la fonction d'initialisation du Timer 6
static void MX_USART1_UART_Init(void); // Prototype de la fonction d'initialisation de l'UART1
void Error_Handler(void);           // Prototype de la fonction de gestion des erreurs

/* Main */
int main(void) {
  HAL_Init();                   // Initialise la couche d'abstraction matérielle (HAL) de STM32
  SystemClock_Config();         // Configure l'horloge du système
  MX_GPIO_Init();               // Initialise les broches GPIO
  MX_DMA_Init();                // Initialise le contrôleur DMA
  MX_DAC_Init();                // Initialise le DAC
  MX_TIM6_Init();               // Initialise le Timer 6
  MX_USART1_UART_Init();        // Initialise l'UART1

  Generate_Sine_Table();        // Génère les échantillons de l'onde sinusoïdale

  HAL_DAC_Start(&hdac, DAC_CHANNEL_1); // Démarre le DAC
  HAL_TIM_Base_Start(&htim6);          // Démarre le Timer 6
  // Lance la première réception UART en mode interruption
  HAL_UART_Receive_IT(&huart1, (uint8_t *)uart_rx_buffer, sizeof(uart_rx_buffer));

  while (1) { // Boucle principale infinie
    if (uart_rx_ready) { // Si des données UART sont prêtes
      uart_rx_ready = 0; // Réinitialise le drapeau
      Process_Note_Command(); // Traite la commande de note reçue
    }
  }
}

/* -------------------------------------------------------------------------- */
/* Clock config */
// Fonction de configuration de l'horloge système
void SystemClock_Config(void) {
  RCC_OscInitTypeDef RCC_OscInitStruct = {0}; // Structure pour la configuration des oscillateurs
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0}; // Structure pour la configuration des horloges périphériques

  // Configuration de l'oscillateur HSI (High-Speed Internal)
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI; // Utilise l'oscillateur interne
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;                  // Active l'oscillateur HSI
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT; // Valeur de calibration par défaut
  // Configuration du PLL (Phase-Locked Loop)
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;              // Active le PLL
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSI;      // Source du PLL est le HSI
  RCC_OscInitStruct.PLL.PLLM = 10;                          // Diviseur pour l'entrée du PLL
  RCC_OscInitStruct.PLL.PLLN = 225;                         // Multiplicateur pour le VCO du PLL
  RCC_OscInitStruct.PLL.PLLP = RCC_PLLP_DIV6;               // Diviseur pour l'horloge système (SYSCLK)
  RCC_OscInitStruct.PLL.PLLQ = 4;                           // Diviseur pour l'horloge USB, SDIO, RNG
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)      // Applique la configuration des oscillateurs
    Error_Handler();                                        // En cas d'erreur, appelle Error_Handler

  // Configuration des horloges principales du système
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_SYSCLK // Type d'horloges à configurer
                              | RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK; // Source de l'horloge système est le PLL
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;        // Pas de division pour l'horloge AHB
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;         // Horloge APB1 divisée par 2
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;         // Pas de division pour l'horloge APB2

  // Applique la configuration des horloges et la latence de flash nécessaire
  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_1) != HAL_OK)
    Error_Handler(); // En cas d'erreur, appelle Error_Handler
}

/* -------------------------------------------------------------------------- */
/* DAC init */
// Fonction d'initialisation du DAC
static void MX_DAC_Init(void) {
  DAC_ChannelConfTypeDef sConfig = {0}; // Structure pour la configuration du canal DAC

  hdac.Instance = DAC;                  // Spécifie l'instance du DAC
  if (HAL_DAC_Init(&hdac) != HAL_OK)    // Initialise le DAC
    Error_Handler();                    // En cas d'erreur, appelle Error_Handler

  sConfig.DAC_Trigger = DAC_TRIGGER_T6_TRGO; // Le DAC est déclenché par le TIM6 TRGO
  sConfig.DAC_OutputBuffer = DAC_OUTPUTBUFFER_ENABLE; // Active le buffer de sortie du DAC
  // Configure le canal 1 du DAC
  if (HAL_DAC_ConfigChannel(&hdac, &sConfig, DAC_CHANNEL_1) != HAL_OK)
    Error_Handler(); // En cas d'erreur, appelle Error_Handler
}

/* -------------------------------------------------------------------------- */
/* TIM6 init */
// Fonction d'initialisation du Timer 6
static void MX_TIM6_Init(void) {
  TIM_MasterConfigTypeDef sMasterConfig = {0}; // Structure pour la configuration du mode maître du timer

  htim6.Instance = TIM6;                // Spécifie l'instance du Timer 6
  htim6.Init.Prescaler = 0;             // Pas de pré-division pour l'horloge (60 MHz)
  htim6.Init.CounterMode = TIM_COUNTERMODE_UP; // Compteur en mode croissant
  htim6.Init.Period = 12000 - 1;        // Période initiale du timer (détermine la fréquence d'échantillonnage)
  htim6.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE; // Pas de pré-chargement pour la période
  if (HAL_TIM_Base_Init(&htim6) != HAL_OK) // Initialise le timer en mode base
    Error_Handler();                    // En cas d'erreur, appelle Error_Handler

  sMasterConfig.MasterOutputTrigger = TIM_TRGO_UPDATE; // Le TRGO est généré sur chaque mise à jour du timer
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE; // Mode maître/esclave désactivé
  // Configure le mode maître du timer
  if (HAL_TIMEx_MasterConfigSynchronization(&htim6, &sMasterConfig) != HAL_OK)
    Error_Handler(); // En cas d'erreur, appelle Error_Handler
}

/* -------------------------------------------------------------------------- */
/* DMA init */
// Fonction d'initialisation du DMA
static void MX_DMA_Init(void) {
  __HAL_RCC_DMA1_CLK_ENABLE(); // Active l'horloge du contrôleur DMA1

  // Configure la priorité des interruptions pour le Stream 5 du DMA1 (utilisé par le DAC)
  HAL_NVIC_SetPriority(DMA1_Stream5_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Stream5_IRQn); // Active l'interruption pour le Stream 5 du DMA1
}

/* -------------------------------------------------------------------------- */
/* GPIO init */
// Fonction d'initialisation des GPIO
static void MX_GPIO_Init(void) {
  __HAL_RCC_GPIOA_CLK_ENABLE(); // Active l'horloge du port GPIOA (où sont connectés DAC et UART)
}

/* -------------------------------------------------------------------------- */
/* USART1 init */
// Fonction d'initialisation de l'UART1
static void MX_USART1_UART_Init(void) {
  huart1.Instance = USART1;             // Spécifie l'instance de l'UART1
  huart1.Init.BaudRate = 115200;        // Débit en bauds
  huart1.Init.WordLength = UART_WORDLENGTH_8B; // Longueur des mots : 8 bits
  huart1.Init.StopBits = UART_STOPBITS_1;     // Bit de stop : 1
  huart1.Init.Parity = UART_PARITY_NONE;      // Pas de parité
  huart1.Init.Mode = UART_MODE_TX_RX;         // Mode émetteur et récepteur
  huart1.Init.HwFlowCtl = UART_HWCONTROL_NONE; // Pas de contrôle de flux matériel
  huart1.Init.OverSampling = UART_OVERSAMPLING_16; // Suréchantillonnage pour la réception
  if (HAL_UART_Init(&huart1) != HAL_OK)       // Initialise l'UART
    Error_Handler();                        // En cas d'erreur, appelle Error_Handler
}

/* -------------------------------------------------------------------------- */
/* Error handler */
// Fonction de gestion des erreurs
void Error_Handler(void) {
  __disable_irq(); // Désactive toutes les interruptions
  while(1) {}      // Boucle infinie, bloque le programme en cas d'erreur critique
}
```
