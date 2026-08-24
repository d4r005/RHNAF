package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object WarehouseInventoryTable : Table("warehouse_inventory") {
    val id = integer("id").autoIncrement()
    val lugar = varchar("lugar", 100)
    val po = varchar("po", 200).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val falta = varchar("falta", 200).default("")
    val existencia = varchar("existencia", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

object WarehouseIncomingLogTable : Table("warehouse_incoming_log") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val po = varchar("po", 200).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val ubicacion = varchar("ubicacion", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

object ShipmentTable : Table("shipments") {
    val id = integer("id").autoIncrement()
    val cliente = varchar("cliente", 50)
    val fechaCarga = varchar("fecha_carga", 50).default("")
    val poContenedor = varchar("po_contenedor", 200).default("")
    val sku = varchar("sku", 200).default("")
    val nombreProducto = varchar("nombre_producto", 300).default("")
    val numeroSello = varchar("numero_sello", 100).default("")
    val placa = varchar("placa", 100).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val gabinetes = varchar("gabinetes", 100).default("")
    val conductor = varchar("conductor", 200).default("")
    val horaInicio = varchar("hora_inicio", 50).default("")
    val horaFin = varchar("hora_fin", 50).default("")
    val operador = varchar("operador", 100).default("")
    val inspector = varchar("inspector", 100).default("")

    override val primaryKey = PrimaryKey(id)
}

object ShipmentSummaryTable : Table("shipment_summary") {
    val id = integer("id").autoIncrement()
    val cliente = varchar("cliente", 50)
    val po = varchar("po", 200).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val fecha = varchar("fecha", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

object WarehouseLocationTable : Table("warehouse_location") {
    val id = integer("id").autoIncrement()
    val codigo = varchar("codigo", 100)
    val zona = varchar("zona", 100).default("")
    val tipo = varchar("tipo", 100).default("")
    val capacidad = varchar("capacidad", 100).default("")
    val ocupacion = varchar("ocupacion", 100).default("")
    val estado = varchar("estado", 50).default("Activa")
    val notas = varchar("notas", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

object WarehouseOutgoingLogTable : Table("warehouse_outgoing_log") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val po = varchar("po", 200).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val ubicacion = varchar("ubicacion", 200).default("")
    val motivo = varchar("motivo", 200).default("")
    val responsable = varchar("responsable", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

object WarehouseAuditTable : Table("warehouse_audit") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val ubicacion = varchar("ubicacion", 200).default("")
    val modelo = varchar("modelo", 300).default("")
    val cantidadSistema = varchar("cantidad_sistema", 100).default("")
    val cantidadFisica = varchar("cantidad_fisica", 100).default("")
    val diferencia = varchar("diferencia", 100).default("")
    val responsable = varchar("responsable", 200).default("")
    val observaciones = varchar("observaciones", 500).default("")
    val estado = varchar("estado", 50).default("Pendiente")

    override val primaryKey = PrimaryKey(id)
}
