package com.example.rhnaf.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import com.example.rhnaf.shared.model.EmployeeStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init() {
        // --------------------------------------------------------------
        // Persistencia real: si existe DATABASE_URL (Postgres, ej. Neon/
        // Supabase) la usamos — así los datos NO se pierden en cada
        // redeploy del contenedor en Hugging Face. Si no está configurada,
        // caemos a H2 en archivo local SOLO para desarrollo.
        //
        // Formato esperado de DATABASE_URL:
        //   postgres://usuario:password@host:5432/nombre_db
        // (el formato típico que dan Neon/Supabase/Render). Lo convertimos
        // al formato jdbc:postgresql:// que espera el driver.
        // --------------------------------------------------------------
        val rawDatabaseUrl = System.getenv("DATABASE_URL")

        val database = if (!rawDatabaseUrl.isNullOrBlank()) {
            val (jdbcUrl, user, password) = parsePostgresUrl(rawDatabaseUrl)
            println("[DatabaseFactory] Usando Postgres persistente en ${jdbcUrl.substringBefore("?")}")
            Database.connect(createHikariDataSource(jdbcUrl, "org.postgresql.Driver", user, password))
        } else {
            println("[DatabaseFactory] ADVERTENCIA: DATABASE_URL no configurada, usando H2 local (./build/db). Esto se BORRA en cada redeploy.")
            Database.connect(createHikariDataSource("jdbc:h2:file:./build/db", "org.h2.Driver"))
        }

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(EmployeeTable, AttendanceLogTable, IncidentTable, DebugLogTable)

            // LIMPIEZA Y CARGA DE PERSONAL COMPLETO (SEGÚN EXCEL)
            if (EmployeeTable.selectAll().empty()) {
                val fullStaff = listOf(
                    listOf("114", "Canizales Julian Carlos", "Almacenista", "24/01/2023"),
                    listOf("163", "Cruz Del Angel Gregorio", "Líder de producción", "03/08/2023"),
                    listOf("171", "Canizales Hernandez Carlos Jose", "Operador General", "21/08/2023"),
                    listOf("184", "Cordova Amaro Flor Michel", "Operador General", "14/09/2023"),
                    listOf("206", "Hernandez Moreno Miguel Angel", "Operador General", "28/09/2023"),
                    listOf("220", "Hernandez Ciriaco Adelaida", "Operador General", "25/10/2023"),
                    listOf("221", "Diaz Oviedo Eduardo", "Líder de producción", "25/10/2023"),
                    listOf("334", "Gregorio Aguilar Elidet", "Inspector de Calidad", "08/02/2024"),
                    listOf("341", "Medrano Rodriguez Juan Francisco", "Operador General", "02/12/2024"),
                    listOf("343", "Alvarado Lara Josefina Marleny", "Operador General", "10/02/2024"),
                    listOf("350", "Lopez Castillo Paola Yamileth", "Operador General", "13/02/2024"),
                    listOf("355", "Morales Diaz Perla Joshelin", "Operador General", "22/02/2024"),
                    listOf("378", "Vazquez Madrigal Salvador", "Operador General", "04/03/2024"),
                    listOf("395", "Herrera Martinez Amanda Sorely", "Operador General", "04/03/2024"),
                    listOf("417", "Corpus Salas Yarely Vanessa", "Operador General", "28/03/2024"),
                    listOf("447", "Moreno Moreno Benito", "Inspector de Calidad", "27/06/2024"),
                    listOf("450", "Gonzalez De La O Abraham", "Operador General", "18/07/2024"),
                    listOf("460", "Gonzalez Cobos Mariana", "Operador General", "18/07/2024"),
                    listOf("472", "Hernandez Hernandez Amadita", "Operador General", "12/07/2024"),
                    listOf("475", "Marin Ramirez Miguel Angel", "Operador General", "13/11/2023"),
                    listOf("478", "Lopez Mascareñas Senui Arani", "Operador General", "21/08/2024"),
                    listOf("479", "Barragan Hernandez Gabriela Guadalupe", "Operador General", "28/08/2024"),
                    listOf("490", "Hernandez Borjon Gema Citlaly", "Operador General", "12/02/2026"),
                    listOf("498", "Antonio Gregorio Oliveth", "Operador General", "03/04/2025"),
                    listOf("987", "Sauceda Llanas Cynthia Esmeralda", "Almacenista", "11/06/2024"),
                    listOf("988", "Ramirez Aguirre Yahir", "Montacarguista", "08/01/2026"),
                    listOf("997", "HERNANDEZ ANTONIO MARIO", "Operador General", "13/01/2026"),
                    listOf("1007", "ZAPATA LOPEZ EVELIN ODALYS", "Operador General", "29/01/2026"),
                    listOf("1009", "HERNANDEZ SERVANTES CRISTINA", "Operador General", "03/02/2026"),
                    listOf("1011", "AGUIRRE HERNANDEZ ARACELI", "Operador General", "03/02/2026"),
                    listOf("1023", "Garcia Guerra Alfredo", "Operador General", "19/02/2026"),
                    listOf("1029", "Palomo Castillo Lucero Berenice", "Operador General", "03/02/2026"),
                    listOf("1032", "Santiago Domingo Anastacio", "Operador General", "04/03/2026"),
                    listOf("1042", "Garcia Serrato Ashly Aily", "Operador General", "04/03/2026"),
                    listOf("1044", "Martinez Garcia Yesenia", "Operador General", "22/02/2026"),
                    listOf("1045", "Armenta Resendiz Mauricio", "Operador General", "04/03/2026"),
                    listOf("1047", "Duarte Orozco Miguel Angel", "Inspector de Calidad", "04/03/2026"),
                    listOf("1048", "Monroy Palacios Diana Guadalupe", "Operador General", "04/03/2026"),
                    listOf("1049", "del Angel Reyna Elizabeth", "Operador General", "04/03/2026"),
                    listOf("1050", "Sanchez Rivero Janett", "Montacarguista", "07/03/2026"),
                    listOf("1051", "Genaro Fernandez Priscilla", "Operador General", "12/03/2026"),
                    listOf("1054", "Torres Barbosa Karla Yadira", "Operador General", "12/03/2026"),
                    listOf("1055", "Reyna Hernandez Brenda Alejandra", "Operador General", "12/03/2026"),
                    listOf("1057", "Diaz Alarcon David", "Operador General", "12/03/2026"),
                    listOf("1060", "Hernandez Hernandez Sandra Concepcion", "Operador General", "19/03/2026"),
                    listOf("1064", "Suarez Romero Roberto Jose", "Operador General", "26/03/2026"),
                    listOf("1067", "Guemes Garcia Fabiola", "Operador General", "25/03/2026"),
                    listOf("1073", "Rodriguez Lopez Maria Guadalupe", "Operador General", "14/04/2026"),
                    listOf("1074", "Huerta Mendoza Jose Ramiro", "Operador General", "15/04/2026"),
                    listOf("1077", "De Leon Herrera Margarita", "Operador General", "13/04/2026"),
                    listOf("1082", "DUARTE MONRROY YANETH ALEXANDRA", "Operador General", "23/04/2026"),
                    listOf("1085", "Acosta Juarez Erik Gamaliel", "Operador General", "23/04/2026"),
                    listOf("1087", "Lopez Gonzalez Josue Efrain", "Operador General", "27/04/2026"),
                    listOf("1088", "Hernandez Martinez Vanessa", "Operador General", "27/04/2026"),
                    listOf("1089", "Adolfo Angel Castillo Martinez", "Operador General", "04/05/2026"),
                    listOf("1090", "LUZ MARIA ORTEGA RUIZ", "Operador General", "04/05/2026"),
                    listOf("1091", "JESUS ALBERTO LOPEZ MASCAREÑAS", "Montacarguista", "08/05/2026"),
                    listOf("1093", "ELDINA DROUAILLET ALEJANDRO", "Operador General", "08/05/2026"),
                    listOf("1094", "HEIDY NAOMY ROMAN CAMPOS", "Operador General", "08/05/2026"),
                    listOf("1095", "VICTOR ALEXANDRO HERNANDEZ", "Operador General", "08/05/2026"),
                    listOf("1096", "CINTIA CAROLINA SALDAÑA PANTOJA", "Operador General", "13/05/2026"),
                    listOf("162", "Zhao Yun", "Director", "19/07/2023"),
                    listOf("164", "Yang Longan", "Técnico Eléctrico", "07/08/2023"),
                    listOf("165", "Huynh Le Phuoc Thien", "Técnico de Mantenimiento", "26/05/2025"),
                    listOf("166", "Do Thi Phuong", "Técnico de Calidad", "29/12/2025"),
                    listOf("167", "Nguyen Van Ngoc", "Técnico de Mantenimiento", "10/08/2023"),
                    listOf("168", "Nguyen Thanh Hong", "Técnico de Mantenimiento", "07/04/2025"),
                    listOf("272", "Pham Khac Nhu", "Operador General", "29/12/2025"),
                    listOf("274", "Wang Zhixiang", "Técnico de Mantenimiento", "14/12/2023"),
                    listOf("364", "Cen He", "Técnico de Mantenimiento", "21/02/2024"),
                    listOf("365", "Cao Yanyun", "Comprador", "21/02/2024"),
                    listOf("366", "Wang Jie", "Técnico de Mantenimiento", "21/04/2025"),
                    listOf("502", "Wu Yurong", "Técnico de Mantenimiento", "19/05/2025"),
                    listOf("503", "Zhao Jinsong", "Líder", "26/05/2025"),
                    listOf("504", "Pan Lihua", "Logística", "22/12/2025"),
                    listOf("506", "Dong Binbin", "Técnico de Mantenimiento", "22/12/2025"),
                    listOf("508", "Tran Thi Huyen", "Técnico de Calidad", "21/07/2025"),
                    listOf("510", "Zhu Ping", "Técnico de Mantenimiento", "11/09/2025"),
                    listOf("10009", "Robles Trujillo Jesus Dario", "Coordinador de EHS", "29/07/2024")
                )

                fullStaff.forEach { s ->
                    EmployeeTable.insert {
                        it[id] = s[0]
                        val names = s[1].split(" ")
                        it[firstName] = names.lastOrNull() ?: ""
                        it[lastName] = names.dropLast(1).joinToString(" ")
                        it[position] = s[2]
                        it[department] = "General"
                        it[entryDate] = s[3]
                        it[status] = EmployeeStatus.ACTIVE
                        it[readerId] = s[0]
                    }
                }
            }
        }
    }

    // Convierte una URL estilo postgres://user:pass@host:port/dbname
    // (formato de Neon/Supabase/Render) al formato jdbc:postgresql://... que
    // espera el driver, separando usuario y password.
    private fun parsePostgresUrl(raw: String): Triple<String, String, String> {
        val cleaned = raw.removePrefix("postgres://").removePrefix("postgresql://")
        val atParts = cleaned.split("@", limit = 2)
        require(atParts.size == 2) {
            "DATABASE_URL invalida: se esperaba el formato completo " +
            "postgresql://usuario:password@host:puerto/basededatos, pero se recibio: " +
            "'${raw.take(15)}...' (posiblemente solo se guardo la password, sin el resto de la cadena)"
        }
        val (credentials, hostPart) = atParts
        val credParts = credentials.split(":", limit = 2)
        require(credParts.size == 2) {
            "DATABASE_URL invalida: falta 'usuario:password' antes del @"
        }
        val (user, password) = credParts
        // hostPart puede traer ?sslmode=require etc, lo dejamos pasar tal cual
        val jdbcUrl = "jdbc:postgresql://$hostPart" + if (!hostPart.contains("?")) "?sslmode=require" else ""
        return Triple(jdbcUrl, user, password)
    }

    private fun createHikariDataSource(
        url: String,
        driver: String,
        user: String? = null,
        password: String? = null
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        if (user != null) username = user
        if (password != null) this.password = password
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
