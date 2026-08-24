package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object OrderTable : Table("shipping_orders") {
    val id = integer("id").autoIncrement()
    val numeroPedido = varchar("numero_pedido", 100)
    val cliente = varchar("cliente", 200)
    val fechaPedido = varchar("fecha_pedido", 50).default("")
    val fechaEntregaSolicitada = varchar("fecha_entrega_solicitada", 50).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val prioridad = varchar("prioridad", 50).default("Normal")
    val estado = varchar("estado", 50).default("Pendiente")
    val notas = varchar("notas", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

object DeliveryRouteTable : Table("shipping_delivery_routes") {
    val id = integer("id").autoIncrement()
    val nombreRuta = varchar("nombre_ruta", 200)
    val fechaSalida = varchar("fecha_salida", 50).default("")
    val fechaEntregaEstimada = varchar("fecha_entrega_estimada", 50).default("")
    val conductor = varchar("conductor", 200).default("")
    val vehiculo = varchar("vehiculo", 100).default("")
    val placa = varchar("placa", 100).default("")
    val paradas = varchar("paradas", 1000).default("")
    val pedidosAsociados = varchar("pedidos_asociados", 500).default("")
    val estado = varchar("estado", 50).default("Programada")
    val notas = varchar("notas", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

object TraceabilityEventTable : Table("shipping_traceability_events") {
    val id = integer("id").autoIncrement()
    val referencia = varchar("referencia", 200)
    val tipoReferencia = varchar("tipo_referencia", 50).default("Pedido")
    val evento = varchar("evento", 100).default("")
    val fecha = varchar("fecha", 50).default("")
    val ubicacion = varchar("ubicacion", 200).default("")
    val responsable = varchar("responsable", 200).default("")
    val notas = varchar("notas", 500).default("")

    override val primaryKey = PrimaryKey(id)
}
