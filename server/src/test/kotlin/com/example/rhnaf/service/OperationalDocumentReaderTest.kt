package com.example.rhnaf.service

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationalDocumentReaderTest {
    @Test fun categorySpecificExcelDoesNotMisrouteColumns() {
        val file = File.createTempFile("operations-", ".xlsx")
        try {
            XSSFWorkbook().use { workbook ->
                val sheet = workbook.createSheet()
                val header = sheet.createRow(0)
                listOf("Fecha", "Tipo", "Riesgo", "Area", "Hallazgos").forEachIndexed { i, v -> header.createCell(i).setCellValue(v) }
                val row = sheet.createRow(1)
                listOf("20/09/2026", "Mensual", "Alto", "Norte", "Revisar salida").forEachIndexed { i, v -> row.createCell(i).setCellValue(v) }
                file.outputStream().use { workbook.write(it) }
            }
            val inspection = OperationalDocumentReader.read(file, "entrada.xlsx", "Inspeccion")
            assertEquals("Mensual", inspection.filas.single()["tipoInspeccion"])
            assertEquals("Alto", inspection.filas.single()["riesgo"])
            val incident = OperationalDocumentReader.read(file, "entrada.xlsx", "Incidente")
            assertEquals("Mensual", incident.filas.single()["tipo"])
            assertTrue("riesgo" !in incident.filas.single())
            assertTrue(OperationalDocumentReader.read(file, "entrada.xlsx", "Otro").filas.isEmpty())
        } finally { file.delete() }
    }
    @Test fun wordTableAndScannedPdfDoNotFabricateRecords() {
        val word = File.createTempFile("operations-", ".docx")
        val pdf = File.createTempFile("operations-", ".pdf")
        try {
            XWPFDocument().use { doc ->
                val table = doc.createTable(2, 3)
                listOf("Fecha", "Tema", "Instructor").forEachIndexed { i, v -> table.getRow(0).getCell(i).text = v }
                listOf("20/09/2026", "Seguridad", "Rosa").forEachIndexed { i, v -> table.getRow(1).getCell(i).text = v }
                word.outputStream().use { doc.write(it) }
            }
            val training = OperationalDocumentReader.read(word, "capacitacion.docx", "Capacitacion")
            assertEquals("Seguridad", training.filas.single()["tema"])
            PDDocument().use { doc ->
                doc.addPage(PDPage())
                doc.save(pdf)
            }
            val scan = OperationalDocumentReader.read(pdf, "escaneo.pdf", "Inspeccion")
            assertTrue(scan.filas.isEmpty())
            assertTrue(scan.advertencias.any { it.contains("OCR") })
        } finally { word.delete(); pdf.delete() }
    }
}
