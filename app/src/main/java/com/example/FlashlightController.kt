package com.example

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FlashlightController(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            Log.e("FlashlightController", "Error initializing camera ID", e)
            _errorMessage.value = "Kamera erişim hatası: ${e.localizedMessage}"
        }
    }

    fun toggleTorch(enabled: Boolean) {
        val id = cameraId
        if (id == null) {
            _errorMessage.value = "Cihazınızda kamera flaşı bulunamadı!"
            _isTorchOn.value = enabled // Still set value for UI representation (e.g. screen flash)
            return
        }
        try {
            cameraManager.setTorchMode(id, enabled)
            _isTorchOn.value = enabled
            _errorMessage.value = null
        } catch (e: CameraAccessException) {
            Log.e("FlashlightController", "CameraAccessException toggling torch", e)
            _errorMessage.value = "Flaş şu anda başka bir uygulama tarafından kullanılıyor veya kilitli."
            _isTorchOn.value = enabled // Let it toggle on-screen indicator
        } catch (e: Exception) {
            Log.e("FlashlightController", "Exception toggling torch", e)
            _errorMessage.value = "Flaş açılamadı: ${e.localizedMessage}"
            _isTorchOn.value = enabled
        }
    }

    fun stopAll() {
        job?.cancel()
        job = null
        toggleTorch(false)
    }

    fun startStrobe(hz: Float) {
        stopAll()
        if (hz <= 0f) return
        val periodMs = (1000 / hz).toLong()
        val onTime = periodMs / 2
        val offTime = periodMs - onTime

        job = scope.launch {
            try {
                while (isActive) {
                    toggleTorch(true)
                    delay(onTime)
                    toggleTorch(false)
                    delay(offTime)
                }
            } finally {
                toggleTorch(false)
            }
        }
    }

    fun startSOS() {
        stopAll()
        job = scope.launch {
            try {
                val dot = 150L
                val dash = 450L
                val elementSpace = 150L
                val charSpace = 450L
                val wordSpace = 1050L

                // SOS: ... --- ...
                while (isActive) {
                    // S: . . .
                    for (i in 0 until 3) {
                        if (!isActive) break
                        toggleTorch(true)
                        delay(dot)
                        toggleTorch(false)
                        delay(elementSpace)
                    }
                    delay(charSpace - elementSpace) // Compensate for last element space

                    // O: - - -
                    for (i in 0 until 3) {
                        if (!isActive) break
                        toggleTorch(true)
                        delay(dash)
                        toggleTorch(false)
                        delay(elementSpace)
                    }
                    delay(charSpace - elementSpace)

                    // S: . . .
                    for (i in 0 until 3) {
                        if (!isActive) break
                        toggleTorch(true)
                        delay(dot)
                        toggleTorch(false)
                        delay(elementSpace)
                    }
                    
                    delay(wordSpace)
                }
            } finally {
                toggleTorch(false)
            }
        }
    }

    fun startMorse(text: String, onProgress: (char: Char, active: Boolean) -> Unit, onFinished: () -> Unit) {
        stopAll()
        val cleanedText = text.uppercase().trim()
        if (cleanedText.isEmpty()) {
            onFinished()
            return
        }

        job = scope.launch {
            try {
                val dot = 150L
                val dash = 450L
                val elementSpace = 150L
                val charSpace = 450L
                val wordSpace = 1050L

                for (char in cleanedText) {
                    if (!isActive) break
                    if (char == ' ') {
                        onProgress(' ', false)
                        delay(wordSpace)
                        continue
                    }

                    val morseCode = morseAlphabet[char]
                    if (morseCode == null) {
                        // Skip unknown chars but delay slightly
                        delay(charSpace)
                        continue
                    }

                    onProgress(char, true)
                    for (i in morseCode.indices) {
                        if (!isActive) break
                        val symbol = morseCode[i]
                        val duration = if (symbol == '.') dot else dash
                        toggleTorch(true)
                        delay(duration)
                        toggleTorch(false)
                        if (i < morseCode.length - 1) {
                            delay(elementSpace)
                        }
                    }
                    onProgress(char, false)
                    delay(charSpace)
                }
            } finally {
                toggleTorch(false)
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }

    fun startHeartbeat() {
        stopAll()
        job = scope.launch {
            try {
                while (isActive) {
                    toggleTorch(true)
                    delay(150L)
                    toggleTorch(false)
                    delay(150L)
                    toggleTorch(true)
                    delay(150L)
                    toggleTorch(false)
                    delay(800L)
                }
            } finally {
                toggleTorch(false)
            }
        }
    }

    fun startCustomPulse(speedHz: Float) {
        stopAll()
        if (speedHz <= 0f) return
        val periodMs = (1000 / speedHz).toLong()
        // Slow pulsing / breathing effect - toggle on and off at custom speed
        job = scope.launch {
            try {
                while (isActive) {
                    toggleTorch(true)
                    delay(periodMs / 2)
                    toggleTorch(false)
                    delay(periodMs - (periodMs / 2))
                }
            } finally {
                toggleTorch(false)
            }
        }
    }

    companion object {
        private val morseAlphabet = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
            'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
            '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
            '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.", '-' to "-....-",
            '(' to "-.--.", ')' to "-.--.-", '@' to ".--.-."
        )
    }
}
