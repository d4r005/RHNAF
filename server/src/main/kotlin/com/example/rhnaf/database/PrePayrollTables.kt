package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

// ============================================================
// Tablas de Pre-Nómina (estilo PreAsyst)
// ============================================================

// Turnos / Horarios
object ShiftTable : Table("pp_shifts") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 200)
    val horaEntrada = varchar("hora_entrada", 10)
    val horaSalida = varchar("hora_salida", 10)
    val minutoTolerancia = integer("minuto_tolerancia").default(0)
    val minutosComida = integer("minutos_comida").default(60)
    val tipoTurno = varchar("tipo_turno", 50).default("Fijo")
    val horaEntrada2 = varchar("hora_entrada_2", 10).default("")
    val horaSalida2 = varchar("hora_salida_2", 10).default("")
    val activo = bool("activo").default(true)

    override val primaryKey = PrimaryKey(id)
}

// Políticas de asistencia
object AttendancePolicyTable : Table("pp_policies") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 200)
    val toleranciaRetardoMin = integer("tolerancia_retardo_min").default(5)
    val retardoMayorMin = integer("retardo_mayor_min").default(15)
    val salidaAnticipadaMin = integer("salida_anticipada_min").default(5)
    val horasExtraInicio = varchar("horas_extra_inicio", 10).default("")
    val primaDominical = double("prima_dominical").default(0.25)
    val diasDescanso = varchar("dias_descanso", 50).default("6")
    val activo = bool("activo").default(true)

    override val primaryKey = PrimaryKey(id)
}

// Asignación de turnos a empleados
object EmployeeShiftTable : Table("pp_employee_shifts") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val employeeName = varchar("employee_name", 200).default("")
    val shiftId = integer("shift_id")
    val shiftName = varchar("shift_name", 200).default("")
    val fechaInicio = varchar("fecha_inicio", 20).default("")
    val activo = bool("activo").default(true)

    override val primaryKey = PrimaryKey(id)
}

// Justificaciones (con flujo de aprobación)
object JustificationTable : Table("pp_justifications") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val employeeName = varchar("employee_name", 200).default("")
    val fecha = varchar("fecha", 20)
    val tipo = varchar("tipo", 50)
    val motivo = varchar("motivo", 500).default("")
    val evidencia = varchar("evidencia", 500).default("")
    val estado = varchar("estado", 50).default("Pendiente")
    val autorizadoPor = varchar("autorizado_por", 200).default("")
    val fechaSolicitud = varchar("fecha_solicitud", 20).default("")
    val observaciones = varchar("observaciones", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

// Pre-nómina (resultado del cálculo por periodo)
object PrePayrollTable : Table("pp_pre_payroll") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val employeeName = varchar("employee_name", 200).default("")
    val periodoInicio = varchar("periodo_inicio", 20)
    val periodoFin = varchar("periodo_fin", 20)
    val diasTrabajados = integer("dias_trabajados").default(0)
    val faltas = integer("faltas").default(0)
    val retardosMenores = integer("retardos_menores").default(0)
    val retardosMayores = integer("retardos_mayores").default(0)
    val salidasAnticipadas = integer("salidas_anticipadas").default(0)
    val horasTrabajadas = double("horas_trabajadas").default(0.0)
    val horasExtra = double("horas_extra").default(0.0)
    val primaDominical = double("prima_dominical").default(0.0)
    val diasDescansoTrabajados = integer("dias_descanso_trabajados").default(0)
    val observaciones = varchar("observaciones", 500).default("")
    val estado = varchar("estado", 50).default("Calculado")

    override val primaryKey = PrimaryKey(id)
}
