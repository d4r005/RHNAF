package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Nómina") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Recibos Generados", style = MaterialTheme.typography.titleLarge)
            
            val receipts = listOf(
                PayrollReceipt("Noviembre 2023 - Q1", "2023-11-15", "$8,500.00"),
                PayrollReceipt("Octubre 2023 - Q2", "2023-10-31", "$8,500.00"),
                PayrollReceipt("Octubre 2023 - Q1", "2023-10-15", "$8,500.00"),
            )
            
            LazyColumn {
                items(receipts) { receipt ->
                    ListItem(
                        headlineContent = { Text(receipt.period) },
                        supportingContent = { Text("Fecha: ${receipt.date} | Total: ${receipt.amount}") },
                        trailingContent = { 
                            IconButton(onClick = { /* TODO: Download PDF */ }) {
                                Icon(Icons.Default.Download, contentDescription = "Descargar")
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

data class PayrollReceipt(val period: String, val date: String, val amount: String)
