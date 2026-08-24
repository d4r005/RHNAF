package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// Inventario actual de producto terminado por ubicación en almacén
@Serializable
data class WarehouseInventoryItem(
    val id: Int = 0,
    val lugar: String,
    val po: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val falta: String = "",
    val existencia: String = ""
)

// Bitácora de entradas de producto (con fecha) al almacén
@Serializable
data class WarehouseIncomingLog(
    val id: Int = 0,
    val fecha: String = "",
    val po: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val ubicacion: String = ""
)

// Registro detallado de un envío que salió de planta (por cliente/línea: FD, SF, AJ, EVF, RBT)
@Serializable
data class Shipment(
    val id: Int = 0,
    val cliente: String,
    val fechaCarga: String = "",
    val poContenedor: String = "",
    val sku: String = "",
    val nombreProducto: String = "",
    val numeroSello: String = "",
    val placa: String = "",
    val cantidad: String = "",
    val gabinetes: String = "",
    val conductor: String = "",
    val horaInicio: String = "",
    val horaFin: String = "",
    val operador: String = "",
    val inspector: String = ""
)

// Resumen rápido de lo que salió de planta (hoja "producto que salio de planta")
@Serializable
data class ShipmentSummary(
    val id: Int = 0,
    val cliente: String,
    val po: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val fecha: String = ""
)

// Ubicación física dentro del almacén (racks, zonas, andenes)
@Serializable
data class WarehouseLocation(
    val id: Int = 0,
    val codigo: String,
    val zona: String = "",
    val tipo: String = "", // Rack, Piso, Anden, Cuarentena
    val capacidad: String = "",
    val ocupacion: String = "",
    val estado: String = "Activa",
    val notas: String = ""
)

// Bitácora de salidas de producto del almacén (consumo interno, merma, devolución - no envío a cliente)
@Serializable
data class WarehouseOutgoingLog(
    val id: Int = 0,
    val fecha: String = "",
    val po: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val ubicacion: String = "",
    val motivo: String = "", // Merma, Consumo interno, Devolucion a proveedor, Ajuste
    val responsable: String = ""
)

// Auditoría de inventario: comparación de cantidad en sistema vs. cantidad física contada
@Serializable
data class WarehouseAudit(
    val id: Int = 0,
    val fecha: String = "",
    val ubicacion: String = "",
    val modelo: String = "",
    val cantidadSistema: String = "",
    val cantidadFisica: String = "",
    val diferencia: String = "",
    val responsable: String = "",
    val observaciones: String = "",
    val estado: String = "Pendiente" // Pendiente, Conciliado, Con diferencia
)
