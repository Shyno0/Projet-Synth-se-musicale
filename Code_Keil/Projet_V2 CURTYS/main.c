#include "stdint.h"
#include "string.h"   // Pour strlen, strcmp, strncmp, memset
#include "cmsis_os.h" // Pour les fonctionnalités RTOS
#include "Driver_USART.h" // Pour le pilote USART
#include "Board_LED.h"    // Pour le contrôle des LEDs

// Déclaration de l'instance du driver USART
extern ARM_DRIVER_USART Driver_USART1;

// --- Variables globales et définitions RTOS ---
void app_main(void const *argument);
void USART1_Callback(uint32_t event);

osThreadDef(app_main, osPriorityNormal, 1, 0); // Définition de la tâche app_main
osSemaphoreDef(rx_semaphore);                 // Définition du sémaphore pour la notification de message complet
osSemaphoreId rx_semaphore_id;                // ID du sémaphore

#define RX_BUFFER_SIZE 32 // Taille maximale du message attendu (ex: "N:C#5\n")
uint8_t rx_char;          // Caractère unique reçu
char rx_buffer[RX_BUFFER_SIZE]; // Buffer pour stocker le message complet
uint32_t rx_buffer_idx = 0;     // Index actuel dans le buffer de réception

// --- Initialisation de l'USART ---
void Init_USART1(void) {
    Driver_USART1.Initialize(USART1_Callback); // Passer le callback pour les événements USART
    Driver_USART1.PowerControl(ARM_POWER_FULL); // Allumer l'USART
    Driver_USART1.Control(ARM_USART_MODE_ASYNCHRONOUS |
                          ARM_USART_DATA_BITS_8 |
                          ARM_USART_PARITY_NONE |
                          ARM_USART_STOP_BITS_1 |
                          ARM_USART_FLOW_CONTROL_NONE, 115200); // Débit en bauds réglé à 115200
    Driver_USART1.Control(ARM_USART_CONTROL_TX, 1); // Activer l'émission
    Driver_USART1.Control(ARM_USART_CONTROL_RX, 1); // Activer la réception
    // Armer la première réception. Le pilote lira un caractère et appellera le callback.
    Driver_USART1.Receive(&rx_char, 1);
}

// --- Fonction de Callback de l'USART ---
// Appelée par le driver USART lorsque des événements se produisent (ex: réception)
void USART1_Callback(uint32_t event) {
    if (event & ARM_USART_EVENT_RECEIVE_COMPLETE) {
        // Un caractère a été reçu et est dans 'rx_char'.

        // Écho du caractère reçu pour le débogage sur le terminal
        Driver_USART1.Send(&rx_char, 1);

        // Ajout du caractère au buffer de réception du message complet
        if (rx_buffer_idx < (RX_BUFFER_SIZE - 1)) { // Évite le dépassement de buffer
            rx_buffer[rx_buffer_idx++] = rx_char;
        }

        // Si le caractère reçu est une fin de ligne ('\n'), le message est complet.
        if (rx_char == '\n') {
            rx_buffer[rx_buffer_idx] = '\0'; // Null-terminate la chaîne
            osSemaphoreRelease(rx_semaphore_id); // Signaler à la tâche app_main qu'un message est prêt
            rx_buffer_idx = 0; // Réinitialiser l'index pour le prochain message
        }
        // Réarmer la réception pour le prochain caractère.
        Driver_USART1.Receive(&rx_char, 1);
    }
}

