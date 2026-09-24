package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsActionTable
import com.example.rhnaf.database.EhsChecklistItemTable
import com.example.rhnaf.database.EhsChecklistTable
import com.example.rhnaf.database.EhsDocumentTable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.update

/**
 * Mantenimiento ADMIN de un solo uso: al borrar la base Supabase anterior las
 * secuencias de autoincremento de las tablas EHS nuevas (evidencia, checklists,
 * planes de acción) no se reiniciaron, así que el primer registro real quedó
 * con un ID heredado (evidencia documental #717) y quedaron dos acciones de
 * prueba generadas al verificar este despliegue. Compacta IDs y reinicia
 * secuencias SOLO en esas tablas nuevas; no toca nómina, almacén, usuarios ni
 * el resto del sistema. Es idempotente: si se vuelve a ejecutar sin filas que
 * mover, no hace nada dañino (solo reafirma la secuencia siguiente).
 */
fun Route.ehsIdMaintenanceRouting() {
    post("/api/v1/ehs/admin/normalizar-ids") {
        requireRoleOr403(call, setOf(Roles.ADMIN)) ?: return@post
        val reporte = mutableMapOf<String, String>()

        DatabaseFactory.dbQuery {
            // 1) Acciones de prueba generadas durante la verificación de checklists
            //    (título literal "Hallazgo de auditoría: PRUEBA..."); nunca son datos reales.
            val borradas = EhsActionTable.deleteWhere {
                EhsActionTable.titulo eq "Hallazgo de auditoría: PRUEBA salida de emergencia obstruida"
            }
            reporte["acciones_prueba_eliminadas"] = borradas.toString()

            // 2) Compactar IDs de evidencia documental (única tabla con datos reales)
            //    en dos fases para no chocar con IDs ya existentes.
            val docs = EhsDocumentTable.selectAll()
                .orderBy(EhsDocumentTable.id to SortOrder.ASC)
                .map { it[EhsDocumentTable.id] }
            docs.forEachIndexed { idx, oldId ->
                val newId = idx + 1
                if (newId != oldId) EhsDocumentTable.update({ EhsDocumentTable.id eq oldId }) { it[EhsDocumentTable.id] = newId + 1_000_000 }
            }
            docs.forEachIndexed { idx, oldId ->
                val newId = idx + 1
                if (newId != oldId) EhsDocumentTable.update({ EhsDocumentTable.id eq (newId + 1_000_000) }) { it[EhsDocumentTable.id] = newId }
            }
            reporte["documentos_renumerados"] = docs.size.toString()

            fun reiniciarSecuencia(tabla: String, siguiente: Int) {
                TransactionManager.current().exec("SELECT setval(pg_get_serial_sequence('$tabla', 'id'), $siguiente, false)")
            }
            reiniciarSecuencia("ehs_documents", docs.size + 1)
            reiniciarSecuencia("ehs_action_plans", 1)
            reiniciarSecuencia("ehs_checklists", 1)
            reiniciarSecuencia("ehs_checklist_items", 1)
        }

        call.respond(mapOf("status" to "ok", "detalle" to reporte))
    }
}
