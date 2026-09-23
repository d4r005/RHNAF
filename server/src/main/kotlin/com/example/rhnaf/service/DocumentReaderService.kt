package com.example.rhnaf.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat

/**
 * Lector de documentos genérico: extrae texto o filas tabulares de PDF, Word
 * (.docx) y Excel (.xlsx/.xls). Se usa para poblar en lote formularios de alta
 * en formularios que lo integren (actualmente Inspecciones) a partir de un
 * documento subido por el usuario. La extracción es heurística por diseño: el
 * resultado se devuelve como vista previa y nunca se guarda automáticamente.
 */
object DocumentReaderService {

    class UnsupportedDocumentException(message: String) : IllegalArgumentException(message)

    private fun extension(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase().trim()

    /** Texto plano completo del documento (PDF o Word), con saltos de línea. */
    fun extractText(bytes: ByteArray, fileName: String): String {
        return when (extension(fileName)) {
            "pdf" -> ByteArrayInputStream(bytes).use { input ->
                PDDocument.load(input).use { doc ->
                    PDFTextStripper().apply { sortByPosition = true }.getText(doc)
                }
            }
            "docx" -> ByteArrayInputStream(bytes).use { input ->
                XWPFDocument(input).use { doc ->
                    val sb = StringBuilder()
                    doc.paragraphs.forEach { p -> if (p.text.isNotBlank()) sb.appendLine(p.text) }
                    doc.tables.forEach { table ->
                        table.rows.forEach { row ->
                            sb.appendLine(row.tableCells.joinToString("\t") { it.text.trim() })
                        }
                    }
                    sb.toString()
                }
            }
            "doc" -> throw UnsupportedDocumentException(
                "El formato .doc (Word 97-2003) no está soportado; guarda el archivo como .docx"
            )
            else -> throw UnsupportedDocumentException("Formato no soportado para lectura de texto: .${extension(fileName)}")
        }
    }

    /**
     * Filas tabulares del documento: para Excel son filas reales de la primera
     * hoja; para PDF/Word es un intento razonable de reconstruir columnas a
     * partir del texto (separadas por tabulaciones, 2+ espacios o "|").
     */
    fun extractRows(bytes: ByteArray, fileName: String): List<List<String>> {
        val ext = extension(fileName)
        return when (ext) {
            "xlsx", "xls" -> extractSpreadsheetRows(bytes)
            "pdf", "docx" -> extractTextAsRows(extractText(bytes, fileName))
            "doc" -> throw UnsupportedDocumentException(
                "El formato .doc (Word 97-2003) no está soportado; guarda el archivo como .docx"
            )
            else -> throw UnsupportedDocumentException("Formato no soportado: .$ext (usa PDF, Word .docx o Excel .xlsx/.xls)")
        }
    }

    private fun extractSpreadsheetRows(bytes: ByteArray): List<List<String>> {
        ByteArrayInputStream(bytes).use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val rows = mutableListOf<List<String>>()
                for (row in sheet) {
                    val lastCol = row.lastCellNum.toInt()
                    if (lastCol < 0) continue
                    val cells = (0 until lastCol).map { c -> cellText(row.getCell(c)) }
                    if (cells.any { it.isNotBlank() }) rows.add(cells)
                }
                return rows
            }
        }
    }

    private fun cellText(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat("dd/MM/yyyy").format(cell.dateCellValue)
                } else {
                    val v = cell.numericCellValue
                    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> runCatching { cell.stringCellValue.trim() }.getOrElse {
                runCatching { cell.numericCellValue.toString() }.getOrElse { "" }
            }
            else -> ""
        }.trim()
    }

    // Separa cada línea de texto en "columnas" usando tabs, 2+ espacios o "|".
    // No es perfecto para PDFs sin estructura de tabla real, pero cubre el caso
    // común de formatos con encabezados alineados (Fecha, Tipo, Área, ...).
    private fun extractTextAsRows(text: String): List<List<String>> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                line.split(Regex("\\t+|\\s{2,}|\\s*\\|\\s*"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
            .filter { it.isNotEmpty() }
    }
}
