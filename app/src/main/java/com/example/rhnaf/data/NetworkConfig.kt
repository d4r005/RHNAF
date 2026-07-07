package com.example.rhnaf.data

/**
 * URL pública real del servidor RHNAF desplegado en Hugging Face Spaces.
 *
 * Antes esto apuntaba a "http://10.0.2.2:8080", que es la IP especial que
 * usa el EMULADOR de Android para hablar con el localhost de tu PC —
 * nunca funciona en un celular real ni contra el servidor en la nube.
 */
object NetworkConfig {
    const val BASE_URL = "https://d4r005-rhnaf-industrial.hf.space"
}
