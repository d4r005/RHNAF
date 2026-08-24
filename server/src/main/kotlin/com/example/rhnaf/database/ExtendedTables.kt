package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

// ============ FERRETERIA ============
object FerreteriaTable : Table("ferreteria") {
    val id = integer("id").autoIncrement()
    val descripcion = varchar("descripcion", 500)
    val categoria = varchar("categoria", 200).default("")
    val stock = varchar("stock", 100).default("")
    val unidad = varchar("unidad", 50).default("pza")
    val ubicacion = varchar("ubicacion", 200).default("")
    val notas = varchar("notas", 500).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ RECEPCION DE MATERIAS PRIMAS ============
object RecepcionMPTable : Table("recepcion_mp") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val tipo = varchar("tipo", 50).default("")      // CALCIO, PVC, CARTON
    val descripcion = varchar("descripcion", 300).default("")
    val proveedor = varchar("proveedor", 200).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val unidad = varchar("unidad", 50).default("")
    val folio = varchar("folio", 100).default("")
    val notas = varchar("notas", 500).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ TARIMAS ============
object TarimaTable : Table("tarimas") {
    val id = integer("id").autoIncrement()
    val compania = varchar("compania", 100).default("")
    val fechaLlegada = varchar("fecha_llegada", 50).default("")
    val folio = varchar("folio", 100).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val medidas = varchar("medidas", 200).default("")
    val rechazadas = varchar("rechazadas", 100).default("")
    val cantidadAceptable = varchar("cantidad_aceptable", 100).default("")
    val fechaRegreso = varchar("fecha_regreso", 50).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ CONTENEDORES DE CHINA ============
object ContenedorChinaTable : Table("contenedor_china") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val codigoProducto = varchar("codigo_producto", 100).default("")
    val nombre = varchar("nombre", 300).default("")
    val especificacion = varchar("especificacion", 300).default("")
    val modelo = varchar("modelo", 200).default("")
    val cantidad = varchar("cantidad", 100).default("")
    val unidad = varchar("unidad", 50).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ SELLOS EN STOCK ============
object SelloStockTable : Table("sello_stock") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val numeroInicial = varchar("numero_inicial", 100).default("")
    val numeroFinal = varchar("numero_final", 100).default("")
    val cantidad = varchar("cantidad", 100).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ BITACORA DE GAS ============
object GasConsumoTable : Table("gas_consumo") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50).default("")
    val cantidadTexto = varchar("cantidad_texto", 100).default("")
    val numeroTanques = integer("numero_tanques").default(0)
    val litrosPorTanque = integer("litros_por_tanque").default(1000)
    val litrosTotales = integer("litros_totales").default(0)
    val diaCarga = varchar("dia_carga", 50).default("")
    override val primaryKey = PrimaryKey(id)
}

// ============ PERSONAL - TALLAS ============
object PersonalTallaTable : Table("personal_talla") {
    val id = integer("id").autoIncrement()
    val numero = varchar("numero", 50).default("")
    val nombre = varchar("nombre", 200).default("")
    val tallaPlayera = varchar("talla_playera", 50).default("")
    val tallaZapatos = varchar("talla_zapatos", 50).default("")
    val notas = varchar("notas", 300).default("")
    override val primaryKey = PrimaryKey(id)
}
