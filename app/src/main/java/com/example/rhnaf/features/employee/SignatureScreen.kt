package com.example.rhnaf.features.employee

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SignatureScreen(onNavigateBack: () -> Unit) {
    var path = remember { Path() }
    val paths = remember { mutableStateListOf<Path>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firma Electrónica") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { paths.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                    IconButton(onClick = { /* TODO: Save Bitmap */ onNavigateBack() }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Firme dentro del recuadro", style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .pointerInteropFilter {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                path.moveTo(it.x, it.y)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                path.lineTo(it.x, it.y)
                                // We need to trigger recomposition. 
                                // One way is to create a new path or use a state for points.
                                val newPath = Path().apply { addPath(path) }
                                path = newPath
                            }
                            MotionEvent.ACTION_UP -> {
                                paths.add(path)
                                path = Path()
                            }
                        }
                        true
                    }
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { p ->
                        drawPath(
                            path = p,
                            color = androidx.compose.ui.graphics.Color.Black,
                            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    drawPath(
                        path = path,
                        color = androidx.compose.ui.graphics.Color.Black,
                        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Al firmar, acepto los términos y condiciones del reglamento interior de trabajo.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
