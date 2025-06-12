#include <stm32f2xx.h>
#include <math.h>                          // sin()
#define PI 3.14159265359f                 // PI

#define DAC_MAX_VALUE 4095                // 12-bit max
#define DAC_CENTER_VALUE 2047             // Centre pour signal bipolaire
#define SINE_TABLE_SIZE 1024               // Points dans la table
#define SINE_AMPLITUDE 2000              // Amplitude (max 2047)

unsigned short sine_table[SINE_TABLE_SIZE]; // Table de sinusoïde
volatile unsigned int sine_index = 0;       // Index global

void generateSineTable(void) {
    int i;
    for (i = 0; i < SINE_TABLE_SIZE; i++) {
        sine_table[i] = (unsigned short)(DAC_CENTER_VALUE + SINE_AMPLITUDE * sin((float)i * 2.0f * PI / SINE_TABLE_SIZE)); // Calcule sinusoïde
    }
}
int calc_freq(int freq) {
	int step;
	return step = (freq*4)/1000;
}
void generateSine(void) {
    DAC->DHR12R1 = sine_table[sine_index]; // Écrit dans le DAC
    sine_index = sine_index +1*8;                          // faire le code pour calc freq
    if (sine_index >= SINE_TABLE_SIZE) sine_index = 0; // Bouclage
}

void TIM6_DAC_IRQHandler(void) {
    if (TIM6->SR & TIM_SR_UIF) {          // Vérifie flag
        TIM6->SR &= ~TIM_SR_UIF;         // Efface flag
        generateSine();                   // Génère signal
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
