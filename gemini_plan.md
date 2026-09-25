# Visa Registration System — Gemini Build Plan

Living execution and progress tracking plan maintained by Gemini for the MCA(SS) Cloud Computing Visa & FRRO Registration System.

**Current Phase:** Phase 6 (Full Orchestration & Officer Assignment)

---

## Architecture & Modules Summary

| Module | Port | Technology / Role | Owns Tables / Data | Status |
|---|---|---|---|---|
| **contracts** | — | Shared contract-first XSDs, JAXB classes | — | **Completed** (Phase 1) |
| **registry-server** | `8080` | SOAP `registerService` / `findService`, in-memory map | In-memory registry map | **Completed** (Phase 2) |
| **visa-server** | `8081` | SOAP VisaApplicationService, ETA issuing | `applications` | **Completed** (Phase 3) |
| **registration-server** | `8082` | SOAP RegistrationService, Nominatim REST, Holiday API, Officer assignment | `registrations`, `officers` | **Next** (Phase 6 full orchestration) |
| **web-dashboard** | `3000` | Thymeleaf UI, Spring Security, SOAP client | `users` | Pending (Phase 7) |

---

## Architectural Decisions Implemented

* **D1 (Holiday Provider):** Calendarific primary + fallback `holidays-IN-<year>.json` under `HolidayProvider` interface to run offline without external API blockers.
* **D2 (Standalone Dashboard):** Spring Boot web app on port 3000 consuming SOAP endpoints via dynamic registry lookups.
* **D3 (Visa Expiry Columns):** `eta_number VARCHAR(20)` and `valid_until DATE` added to `applications` table.
* **D4 (List Operations):** Added `listApplications` to `visa.xsd` and `listRegistrations` to `registration.xsd`.
* **D5 (Status Flow Reconciled):** `SUBMITTED` → `DOCS_VERIFIED` → `APPOINTMENT_SCHEDULED` → `REGISTERED`.
* **D6 (Local Database):** MySQL 8 running locally on port 3306 (`visareg` DB, user `visareg`).

---

## Phase Breakdown & Progress Tracker

### [x] Phase 0 — Environment Setup
- [x] Java 21 / 25 verified.
- [x] Maven 3.9.16 installed and configured.
- [x] MySQL 8.0 running (`MySQL80` service).
- [x] Created database `visareg` and user `visareg`.
- [x] Base project repository structure established with `.gitignore`.

### [x] Phase 1 — Parent POM, Contracts Module & Database
- [x] Parent `pom.xml` with Spring Boot 3.3.4 parent, Java 21 target, and multi-module layout.
- [x] `contracts/pom.xml` with `jaxb2-maven-plugin` (org.codehaus.mojo 3.2.0, Jakarta namespace) with 3 isolated executions (`clearOutputDir=false` for 2nd and 3rd).
- [x] `contracts/src/main/resources/xsd/registry.xsd` (`registerService`, `findService`).
- [x] `contracts/src/main/resources/xsd/visa.xsd` (`submitApplication`, `getApplication`, `updateApplicationStatus`, `issueETA`, `listApplications`, `ApplicationInfo`).
- [x] `contracts/src/main/resources/xsd/registration.xsd` (`createRegistration`, `verifyDocuments`, `scheduleAppointment`, `completeRegistration`, `listRegistrations`, `RegistrationInfo`).
- [x] Verified `mvn clean install` builds and compiles 32 generated JAXB classes into `contracts/target/generated-sources/jaxb/`.
- [x] `db/schema.sql` written and executed: creates `officers`, `applications`, `registrations`, `users`.
- [x] `db/seed.sql` written and executed: seeds sample officers across Chennai, Coimbatore, Madurai.

