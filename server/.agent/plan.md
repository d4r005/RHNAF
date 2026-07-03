# Project Plan

RH NAF: A comprehensive HR management platform for industrial companies (maquiladoras) with CTPAT and ISO certifications. It covers the full employee lifecycle, EHS, security, and CTPAT compliance. Key modules include Dashboard, Digital Employee File, Recruitment, Onboarding, Attendance (QR/GPS/Biometric), Vacations, Incidences, Payroll integration, Training, Performance, Competencies, Career Plan, E-signature, Asset tracking, Occupational Health, Industrial Safety, CTPAT, Employee/Supervisor Portals, AI features, and Integrations.

## Project Brief

# Project Brief: RH NAF (MVP)

RH NAF is a specialized HR and operational management platform tailored for the maquiladora industry. It streamlines compliance with CTPAT and ISO certifications while managing the complete employee lifecycle and industrial safety requirements.

## Features
*   **Digital Employee File & Lifecycle:** A centralized management system for employee records, from recruitment and onboarding to performance reviews and career planning.
*   **Attendance & Security (QR/GPS):** Multi-modal attendance tracking utilizing QR codes and GPS verification to ensure secure and accurate time-logging for industrial sites.
*   **Compliance & Safety Modules (CTPAT/ISO):** Dedicated tools for monitoring Occupational Health, Industrial Safety, and CTPAT/ISO standards to maintain certifications.
*   **Role-Based Portals:** Unified dashboards for Employees and Supervisors to manage payroll integration, vacation requests, incidences, and asset tracking.

## High-Level Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose with Material 3 (Material Design 3)
*   **Navigation:** Jetpack Navigation 3 (State-driven)
*   **Adaptive Strategy:** Compose Material Adaptive Library (supporting diverse screen sizes for warehouse and office environments)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Networking:** Retrofit & OkHttp (for integration with central HRIS/Payroll systems)

## Implementation Steps

### Task_1_Core_Navigation_and_Employee_Management: Initialize the core application structure, database, and navigation. Complete the Digital Employee File module including listing, adding, and viewing employee details.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - AppDatabase is properly initialized in RHNAFApplication
  - Navigation 3 host is implemented in MainActivity
  - EmployeeListScreen, EmployeeDetailScreen, and AddEmployeeScreen are functional
  - Room database successfully stores and retrieves employee data
  - Build passes and basic navigation works
- **StartTime:** 2026-07-03 17:40:20 CST

### Task_2_Attendance_and_Scanner_Module: Implement the attendance tracking system using QR code scanning and GPS verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - ScannerScreen uses CameraX to decode QR codes
  - Attendance records are saved with location metadata (GPS)
  - AttendanceScreen displays current status and history
  - Permissions for Camera and Location are handled correctly

### Task_3_Compliance_Safety_and_Portals: Implement the Safety and Training modules, and finalize the Supervisor and Employee portals including digital signatures.
- **Status:** PENDING
- **Acceptance Criteria:**
  - SafetyScreen allows reporting and viewing incidents
  - TrainingScreen displays training progress and requirements
  - EmployeePortal and SupervisorPortal screens are fully integrated with their respective data
  - SignatureScreen captures and saves digital signatures
  - Asset tracking (Equipment) is functional within the Employee Portal

### Task_4_UI_Polish_Adaptive_Layouts_and_Verification: Apply Material 3 styling, implement adaptive layouts for various screen sizes, create the app icon, and perform final verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Material 3 theme with a vibrant color scheme is applied
  - Edge-to-Edge display is enabled
  - Adaptive layouts use Compose Material Adaptive library
  - Adaptive app icon is created and matches the theme
  - Final app is stable, does not crash, and meets all project brief requirements

