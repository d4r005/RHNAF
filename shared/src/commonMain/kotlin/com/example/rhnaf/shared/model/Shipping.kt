package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============================================================
// Modulo EMBARQUES: pedidos, rutas de entrega y trazabilidad
// ============================================================

// Pedido de cliente (previo al armado/carga del envío)
@Serializable
data class Order(
    val id: Int = 0,
    val numeroPedido: String,
    val cliente: String,
    val fechaPedido: String = "",
    val fechaEntregaSolicitada: String = "",
    val modelo: String = "",
    val cantidad: String = "",
    val prioridad: String = "Normal", // Normal, Urgente
    val estado: String = "Pendiente", // Pendiente, En preparacion, Listo para embarque, Embarcado, Entregado, Cancelado
    val notas: String = ""
)

// Ruta de entrega: agrupa uno o varios pedidos/envíos que salen juntos con un conductor/vehículo
@Serializable
data class DeliveryRoute(
    val id: Int = 0,
    val nombreRuta: String,
    val fechaSalida: String = "",
    val fechaEntregaEstimada: String = "",
    val conductor: String = "",
    val vehiculo: String = "",
    val placa: String = "",
    val paradas: String = "", // lista de destinos separados por coma
    val pedidosAsociados: String = "", // numeros de pedido separados por coma
    val estado: String = "Programada", // Programada, En ruta, Completada, Retrasada, Cancelada
    val notas: String = ""
)

// Evento de trazabilidad: bitácora cronológica de un pedido/envío desde que se crea hasta que se entrega
@Serializable
data class TraceabilityEvent(
    val id: Int = 0,
    val referencia: String, // numero de pedido o de envio/contenedor
    val tipoReferencia: String = "Pedido", // Pedido, Envio
    val evento: String = "", // Creado, En preparacion, Cargado, En ruta, Entregado, Incidencia
    val fecha: String = "",
    val ubicacion: String = "",
    val responsable: String = "",
    val notas: String = ""
)
