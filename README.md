# BioVerify

A secure, multi-tenant payroll and pension verification system designed to eliminate ghost workers and ensure data integrity for government payrolls.

## About The Project

BioVerify is a full-stack application built with a Spring Boot backend and an Angular frontend. It serves as a "System of Record and Verification," allowing government tenants to upload employee master lists, verify identities against a trusted Source of Truth (SoT), manage data mismatches, and export clean, validated payroll files.

The system is built around a two-stage confirmation process, robust role-based access controls, and a complete audit trail for all actions.

## Key Features

- **Multi-Tenant Architecture**: Securely isolates data for different government clients.
- **Verification & Validation Workflow**: A two-step process where employees first verify their identity, and administrators later provide a final validation.
- **Asynchronous Bulk Operations**: Perform bulk verification and payroll exports as non-blocking background jobs.
- **Unique Work ID (WID) Generation**: Assigns a permanent, unique WID to every validated employee record.
- **Role-Based Access Control (RBAC)**: Granular permissions, including the ability to scope a reviewer's access to specific ministries or departments.
- **Administrative Dashboards**: Rich dashboards for performance overview, job tracking, and managing validation queues.
- **Mismatch Resolution**: A side-by-side UI to efficiently resolve data discrepancies between the uploaded list and the Source of Truth.
- **Complete Audit Trail**: Logs all critical actions, from uploads to exports.

## Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

- Java 17+ and Maven
- Node.js and Angular CLI
- PostgreSQL database

### Run Locally

**Clone the repository:**

```bash
git clone <your-repository-url>
```

**Configure the Backend:**

- Navigate to `backend-java/`
- Update the database connection and JWT secret in `src/main/resources/application.properties`.

**Run the Backend:**

```bash
cd backend-java
mvn spring-boot:run
```

The backend will be available at `http://localhost:8080`.

**Run the Frontend:**

```bash
cd ../frontend-angular
npm install
ng serve
```

The frontend will be available at `http://localhost:4200`.

## System Architecture & In-Depth Documentation

This section provides a detailed technical overview of the BioVerify platform.

### Project Structure

The project is a monorepo containing two main packages:

- `backend-java/`: The Spring Boot REST API that handles all business logic, database interaction, and security.
- `frontend-angular/`: The Angular SPA that provides the complete user interface.

### Backend (Spring Boot)

#### Core Concepts

**Two-Stage Confirmation: Verification vs. Validation**

- **Verification**: An automated, user-driven process where an employee proves their identity by matching their SSID + NIN against the external Source of Truth (SoT).
- **Validation**: A manual, administrative process where a REVIEWER or TENANT_ADMIN gives the final approval to a verified record, making it eligible for payroll.

**Asynchronous Jobs**: Computationally heavy tasks like bulk verification and payroll exports are executed as asynchronous background jobs to prevent blocking API requests and improve system responsiveness.

**Work ID (WID) Generation**: Upon final validation, the system generates a unique, permanent Work ID (WID) for each employee record. This is the primary identifier for verified personnel.

#### Domain Entities & Repositories

**Key Entities**: `MasterListRecord` (with a `wid` field), `Tenant`, `User`, `Ministry`, `Department`, `BulkVerificationJob`, `PayrollExportLog`.

**Enums**: `Role`, `JobStatus`, and `RecordStatus` (which includes states like `PENDING_VERIFICATION`, `PENDING_APPROVAL`, `FLAGGED_DATA_MISMATCH`, and `FLAGGED_NOT_IN_SOT`).

#### Services (Business Logic)

- `VerificationService`: Orchestrates the automated verification step.
- `MasterListRecordService`: Manages the administrative validation queue and mismatch resolution.
- `BulkVerificationService` & `ExportService`: Run asynchronous background jobs.
- `WIDGenerationService`: A dedicated service to generate a unique WID upon validation.

#### Security & RBAC

The system uses JWT for stateless authentication.

A key RBAC feature is that Tenant Admins can assign Reviewers to specific Ministries and Departments. The backend enforces this by filtering the validation queues based on the authenticated reviewer's assignments.

### Frontend (Angular)

#### Key Components & Dashboards

- **Performance Overview Dashboard**: For Tenant Admins. Displays high-level KPIs: total records, a funnel showing the count of records at each stage (uploaded -> verified -> validated), and user counts by role.
- **Bulk Verification Dashboard**: Displays the history and status of all bulk verification jobs, showing verified vs. not-found counts.
- **ValidationQueueComponent (Refactored)**: Presents a tabbed interface for a cleaner workflow:
  - **"Pending Approval" Tab**: Shows records that have been successfully verified by users and are awaiting final validation.
  - **"Mismatched Data" Tab**: A dedicated view for reviewers to handle records flagged with `FLAGGED_DATA_MISMATCH`.
- **Mismatch Resolution Component**: A UI for reviewers that shows a side-by-side comparison of the originally uploaded data versus the data received from the SoT, allowing for one-click resolution.

### Key Workflows

#### End-to-End Record Lifecycle (User-Driven)

1. **Ingestion**: A `TENANT_ADMIN` uploads a master list. Records are created with a `PENDING_VERIFICATION` status.
2. **Verification (User-Driven)**: An employee uses the self-service portal to provide their SSID + NIN. The system checks this against the SoT.
   - On Success: Status becomes `PENDING_APPROVAL`.
   - If Not Found: Status becomes `FLAGGED_NOT_IN_SOT`.
3. **Validation (Admin-Driven)**: A `REVIEWER` logs into their dashboard and validates scoped records.
4. **Finalization**: Upon validation, a WID is assigned, and the record becomes `VALIDATED`.
5. **Export**: A `TENANT_ADMIN` exports all `VALIDATED` records as a CSV.

#### Bulk Verification Workflow (Admin-Driven)

1. **Prerequisite**: Uploaded master list must have `SSID` and `NIN`.
2. **Initiation**: Admin selects upload and starts bulk verification.
3. **Execution**: Background job triggers, processing each record via SoT.
4. **Status Updates**:
   - On Success: `PENDING_APPROVAL`
   - Not Found: `FLAGGED_NOT_IN_SOT`
5. **Completion & Reporting**: Dashboard updates with results (e.g., 10,000 processed: 9,500 succeeded, 500 not found).
6. **Next Step**: Successful records are queued for human validation.