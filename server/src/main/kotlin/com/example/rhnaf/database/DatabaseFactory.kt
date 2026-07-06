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
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/db"
        val database = Database.connect(createHikariDataSource(jdbcURL, driverClassName))
        
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(EmployeeTable, AttendanceLogTable)
            
            // Carga de Personal NAF CONNECT desde la lista proporcionada
            if (EmployeeTable.selectAll().empty()) {
                val personal = listOf(
                    listOf("114", "Canizales Julian Carlos", "CAJC861013HCLNLN02", "Almacenista", "24/01/2023"),
                    listOf("162", "Zhao Yun", "ZAKY811214HNENXN07", "Director", "19/07/2023"),
                    listOf("163", "Cruz Del Angel Gregorio", "CUAG030102HVZRNRA7", "Líder de producción", "03/08/2023"),
                    listOf("164", "Yang Longan", "YAXL651116HNENXN06", "Técnico Eléctrico", "07/08/2023"),
                    listOf("165", "Huynh Le Phuoc Thien", "HUXP940316HNEYXN05", "Técnico de Mantenimiento", "26/05/2023"),
                    listOf("166", "Do Thi Phuong", "DOXT840206MNEXN01", "Técnico de Calidad", "29/12/2025"),
                    listOf("167", "Nguyen Van Ngoc", "NUXN961126HNEGXG04", "Técnico de Mantenimiento", "10/08/2023"),
                    listOf("168", "Nguyen Thanh Hong", "NUXT890527HNEGXH00", "Técnico de Mantenimiento", "07/04/2025"),
                    listOf("171", "Canizales Hernandez Carlos Jose", "CAHC050805HNLNRRA3", "Operador General", "21/08/2023"),
                    listOf("184", "Cordova Amaro Flor Michel", "COAF960224MNLRML00", "Operador General", "14/09/2023"),
                    listOf("206", "Hernandez Moreno Miguel Angel", "HEMM851216HTSRRG09", "Operador General", "28/09/2023"),
                    listOf("220", "Hernandez Ciriaco Adelaida", "HECA770311MNGRRD01", "Operador General", "25/10/2023"),
                    listOf("221", "Diaz Oviedo Eduardo", "DIDE840124MHGZVD01", "Líder de producción", "25/10/2023"),
                    listOf("225", "Martinez Peralta Ismael", "MAPI890617HDCRRS03", "Operador General", "27/01/2026"),
                    listOf("272", "Pham Khac Nhu", "PAKH840619HNENXN03", "Operador General", "29/01/2026"),
                    listOf("274", "Wang Zhixiang", "WAZX911111HNENXH00", "Técnico de Mantenimiento", "14/12/2023"),
                    listOf("334", "Gregorio Aguilar Elidet", "GEAE930917MVZRGL04", "Inspector de Calidad", "08/02/2024"),
                    listOf("341", "Medrano Rodriguez Juan Francisco", "MERF780623HNLNDR02", "Operador General", "02/12/2024"),
                    listOf("343", "Alvarado Lara Josefina Marleny", "AALJ760406MNLLRS08", "Operador General", "10/02/2024"),
                    listOf("350", "Lopez Castillo Paola Yamileth", "LOCP000918MNLPLA02", "Operador General", "13/02/2024"),
                    listOf("355", "Morales Diaz Perla Joshelin", "MODP040727MTSPLA01", "Operador General", "22/02/2024"),
                    listOf("364", "Cen He", "CEHE811225HNENXH09", "Técnico de Mantenimiento", "21/02/2024"),
                    listOf("365", "Cao Yanyun", "CAYA761205HNENXX04", "Comprador", "21/02/2024"),
                    listOf("366", "Wang Jie", "WAXJ880104HNENXX06", "Técnico de Mantenimiento", "21/04/2025"),
                    listOf("378", "Vazquez Madrigal Salvador", "VAMS971022HVZLDL09", "Operador General", "04/03/2024"),
                    listOf("395", "Herrera Martinez Amanda Sorely", "HEMA050125MNLRRM00", "Operador General", "04/03/2024"),
                    listOf("417", "Corpus Salas Yarely Vanessa", "COSY050212MNLRRA00", "Operador General", "28/03/2024"),
                    listOf("447", "Moreno Moreno Benito", "MOMB920915MCRRND2", "Inspector de Calidad", "27/06/2024"),
                    listOf("450", "Gonzalez De La O Abraham", "GODA840222MGRNXB02", "Operador General", "18/07/2024"),
                    listOf("460", "Gonzalez Cobos Mariana", "GOCM940119MVZNBR05", "Operador General", "18/07/2024"),
                    listOf("466", "Arreola Emiliano Martin Ignacio", "AEEM911205MNLRMN06", "Operador General", "15/01/2026"),
                    listOf("10021", "Cortes Cavazos Andrea Georgina", "COCA940423MNLRVN08", "Auxiliar de Import-Export", "17/03/2026"),
                    listOf("10022", "Valenzuela Carrizales Jonathan Fernando", "VACJ011113HNLLRNAS", "Auxiliar de compras", "23/03/2026"),
                    listOf("10010", "Salazar Rios Arni Oziel", "SARA960801MNLLRS01", "Auxiliar de recursos humanos", "09/02/2026"),
                    listOf("10009", "Robles Trujillo Jesus Dario", "ROTJ920320HNLBRS04", "Coordinador de EHS", "29/07/2024")
                )

                personal.forEach { p ->
                    EmployeeTable.insert {
                        it[id] = p[0]
                        val names = p[1].split(" ")
                        it[firstName] = names.lastOrNull() ?: ""
                        it[lastName] = names.dropLast(1).joinToString(" ")
                        it[position] = p[3]
                        it[department] = "General"
                        it[entryDate] = p[4]
                        it[status] = EmployeeStatus.ACTIVE
                        it[readerId] = p[0] // Usamos el # Emp como ID de lectora
                    }
                }
            }
        }
    }

    private fun createHikariDataSource(
        url: String,
        driver: String
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
