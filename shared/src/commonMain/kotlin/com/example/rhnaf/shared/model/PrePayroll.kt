package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============================================================
// Modelos de Pre-Nómina (estilo PreAsyst)
// ============================================================

// Turno/Horario: define entrada, salida, comida y tipo
@Serializable
data class Shift(
    val id: Int = 0,
    val nombre: String,               // "Turno Matutino 8h", "Turno Nocturno 12h", etc.
    val horaEntrada: String,          // "06:00"
    val horaSalida: String,            // "14:00"
    val minutoTolerancia: Int = 0,     // minutos de tolerancia antes de contar retardo
    val minutosComida: Int = 60,       // minutos de pausa para comida (se descuentan de horas trabajadas)
    val tipoTurno: String = "Fijo",    // "Fijo", "Quebrado", "Rotativo", "24h"
    val horaEntrada2: String = "",      // si es quebrado: segunda entrada
    val horaSalida2: String = "",      // si es quebrado: segunda salida
    val activo: Boolean = true
)

// Política de asistencia: reglas globales para generar incidencias
@Serializable
data class AttendancePolicy(
    val id: Int = 0,
    val nombre: String,                 // "Política General RHNAF"
    val toleranciaRetardoMin: Int = 5,  // después de cuántos minutos de la entrada es retardo menor
    val retardoMayorMin: Int = 15,      // después de cuántos minutos es retardo mayor (castigo)
    val salidaAnticipadaMin: Int = 5,  // minutos antes de la salida para contar salida anticipada
    val horasExtraInicio: String = "",  // hora a partir de la cual cuenta tiempo extra (ej. "14:00")
    val primaDominical: Double = 0.25,  // porcentaje de prima dominical (25%)
    val diasDescanso: String = "6",     // días de la semana que son descanso (0=domingo, 6=sabado)
    val activo: Boolean = true
)

// Asignación de turno a empleado
@Serializable
data class EmployeeShiftAssignment(
    val id: Int = 0,
    val employeeId: String,
    val employeeName: String = "",
    val shiftId: Int,
    val shiftName: String = "",
    val fechaInicio: String = "",       // desde qué fecha aplica este turno
    val activo: Boolean = true
)

// Justificación de falta/incidencia con flujo de aprobación
@Serializable
data class Justification(
    val id: Int = 0,
    val employeeId: String,
    val employeeName: String = "",
    val fecha: String,                  // fecha de la incidencia
    val tipo: String,                   // "Falta", "Retardo", "Salida anticipada", "Permiso", "Incapacidad", "Vacaciones"
    val motivo: String = "",
    val evidencia: String = "",          // URL o descripción de evidencia (foto de incapacidad, etc.)
    val estado: String = "Pendiente",   // "Pendiente", "Aprobado", "Rechazado"
    val autorizadoPor: String = "",
    val fechaSolicitud: String = "",
    val observaciones: String = ""
)

// Registro de pre-nómina: resultado del cálculo por empleado por periodo
@Serializable
data class PrePayrollRecord(
    val id: Int = 0,
    val employeeId: String,
    val employeeName: String = "",
    val periodoInicio: String,          // "2026-08-01"
    val periodoFin: String,             // "2026-08-15"
    val diasTrabajados: Int = 0,
    val faltas: Int = 0,
    val retardosMenores: Int = 0,
    val retardosMayores: Int = 0,
    val salidasAnticipadas: Int = 0,
    val horasTrabajadas: Double = 0.0,
    val horasExtra: Double = 0.0,
    val primaDominical: Double = 0.0,
    val diasDescansoTrabajados: Int = 0,
    val observaciones: String = "",
    val estado: String = "Calculado"    // "Calculado", "Revisado", "Cerrado"
)
