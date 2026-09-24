package com.example.rhnaf.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.text.Normalizer

/** Extraction is a suggestion, never a write. Explicit category selects the target schema;
 * only exact normalized field labels and listed aliases are mapped. */
object OperationalDocumentReader {
    data class Result(val categoria: String, val campos: List<String>, val filas: List<Map<String, String>>, val advertencias: List<String>)
    private val categories = mapOf(
        "Inspeccion" to "fecha tipoInspeccion area inspector hallazgos riesgo accionesCorrectivas fechaCierre estado",
        "Incidente" to "fecha tipo severidad personaAfectada departamento parteCuerpo diasPerdidos descripcion causaRaiz accionesCorrectivas estado",
        "PermisoTrabajo" to "tipo solicitante autorizadoPor fechaInicio fechaFin area riesgosIdentificados eppRequerido estado",
        "EPP" to "fecha empleado tipoEpp talla proximaReposicion firma",
        "Capacitacion" to "fecha tema instructor asistentes vigenciaMeses proximaFecha estado",
        "Simulacro" to "fecha tipo participantes tiempoEvacuacion resultado observaciones estado",
        "Riesgos" to "area proceso riesgoIdentificado probabilidad severidad nivelRiesgo controles responsable estado",
        "Residuos" to "fecha residuo tipo cantidad unidad transportista destinoFinal numeroManifiesto estado",
        "Quimicos" to "nombre fabricante areaUso nivelRiesgo hojaSeguridadUrl estado ultimaRevision",
        "ExamenMedico" to "empleadoId nombreEmpleado fecha tipoExamen resultado observaciones proximaCita medico"
    ).mapValues { it.value.split(' ') }
    // Salud ocupacional solo se procesa bajo EHS_WRITE, como el resto de evidencias restringidas.
    private val aliases = mapOf(
        "tipoInspeccion" to listOf("tipo de inspeccion", "tipo inspeccion", "tipo"),
        "fechaCierre" to listOf("f cierre", "fecha de cierre"),
        "accionesCorrectivas" to listOf("acciones correctivas", "acciones"),
        "riesgoIdentificado" to listOf("riesgo identificado"),
        "nivelRiesgo" to listOf("nivel de riesgo"),
        "fechaInicio" to listOf("fecha inicio", "fecha de inicio"),
        "fechaFin" to listOf("fecha fin", "fecha de fin"),
        "tipoEpp" to listOf("tipo de epp"),
        "numeroManifiesto" to listOf("numero de manifiesto"),
        "areaUso" to listOf("area de uso"),
        "proximaFecha" to listOf("proxima fecha"),
        "proximaReposicion" to listOf("proxima reposicion"),
        "fecha" to listOf("fecha del documento")
    )
    fun supportedCategories(): Set<String> = categories.keys
    private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("[^a-z0-9]"), "")
    private fun column(raw: String, fields: List<String>): String? {
        val n = norm(raw)
        if (n.isBlank()) return null
        return fields.firstOrNull { field -> norm(field) == n || aliases[field]?.any { norm(it) == n } == true }
    }
    private fun table(rows: List<List<String>>, category: String): Result {
        val fields = categories.getValue(category)
        val results = mutableListOf<Map<String, String>>()
        var columns: List<String?> = emptyList()
        for (row in rows.take(1001)) {
            val mapping = row.map { column(it, fields) }
            if (mapping.count { it != null } >= 2) { columns = mapping; continue }
            if (columns.isEmpty()) continue
            val mapped = columns.mapIndexedNotNull { index, key ->
                key?.let { k -> row.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let { k to it.take(4000) } }
            }.toMap()
            if (mapped.isNotEmpty()) results += mapped
        }
        return Result(category, fields, results, listOfNotNull(
            if (columns.isEmpty()) "No se detectaron dos encabezados de esta sección; revisa el documento original." else null,
            if (rows.size > 1001) "Se limitó la revisión a 1,000 filas." else null,
            if (results.isNotEmpty()) "Datos sugeridos. Revisa campos, filas y posibles duplicados antes de guardarlos." else null
        ))
    }
    fun read(file: File, fileName: String, category: String): Result {
        val fields = categories[category] ?: return Result(category, emptyList(), emptyList(),
            listOf("Esta categoría no tiene un esquema operativo de extracción. No se crearon registros."))
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "xlsx", "xls" -> WorkbookFactory.create(file).use { book ->
                val formatter = DataFormatter()
                val rows = book.getSheetAt(0).map { row ->
                    (0 until row.lastCellNum.coerceAtLeast(0)).map { index ->
                        row.getCell(index)?.let { formatter.formatCellValue(it) } ?: ""
                    }
                }
                table(rows, category)
            }
            "docx" -> XWPFDocument(file.inputStream()).use { doc ->
                val rows = doc.tables.flatMap { t -> t.rows.map { r -> r.tableCells.map { it.text } } }
                table(rows, category)
            }
            "pdf" -> PDDocument.load(file).use { doc ->
                val text = PDFTextStripper().getText(doc).take(200_000)
                if (text.isBlank()) Result(category, fields, emptyList(), listOf("PDF escaneado o sin texto seleccionable. Requiere OCR; no se extrajo información."))
                else {
                    // PDF positional text does not preserve table boundaries reliably.
                    // Accept only explicit key: value lines as one candidate, not guessed columns.
                    val mapped = text.lines().mapNotNull { line ->
                        val m = Regex("^\\s*([^:]{2,60}):\\s*(.{1,4000})\\s*$").matchEntire(line)
                        m?.let { column(it.groupValues[1], fields)?.let { key -> key to it.groupValues[2].trim() } }
                    }.toMap()
                    Result(category, fields, if (mapped.isEmpty()) emptyList() else listOf(mapped),
                        listOf("PDF: solo se extrajeron etiquetas explícitas 'Campo: valor'. Confirma que los valores correspondan a esta sección."))
                }
            }
            else -> Result(category, fields, emptyList(), listOf("Formato no admitido. Usa PDF con texto, DOCX, XLS o XLSX."))
        }
    }
}
