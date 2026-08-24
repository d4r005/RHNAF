package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============ FERRETERIA ============
@Serializable
data class FerreteriaItem(
    val id: Int = 0,
    val descripcion: String,
    val categoria: String = "",
    val stock: String = "",
    val unidad: String = "pza",
    val ubicacion: String = "",
    val notas: String = ""
)

// ============ RECEPCION DE MATERIAS PRIMAS (CALCIO, PVC, CARTON) ============
@Serializable
data class RecepcionMP(
    val id: Int = 0,
    val fecha: String = "",
    val tipo: String = "",        // CALCIO, PVC, CARTON
    val descripcion: String = "",
    val proveedor: String = "",
    val cantidad: String = "",
    val unidad: String = "",      // kg, pza, m2
    val folio: String = "",
    val notas: String = ""
)

// ============ TARIMAS / PALLETS ============
@Serializable
data class Tarima(
    val id: Int = 0,
    val compania: String = "",
    val fechaLlegada: String = "",
    val folio: String = "",
    val cantidad: String = "",
    val medidas: String = "",
    val rechazadas: String = "",
    val cantidadAceptable: String = "",
    val fechaRegreso: String = ""
)

// ============ CONTENEDORES DE CHINA ============
@Serializable
data class ContenedorChina(
    val id: Int = 0,
    val fecha: String = "",
    val codigoProducto: String = "",
    val nombre: String = "",
    val especificacion: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val unidad: String = ""        // kg, pza, m2
)

// ============ SELLOS EN STOCK ============
@Serializable
data class SelloStock(
    val id: Int = 0,
    val fecha: String = "",
    val numeroInicial: String = "",
    val numeroFinal: String = "",
    val cantidad: String = ""
)

// ============ BITACORA DE GAS ============
@Serializable
data class GasConsumo(
    val id: Int = 0,
    val fecha: String = "",
    val cantidadTexto: String = "",   // "13 TANQUES" (raw)
    val numeroTanques: Int = 0,       // parsed
    val litrosPorTanque: Int = 1000,  // configurable, default 1000L
    val litrosTotales: Int = 0,       // calculated
    val diaCarga: String = ""
)

// ============ HUELLA DE CARBONO (EHS) ============
@Serializable
data class CarbonFootprintSummary(
    val totalRegistros: Int = 0,
    val totalTanquesAnual: Int = 0,
    val totalLitrosAnual: Int = 0,
    val co2AnualKg: Double = 0.0,     // kg CO2
    val co2AnualTon: Double = 0.0,    // toneladas CO2
    val litrosPorTanque: Int = 1000,
    val factorEmision: Double = 1.51, // kg CO2 por litro de LPG
    val meses: List<MonthlyGasData> = emptyList()
)

@Serializable
data class MonthlyGasData(
    val mes: String,                  // "2024-05"
    val tanques: Int = 0,
    val litros: Int = 0,
    val co2Kg: Double = 0.0
)

// ============ PERSONAL - TALLAS (RRHH) ============
@Serializable
data class PersonalTalla(
    val id: Int = 0,
    val numero: String = "",
    val nombre: String = "",
    val tallaPlayera: String = "",
    val tallaZapatos: String = "",
    val notas: String = ""
)