### [x] Phase 2 — Registry Server (Port 8080)
- [x] Dependencies: `spring-boot-starter-web-services`, `wsdl4j`, `contracts`.
- [x] Configure `application.properties`: `server.port=8080`, `spring.webservices.path=/ws`.
- [x] Spring WS `WebServiceConfig` publishing WSDL at `/ws/registry.wsdl`.
- [x] `RegistryEndpoint`: handles `registerServiceRequest` and `findServiceRequest`.
- [x] Thread-safe storage: `ConcurrentHashMap<String, String>` mapping service names to endpoint URLs.
- [x] SOAP fault handling: custom exception annotated with `@SoapFault(faultCode = FaultCode.CLIENT)` when service is not registered.
- [x] Create test XMLs in `docs/soap-samples/` (`register.xml`, `find.xml`, `find-unknown.xml`).
- [x] Verify: register service via curl → find service returns URL → unknown service returns SOAP fault.

### [x] Phase 3 — Visa Application Server (Port 8081)
- [x] JPA Entity `Application` mapping to `applications` table (`ddl-auto=validate`).
- [x] Shared `RegistryClient` (helper for registering and discovering endpoints).
- [x] `VisaEndpoint` implementing all 5 visa operations.
- [x] Startup hook: `ApplicationReadyEvent` registering `VisaApplicationService` with Registry Server (retry logic).
- [x] SOAP fault handling for invalid state transitions or unknown applications.
- [x] Verification with sample curl requests: submit → approve → issueETA, and registry discovery verified.

### [x] Phase 4 — Registration Server Skeleton (Port 8082)
- [x] JPA Entities: `Registration`, `Officer`.
- [x] Repositories: `RegistrationRepository`, `OfficerRepository` with atomic claim/release queries.
- [x] Dynamic SOAP client `VisaClient` querying `VisaApplicationService` via Registry lookup.
- [x] `RegistrationEndpoint` with `verifyDocuments` and `createRegistration` reaching `DOCS_VERIFIED`.
- [x] Startup hook registering `RegistrationService` with Registry Server.
- [x] Verified full SOAP verification chain: APPROVED visa reaches `DOCS_VERIFIED`, unapproved visa returns fault, stopped visa-server returns clean SOAP fault.

### [x] Phase 5 — External APIs Integration
- [x] 5a: Nominatim Location API (`RestClient` address geocoding + district extraction + rate limiter + samples saved).
- [x] 5b: Holiday Provider (Calendarific + local gazetted holiday JSON fallback + `WorkingDayService` for appointment scheduling).
- [x] Unit test verified: given a known holiday on a Monday (Republic Day 2026-01-26), `nextWorkingDay(Friday 2026-01-23)` returns Tuesday 2026-01-27.

### [ ] Phase 6 — Full Orchestration & Officer Assignment
- [ ] Atomic officer assignment (`UPDATE ... SET status='ASSIGNED' WHERE id=? AND status='AVAILABLE'`).
- [ ] `createRegistration` orchestration chain: Nominatim → Visa verification SOAP → appointment date calculation → officer claiming.
- [ ] `completeRegistration` transition to `REGISTERED` and freeing officer back to `AVAILABLE`.

### [ ] Phase 7 — Web Dashboard (Port 3000)
- [ ] Spring Security login for Visa Officers and FRRO Officers (BCrypt authentication against `users` table).
- [ ] Thymeleaf UI:
  - `/apply`: Submit e-Visa.
  - `/track`: View visa status and ETA.
  - `/register`: Submit post-arrival registration.
  - `/visa-officer`: Review, approve, reject applications and issue ETA.
  - `/frro`: View assigned appointments, verify documents, Leaflet OpenStreetMap view.

### [ ] Phase 8 — End-to-End Testing (Report §14 Scenario)
- [ ] Local simulation of full workflow from e-Visa submission to FRRO verification.

### [ ] Phase 9 — Azure VM Deployment
- [ ] Ubuntu Linux Azure VM deployment, systemd service units, and environment configuration.

### [ ] Phase 10 — Final Polish & Report Alignment
- [ ] Update report references and document final setup.
