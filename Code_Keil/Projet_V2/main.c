#include "stm32f2xx_hal.h"
#include "cs42l52.h"
#include "math.h"
#include "stm32f2xx.h"                  // Device header

// === PROTOTYPES ===
void SystemClock_Config(void);
void MX_I2S3_Init(void);
void Generate_Tone(uint16_t* buf, uint32_t freq, uint32_t sample_rate, uint32_t length);
I2C_HandleTypeDef hi2c1;

// === VARIABLES ===
I2S_HandleTypeDef hi2s3;
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#define AUDIO_I2C_ADDR    0x4A
#define SAMPLE_RATE       16000
#define TONE_FREQ         1000   // 1 kHz bip
#define DURATION_MS       500
#define SAMPLE_COUNT      (SAMPLE_RATE * DURATION_MS / 1000)
uint16_t audioBuffer[SAMPLE_COUNT];

int main(void)
{
    HAL_Init();
    SystemClock_Config();
    MX_I2S3_Init();
		HAL_Init();
		SystemClock_Config();
		MX_I2C1_Init(); // très important pour que le codec fonctionne
		MX_I2S3_Init();

    // Initialiser le codec pour sortie haut-parleur
    cs42l52_Init(AUDIO_I2C_ADDR, OUTPUT_DEVICE_SPEAKER, 80, SAMPLE_RATE);

    // Génère une onde sinusoïdale dans le buffer
    Generate_Tone(audioBuffer, TONE_FREQ, SAMPLE_RATE, SAMPLE_COUNT);

    // Active la sortie
    cs42l52_Play(AUDIO_I2C_ADDR, audioBuffer, SAMPLE_COUNT);

    // Transmet l’audio via I2S (mode bloquant)
    HAL_I2S_Transmit(&hi2s3, audioBuffer, SAMPLE_COUNT, HAL_MAX_DELAY);

    // Arrête le son
    cs42l52_Stop(AUDIO_I2C_ADDR, CODEC_PDWN_SW);

    while (1);
}

// === Fonction de génération d’un bip sinusoïdal ===
void Generate_Tone(uint16_t* buf, uint32_t freq, uint32_t sample_rate, uint32_t length)
{
    for (uint32_t i = 0; i < length; i++)
    {
        float t = (float)i / sample_rate;
        buf[i] = (uint16_t)(0x7FFF * (sinf(2.0f * M_PI * freq * t)) + 0x8000);
    }
}

// === Init horloge système 120 MHz HSE ===
void SystemClock_Config(void)
{
    RCC_OscInitTypeDef RCC_OscInitStruct = {0};
    RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

    __HAL_RCC_PWR_CLK_ENABLE();

    // Retiré : __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

    RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
    RCC_OscInitStruct.HSEState = RCC_HSE_ON;
    RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
    RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
    RCC_OscInitStruct.PLL.PLLM = 8;
    RCC_OscInitStruct.PLL.PLLN = 240;
    RCC_OscInitStruct.PLL.PLLP = RCC_PLLP_DIV2;
    RCC_OscInitStruct.PLL.PLLQ = 5;
    if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
    {
        while (1);
    }

    RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_SYSCLK |
                                  RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2;
    RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
    RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
    RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV4;
    RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV2;

    if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_3) != HAL_OK)
    {
        while (1);
    }
}


// === Init I2S3 (mode Master Transmit) ===
void MX_I2S3_Init(void)
{
    __HAL_RCC_SPI3_CLK_ENABLE();

    hi2s3.Instance = SPI3;
    hi2s3.Init.Mode = I2S_MODE_MASTER_TX;
    hi2s3.Init.Standard = I2S_STANDARD_PHILIPS;
    hi2s3.Init.DataFormat = I2S_DATAFORMAT_16B;
    hi2s3.Init.MCLKOutput = I2S_MCLKOUTPUT_ENABLE;
    hi2s3.Init.AudioFreq = SAMPLE_RATE;
    hi2s3.Init.CPOL = I2S_CPOL_LOW;
    hi2s3.Init.ClockSource = I2S_CLOCK_PLL;

    if (HAL_I2S_Init(&hi2s3) != HAL_OK)
    {
        while (1);
    }
}
void MX_I2C1_Init(void)
{
    hi2c1.Instance = I2C1;
    hi2c1.Init.ClockSpeed = 100000;
    hi2c1.Init.DutyCycle = I2C_DUTYCYCLE_2;
    hi2c1.Init.OwnAddress1 = 0;
    hi2c1.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
    hi2c1.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
    hi2c1.Init.OwnAddress2 = 0;
    hi2c1.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
    hi2c1.Init.NoStretchMode = I2C_NOSTRETCH_DISABLE;

    if (HAL_I2C_Init(&hi2c1) != HAL_OK)
    {
        // Gestion d’erreur
        while (1);
    }
}
extern I2C_HandleTypeDef hi2c1;  // utilise l’I2C1 (déclaré ailleurs)

// Initialise l'I2C utilisé par le codec
void AUDIO_IO_Init(void)
{
    // Déjà initialisé via MX_I2C1_Init() si CubeMX utilisé
    // Sinon, initialisation manuelle à faire ici
}

// Arrête l'I2C
void AUDIO_IO_DeInit(void)
{
    HAL_I2C_DeInit(&hi2c1);
}

// Écrit une donnée dans un registre du codec
void AUDIO_IO_Write(uint16_t DevAddress, uint8_t Reg, uint8_t Value)
{
    HAL_I2C_Mem_Write(&hi2c1, DevAddress, Reg, I2C_MEMADD_SIZE_8BIT, &Value, 1, HAL_MAX_DELAY);
}

// Lit une donnée depuis un registre du codec
uint8_t AUDIO_IO_Read(uint16_t DevAddress, uint8_t Reg)
{
    uint8_t value = 0;
    HAL_I2C_Mem_Read(&hi2c1, DevAddress, Reg, I2C_MEMADD_SIZE_8BIT, &value, 1, HAL_MAX_DELAY);
    return value;
}

// Attente bloquante (en millisecondes)
void AUDIO_IO_Delay(uint32_t delay)
{
    HAL_Delay(delay);
}