// --- Tâche principale de l'application ---
void app_main(void const *argument) {
    char *note_name; // Déclaration
    size_t len;      // Déclaration

    while (1) {
        // Attendre qu'un message complet soit reçu (sémaphore libéré par le callback)
        osSemaphoreWait(rx_semaphore_id, osWaitForever);

        // === Message de debug: Affiche le message complet reçu sur le terminal ===
        Driver_USART1.Send((uint8_t*)"\nMessage complet recu: ", 23);
        Driver_USART1.Send((uint8_t*)rx_buffer, strlen(rx_buffer));
        Driver_USART1.Send((uint8_t*)"\n", 1);

        // --- Logique de traitement des messages Bluetooth ---
        // Exemple de message attendu : "N:C4\n" ou "F:C4\n"

        note_name = &rx_buffer[2]; // Pointe après "N:" ou "F:"
        len = strlen(note_name);
        if (len > 0 && note_name[len - 1] == '\n') { // Supprimer le '\n' si présent pour la comparaison
            note_name[len - 1] = '\0';
        }

        if (strncmp(rx_buffer, "N:", 2) == 0) { // Si le message commence par "N:" (Note On)
            if (strcmp(note_name, "C4") == 0) {
                LED_On(0);
                Driver_USART1.Send((uint8_t*)"LED0 ON (C4)\n", 13);
            } else if (strcmp(note_name, "D4") == 0) {
                LED_On(1);
                Driver_USART1.Send((uint8_t*)"LED1 ON (D4)\n", 13);
            } else if (strcmp(note_name, "E4") == 0) {
                LED_On(2);
                Driver_USART1.Send((uint8_t*)"LED2 ON (E4)\n", 13);
            } else if (strcmp(note_name, "F4") == 0) {
                LED_On(3);
                Driver_USART1.Send((uint8_t*)"LED3 ON (F4)\n", 13);
            } else if (strcmp(note_name, "G4") == 0) { // Nouvelle note G4
                LED_On(4);
                Driver_USART1.Send((uint8_t*)"LED4 ON (G4)\n", 13);
            } else if (strcmp(note_name, "A4") == 0) { // Nouvelle note A4
                LED_On(5);
                Driver_USART1.Send((uint8_t*)"LED5 ON (A4)\n", 13);
            } else if (strcmp(note_name, "B4") == 0) { // Nouvelle note B4
                LED_On(6);
                Driver_USART1.Send((uint8_t*)"LED6 ON (B4)\n", 13);
            } else if (strcmp(note_name, "C5") == 0) { // Nouvelle note C5 (dernière LED disponible)
                LED_On(7);
                Driver_USART1.Send((uint8_t*)"LED7 ON (C5)\n", 13);
            }
            // Vous pouvez ajouter d'autres notes si vous avez plus de LEDs ou si vous voulez remapper
            // Par exemple, si vous avez des notes comme C#4, D#4, etc.
        } else if (strncmp(rx_buffer, "F:", 2) == 0) { // Si le message commence par "F:" (Note Off)
            if (strcmp(note_name, "C4") == 0) {
                LED_Off(0);
                Driver_USART1.Send((uint8_t*)"LED0 OFF (C4)\n", 14);
            } else if (strcmp(note_name, "D4") == 0) {
                LED_Off(1);
                Driver_USART1.Send((uint8_t*)"LED1 OFF (D4)\n", 14);
            } else if (strcmp(note_name, "E4") == 0) {
                LED_Off(2);
                Driver_USART1.Send((uint8_t*)"LED2 OFF (E4)\n", 14);
            } else if (strcmp(note_name, "F4") == 0) {
                LED_Off(3);
                Driver_USART1.Send((uint8_t*)"LED3 OFF (F4)\n", 14);
            } else if (strcmp(note_name, "G4") == 0) { // Nouvelle note G4
                LED_Off(4);
                Driver_USART1.Send((uint8_t*)"LED4 OFF (G4)\n", 14);
            } else if (strcmp(note_name, "A4") == 0) { // Nouvelle note A4
                LED_Off(5);
                Driver_USART1.Send((uint8_t*)"LED5 OFF (A4)\n", 14);
            } else if (strcmp(note_name, "B4") == 0) { // Nouvelle note B4
                LED_Off(6);
                Driver_USART1.Send((uint8_t*)"LED6 OFF (B4)\n", 14);
            } else if (strcmp(note_name, "C5") == 0) { // Nouvelle note C5
                LED_Off(7);
                Driver_USART1.Send((uint8_t*)"LED7 OFF (C5)\n", 14);
            }
        }
        // Réinitialiser le buffer pour le prochain message après traitement
        memset(rx_buffer, 0, RX_BUFFER_SIZE);
    }
}

// --- Fonction main du programme ---
int main(void) {
    LED_Initialize(); // Initialiser les LEDs
    // Créer le sémaphore initialisé à 0. Il sera libéré quand un message complet est reçu.
    rx_semaphore_id = osSemaphoreCreate(osSemaphore(rx_semaphore), 0);
    Init_USART1();    // Initialiser l'USART

    osKernelInitialize();           // Initialiser le noyau RTOS
    osThreadCreate(osThread(app_main), NULL); // Créer la tâche principale
    osKernelStart();                // Lancer le scheduler RTOS

    while (1) {
        // Cette boucle main est généralement vide après le démarrage du scheduler RTOS
        // car toutes les opérations sont gérées par les tâches.
    }
}