#include <stm32f2xx.h>
#include <math.h>                          // sin()
#define PI 3.14159265359f                 // PI

#define DAC_MAX_VALUE 4095                // 12-bit max
#define DAC_CENTER_VALUE 2047             // Centre pour signal bipolaire
#define SINE_TABLE_SIZE 1024               // Points dans la table
#define SINE_AMPLITUDE 2000              // Amplitude (max 2047)
#define Do5 4.8f
#define DoSib5 5.2f  // Do#5 / Réb5
#define Re5 5.465f
#define ReSib5 5.68f  // Ré#5 / Mib5
#define Mi5 6.2f
#define Fa5 6.47f
#define FaSib5 6.9f  // Fa#5 / Solb5
#define Sol5 7.23f
#define SolSib5 7.72f // Sol#5 / Lab5
#define La5 8.0f
#define LaSib5 8.5f  // La#5 / Sib5
#define Si5 9.0f
unsigned short sine_table[SINE_TABLE_SIZE]; // Table de sinusoïde
volatile float sine_index = 0.0f;      // Index global

void generateSineTable(void) {
    int i;
    for (i = 0; i < SINE_TABLE_SIZE; i++) {
        sine_table[i] = (unsigned short)(DAC_CENTER_VALUE + SINE_AMPLITUDE * sin((float)i * 2.0f * PI / SINE_TABLE_SIZE)); // Calcule sinusoïde
    }
}
/*int calc_freq(int freq) {
	int step;
	return step = (freq*4)/1000;
}*/
void generateSine(float note) {
    int index_int = (int)sine_index;               // Convertir en entier pour l'accès à la table
    DAC->DHR12R1 = sine_table[index_int];          // Écrit dans le DAC
    sine_index += note;                             
    if (sine_index >= SINE_TABLE_SIZE)             // Dépassement
        sine_index -= SINE_TABLE_SIZE;             // Wrap autour proprement
}

void TIM6_DAC_IRQHandler(void) {
    if (TIM6->SR & TIM_SR_UIF) {          // Vérifie flag
        TIM6->SR &= ~TIM_SR_UIF;         // Efface flag
        generateSine(Do5);                   // Génère signal
    }
}

int main(void) {
    RCC->APB1ENR |= (1 << 29);            // DAC clock
    RCC->AHB1ENR |= (1 << 0);             // GPIOA clock
    RCC->APB1ENR |= (1 << 4);             // TIM6 clock

    GPIOA->MODER |= ((1 << 8) | (1 << 9)); // PA4 analogique

    DAC->CR |= (1 << 0);                  // Active DAC1

    TIM6->PSC = 0;                        // Prescaler = 0
    TIM6->ARR = 2;                       
    TIM6->DIER |= TIM_DIER_UIE;          // Active interruption
    NVIC_SetPriority(TIM6_DAC_IRQn, 0);  // Priorité IRQ
    NVIC_EnableIRQ(TIM6_DAC_IRQn);       // Active IRQ

    generateSineTable();                 // Remplit table

    TIM6->CR1 |= TIM_CR1_CEN;            // Démarre timer

    while(1) {}                           // Boucle infinie
    return 0;
}
