# 📇 BioVerify – Payroll Identity Verification Platform

**BioVerify** is a modular multi-tenant payroll verification system.  
It combines a secure Java Spring Boot backend with an Angular frontend to support identity enrollment, SSID/master list validation, biometric matching, and tenant management.

---

## 🚀 Overview

- **Backend**: Java (Spring Boot, Maven)
- **Frontend**: Angular (TypeScript)
- **Database**: Relational (JPA/Hibernate) — connection defined in `application.properties`
- **Security**: JWT authentication, Spring Security, crypto converter for sensitive fields
- **Core flows**:  
  ✅ User authentication & JWT  
  ✅ Master list uploads (for identity validation)  
  ✅ Biometric data submission & matching  
  ✅ Tenant management (multi-tenant support)

---

## 🗂️ Project Structure

\`\`\`
/bioverify
 ├── backend-java
 │   ├── pom.xml                # Maven dependencies
 │   ├── src/main/java/com/proximaforte/bioverify
 │   │    ├── controller/       # REST controllers (API endpoints)
 │   │    ├── service/          # Business logic
 │   │    ├── repository/       # JPA repositories
 │   │    ├── domain/           # Entity models & enums
 │   │    ├── config/           # JWT auth & security config
 │   │    ├── crypto/           # Encryption helpers
 │   │    ├── dto/              # Request/response models
 │   ├── src/main/resources/    # `application.properties`, static files
 │   ├── src/test/java/         # Unit tests scaffold
 │
 ├── frontend-angular
 │   ├── src/                   # Angular app source
 │   ├── package.json           # Frontend dependencies
 │   ├── angular.json           # Angular workspace config
 │   ├── node_modules/          # Installed packages
\`\`\`

---

## ⚙️ Backend Details

### ✅ **Key APIs**

| Endpoint | Purpose |
|----------|---------|
| `/auth` | User login, JWT issuance |
| `/verify` | Submit biometric or SSID data for verification |
| `/upload` | Master list file uploads |
| `/tenant` | Tenant admin management |

### 🔐 **Security**

- JWT tokens secured by `SecurityConfig` and `JwtAuthenticationFilter`.
- Roles defined in `domain.enums.Role`.
- Sensitive fields handled by `StringCryptoConverter`.

### 🗃️ **Database Entities**

- `User` — user accounts and credentials
- `Tenant` — multi-tenant isolation
- `MasterListRecord` — stored SSID/master list data
- `RecordStatus` & `Role` — enums for workflow status and access control

### 🧩 **Business Logic**

- Service layer (e.g., `VerificationService`, `MasterListUploadService`) handles core operations.
- JPA repositories interface with the database.

---

## 💻 Frontend Details

- **Framework**: Angular CLI.
- **Structure**: Modular `src/app/` with services for REST API integration.
- **Biometric Input**: Placeholder — SDK/device integration required.
- **Environment**: Uses `environment.ts` to configure backend base URLs.
- **State**: JWT tokens stored in LocalStorage or cookies (confirm in implementation).

---

## 🔗 How It Connects

- The Angular app calls backend REST endpoints with JWT tokens.
- Master list upload and biometric data submissions go through secured APIs.
- Multi-tenancy is enforced in backend services & queries.

---

## 🧪 Local Development

### ✅ Prerequisites

- **Backend**
  - Java 17+
  - Maven 3.x
  - Database connection configured in `application-local.properties`

- **Frontend**
  - Node.js (LTS)
  - Angular CLI (`npm install -g @angular/cli`)

---

### 🏃 Run Locally

\`\`\`bash
# Backend
cd backend-java
mvn spring-boot:run

# Frontend
cd frontend-angular
npm install
ng serve --open
\`\`\`

---

## ✅ Testing

- **Backend**
  - Run unit tests: `mvn test`
  - Current coverage: minimal (`BioverifyApplicationTests` scaffold) — extend with service & controller tests.

- **Frontend**
  - Uses default Angular test harness (`ng test`)

---

## 🔍 Future Development Considerations


- Expand unit & integration test coverage.
- Add large file upload resilience for master lists.
- Review encryption & sensitive data handling compliance.
- Consider containerizing for easier deployment.

---

## 📬 Contact / Maintainers

- **Org:** Proximaforte

---


---
