#include "main.h"
#include <string.h>
#include <stdlib.h>
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define SINE_TABLE_SIZE 128
uint16_t sine_table[SINE_TABLE_SIZE];

UART_HandleTypeDef huart1;
DAC_HandleTypeDef hdac;
DMA_HandleTypeDef hdma_dac1;
TIM_HandleTypeDef htim6;

char uart_rx_buffer[8];  // Pour recevoir des chaînes comme "N:C#4\n"
volatile uint8_t uart_rx_ready = 0;

typedef struct {
  const char *note;
  float frequency;
} NoteFrequency;

NoteFrequency notes_octave4[] = {
  {"C4", 261.63f}, {"C#4", 277.18f}, {"D4", 293.66f},
  {"D#4", 311.13f}, {"E4", 329.63f}, {"F4", 349.23f},
  {"F#4", 369.99f}, {"G4", 392.00f}, {"G#4", 415.30f},
  {"A4", 440.00f}, {"A#4", 466.16f}, {"B4", 493.88f}
};

void Generate_Sine_Table(void) {
  for (int i = 0; i < SINE_TABLE_SIZE; i++) {
    float angle = 2.0f * M_PI * i / SINE_TABLE_SIZE;
    sine_table[i] = (uint16_t)(2047 + 2047 * sinf(angle));
  }
}

void Set_Sine_Frequency(float freq_hz) {
  if (freq_hz <= 0.0f) return;

  float timer_clock = 60000000.0f; // 60 MHz
  float sample_rate = SINE_TABLE_SIZE * freq_hz;
  uint32_t period = (uint32_t)((timer_clock / sample_rate) - 1);

  if (period < 10) period = 10;
  if (period > 0xFFFF) period = 0xFFFF;

  __HAL_TIM_DISABLE(&htim6);
  __HAL_TIM_SET_AUTORELOAD(&htim6, period);
  __HAL_TIM_SET_COUNTER(&htim6, 0);
  __HAL_TIM_ENABLE(&htim6);

  // Redémarrer DAC DMA
  HAL_DAC_Start_DMA(&hdac, DAC_CHANNEL_1, (uint32_t*)sine_table, SINE_TABLE_SIZE, DAC_ALIGN_12B_R);
}

void Stop_Sine(void) {
  HAL_DAC_Stop_DMA(&hdac, DAC_CHANNEL_1);
  HAL_DAC_SetValue(&hdac, DAC_CHANNEL_1, DAC_ALIGN_12B_R, 0); // Silence
}

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart) {
  uart_rx_ready = 1;
  HAL_UART_Receive_IT(&huart1, (uint8_t *)uart_rx_buffer, sizeof(uart_rx_buffer));
}

void Process_Note_Command(void) {
  uart_rx_buffer[7] = '\0'; // Sécurité

  if (strlen(uart_rx_buffer) < 4) return;  // Ex: "N:C4\n"

  char action = uart_rx_buffer[0];
  if (uart_rx_buffer[1] != ':') return;

  // Extraire la note entre ':' et '\n'
  char clean_note[4] = {0};
  int j = 0;
  for (int i = 2; i < 7 && j < 3; i++) {
    if (uart_rx_buffer[i] == '\n' || uart_rx_buffer[i] == '\r' || uart_rx_buffer[i] == '\0')
      break;
    clean_note[j++] = uart_rx_buffer[i];
  }

  for (int i = 0; i < sizeof(notes_octave4)/sizeof(NoteFrequency); i++) {
    if (strcmp(clean_note, notes_octave4[i].note) == 0) {
      if (action == 'N') {
        Set_Sine_Frequency(notes_octave4[i].frequency);
      } else {
        Stop_Sine();
      }
      break;
    }
  }
}

/* Prototypes */
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_DMA_Init(void);
static void MX_DAC_Init(void);
static void MX_TIM6_Init(void);
static void MX_USART1_UART_Init(void);
void Error_Handler(void);

/* Main */
int main(void) {
  HAL_Init();
  SystemClock_Config();
  MX_GPIO_Init();
  MX_DMA_Init();
  MX_DAC_Init();
  MX_TIM6_Init();
  MX_USART1_UART_Init();

  Generate_Sine_Table();

  HAL_DAC_Start(&hdac, DAC_CHANNEL_1);
  HAL_TIM_Base_Start(&htim6);
  HAL_UART_Receive_IT(&huart1, (uint8_t *)uart_rx_buffer, sizeof(uart_rx_buffer));

  while (1) {
    if (uart_rx_ready) {
      uart_rx_ready = 0;
      Process_Note_Command();
    }
  }
}

/* -------------------------------------------------------------------------- */
/* Clock config */
void SystemClock_Config(void) {
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSI;
  RCC_OscInitStruct.PLL.PLLM = 10;
  RCC_OscInitStruct.PLL.PLLN = 225;
  RCC_OscInitStruct.PLL.PLLP = RCC_PLLP_DIV6;
  RCC_OscInitStruct.PLL.PLLQ = 4;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
    Error_Handler();

  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_SYSCLK
                              | RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_1) != HAL_OK)
    Error_Handler();
}

/* -------------------------------------------------------------------------- */
/* DAC init */
static void MX_DAC_Init(void) {
  DAC_ChannelConfTypeDef sConfig = {0};

  hdac.Instance = DAC;
  if (HAL_DAC_Init(&hdac) != HAL_OK)
    Error_Handler();

  sConfig.DAC_Trigger = DAC_TRIGGER_T6_TRGO;
  sConfig.DAC_OutputBuffer = DAC_OUTPUTBUFFER_ENABLE;
  if (HAL_DAC_ConfigChannel(&hdac, &sConfig, DAC_CHANNEL_1) != HAL_OK)
    Error_Handler();
}

/* -------------------------------------------------------------------------- */
/* TIM6 init */
static void MX_TIM6_Init(void) {
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  htim6.Instance = TIM6;
  htim6.Init.Prescaler = 0;  // 60 MHz
  htim6.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim6.Init.Period = 12000 - 1;
  htim6.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim6) != HAL_OK)
    Error_Handler();

  sMasterConfig.MasterOutputTrigger = TIM_TRGO_UPDATE;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim6, &sMasterConfig) != HAL_OK)
    Error_Handler();
}

/* -------------------------------------------------------------------------- */
/* DMA init */
static void MX_DMA_Init(void) {
  __HAL_RCC_DMA1_CLK_ENABLE();

  HAL_NVIC_SetPriority(DMA1_Stream5_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Stream5_IRQn);
}

/* -------------------------------------------------------------------------- */
/* GPIO init */
static void MX_GPIO_Init(void) {
  __HAL_RCC_GPIOA_CLK_ENABLE();
}

/* -------------------------------------------------------------------------- */
/* USART1 init */
static void MX_USART1_UART_Init(void) {
  huart1.Instance = USART1;
  huart1.Init.BaudRate = 115200;
  huart1.Init.WordLength = UART_WORDLENGTH_8B;
  huart1.Init.StopBits = UART_STOPBITS_1;
  huart1.Init.Parity = UART_PARITY_NONE;
  huart1.Init.Mode = UART_MODE_TX_RX;
  huart1.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart1.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart1) != HAL_OK)
    Error_Handler();
}

/* -------------------------------------------------------------------------- */
/* Error handler */
void Error_Handler(void) {
  __disable_irq();
  while(1) {}
}
