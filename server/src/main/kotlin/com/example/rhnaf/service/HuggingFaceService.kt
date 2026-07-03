package com.example.rhnaf.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class HuggingFaceService(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun analyzeIncident(description: String): String {
        return try {
            val response: HttpResponse = client.post("https://api-inference.huggingface.co/models/facebook/bart-large-cnn") {
                header("Authorization", "Bearer $apiKey")
                setBody(buildJsonObject {
                    put("inputs", "Resume and classify this industrial incident: $description")
                })
            }
            response.bodyAsText()
        } catch (e: Exception) {
            "Error calling HF (Analysis): ${e.message}"
        }
    }

    suspend fun extractTextFromImage(imageBytes: ByteArray): String {
        return try {
            val response: HttpResponse = client.post("https://api-inference.huggingface.co/models/microsoft/trocr-base-printed") {
                header("Authorization", "Bearer $apiKey")
                setBody(imageBytes)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            "Error calling HF (OCR): ${e.message}"
        }
    }
}
