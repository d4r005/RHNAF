package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============================================================
// QIS - Quality Information System
// Control de documentos del sistema de gestión (ISO 9001):
// alta, versionado, vigencias y registro de cambios.
// ============================================================

@Serializable
data class QisDocument(
    val id: Int = 0,
    val codigo: String = "",            // PRO-001, INS-014, FOR-002...
    val titulo: String = "",
    val tipo: String = "Procedimiento", // Procedimiento, Instructivo, Formato, Politica, Manual, Plan, Registro, Otro
    val proceso: String = "",           // área/proceso dueño del documento
    val versionActual: String = "1.0",
    val fechaEmision: String = "",      // dd/MM/yyyy
    val proximaRevision: String = "",   // dd/MM/yyyy (opcional)
    val estado: String = "Vigente",     // Vigente, EnRevision, Obsoleto
    val elaboradoPor: String = "",
    val aprobadoPor: String = "",
    val urlArchivo: String = "",        // PDF controlado en Drive
    val notas: String = ""
)

// Alta de un documento controlado. La versión siempre nace en 1.0 y el
// primer registro del historial ("Alta del documento") se genera solo.
@Serializable
data class QisDocumentRequest(
    val codigo: String = "",
    val titulo: String = "",
    val tipo: String = "Procedimiento",
    val proceso: String = "",
    val fechaEmision: String = "",
    val proximaRevision: String = "",
    val elaboradoPor: String = "",
    val aprobadoPor: String = "",
    val urlArchivo: String = "",
    val notas: String = ""
)

// Registro de un cambio de versión sobre un documento existente:
// actualiza la versión vigente, guarda el historial y opcionalmente
// reemplaza la URL del archivo controlado.
@Serializable
data class QisChangeRequest(
    val versionNueva: String = "",
    val fechaCambio: String = "",
    val descripcion: String = "",
    val cambiadoPor: String = "",
    val aprobadoPor: String = "",
    val nuevaUrlArchivo: String = ""
)

@Serializable
data class QisChangeRecord(
    val id: Int = 0,
    val documentoId: Int = 0,
    val versionAnterior: String = "",
    val versionNueva: String = "",
    val fechaCambio: String = "",
    val descripcion: String = "",
    val cambiadoPor: String = "",
    val aprobadoPor: String = ""
)
