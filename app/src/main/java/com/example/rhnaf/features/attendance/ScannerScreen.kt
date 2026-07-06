package com.example.rhnaf.features.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import com.example.rhnaf.data.local.entities.AttendanceType
import com.example.rhnaf.ui.ViewModelFactory
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    viewModel: AttendanceViewModel = viewModel(factory = ViewModelFactory)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var scanResult by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var attendanceType by remember { mutableStateOf(AttendanceType.CLOCK_IN) }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
        locationPermissionState.launchMultiplePermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner de Asistencia") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        if (cameraPermissionState.status.isGranted && locationPermissionState.allPermissionsGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CameraPreview(
                    onBarcodeDetected = { barcode ->
                        if (scanResult == null) {
                            scanResult = barcode
                            handleAttendance(context, barcode, attendanceType, viewModel) { success, message ->
                                isSuccess = success
                                dialogMessage = message
                                showDialog = true
                            }
                        }
                    }
                )
                
                // Controls
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = attendanceType == AttendanceType.CLOCK_IN,
                            onClick = { attendanceType = AttendanceType.CLOCK_IN },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Entrada")
                        }
                        SegmentedButton(
                            selected = attendanceType == AttendanceType.CLOCK_OUT,
                            onClick = { attendanceType = AttendanceType.CLOCK_OUT },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Salida")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Escanee su QR para ${if (attendanceType == AttendanceType.CLOCK_IN) "ENTRADA" else "SALIDA"}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requieren permisos de cámara y ubicación")
                    Button(onClick = {
                        cameraPermissionState.launchPermissionRequest()
                        locationPermissionState.launchMultiplePermissionRequest()
                    }) {
                        Text("Solicitar Permisos")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                scanResult = null
            },
            title = { Text(if (isSuccess) "Éxito" else "Error") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    showDialog = false
                    if (isSuccess) onNavigateBack() else scanResult = null
                }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analysisExecutor) { imageProxy ->
                            processImageProxy(imageProxy, onBarcodeDetected)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("UnsafeOptInUsageError")
fun processImageProxy(imageProxy: ImageProxy, onBarcodeDetected: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { onBarcodeDetected(it) }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@SuppressLint("MissingPermission")
fun handleAttendance(
    context: Context,
    employeeId: String,
    viewModel: AttendanceViewModel,
    onResult: (Boolean, String) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            val factoryLat = 25.6866 // Mock factory lat
            val factoryLng = -100.3161 // Mock factory lng
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, factoryLat, factoryLng, distance)
            
            if (distance[0] < 500) { // 500 meters
                val log = AttendanceLogEntity(
                    employeeId = employeeId,
                    timestamp = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    type = AttendanceType.CLOCK_IN // In a real app, logic for IN vs OUT
                )
                viewModel.logAttendance(log)
                onResult(true, "Asistencia registrada correctamente. Distancia: ${distance[0].toInt()}m")
            } else {
                onResult(false, "Fuera de rango. Distancia: ${distance[0].toInt()}m. Debe estar a menos de 500m.")
            }
        } else {
            onResult(false, "No se pudo obtener la ubicación actual.")
        }
    }.addOnFailureListener {
        onResult(false, "Error al obtener ubicación: ${it.message}")
    }
}
