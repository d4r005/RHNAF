package com.example.rhnaf.database

/** Inicializa solamente el esquema destino antes del cambio de servidor. */
fun main() {
    require(!System.getenv("DATABASE_URL").isNullOrBlank()) { "DATABASE_URL es obligatorio" }
    require(System.getenv("OMIT_EHS_DOCUMENTS") == "true") { "No se permite crear ehs_documents en la migracion" }
    DatabaseFactory.init()
}
