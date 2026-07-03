package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.EmployeeDao
import com.example.rhnaf.data.local.entities.toDomain
import com.example.rhnaf.data.local.entities.toEntity
import com.example.rhnaf.shared.model.Employee
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmployeeRepository(private val employeeDao: EmployeeDao) {

    fun getAllEmployees(): Flow<List<Employee>> {
        return employeeDao.getAllEmployees().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getEmployeeById(id: String): Employee? {
        return employeeDao.getEmployeeById(id)?.toDomain()
    }

    suspend fun insertEmployee(employee: Employee) {
        employeeDao.insertEmployee(employee.toEntity())
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee.toEntity())
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee.toEntity())
    }
}
