# Comprehensive Project & SOAP Presentation Guide
### Visa & Foreigners Regional Registration Office (FRRO) System
**MCA Cloud Computing Mini-Project**

---

## Table of Contents
1. [Executive Summary (What is this project?)](#1-executive-summary)
2. [What is SOAP? (The Core Technology)](#2-what-is-soap)
   - [SOAP vs REST Comparison](#soap-vs-rest)
   - [Structure of a SOAP Message](#structure-of-a-soap-message)
   - [Contract-First Development (XSD & WSDL)](#contract-first-development)
3. [Architecture & Microservices Blueprint](#3-architecture--microservices-blueprint)
   - [System Components & Port Mapping](#system-components)
   - [Microservice Isolation & Database Boundaries](#microservice-isolation)
   - [Dynamic Service Registry Pattern](#dynamic-service-registry-pattern)
4. [End-to-End Workflow (The Life of a Visa & Registration)](#4-end-to-end-workflow)
   - [Phase 1: Pre-Arrival e-Visa Application](#phase-1-pre-arrival-e-visa)
   - [Phase 2: Visa Officer Review & ETA Issuance](#phase-2-visa-officer-review)
   - [Phase 3: Post-Arrival FRRO Registration & Orchestration](#phase-3-post-arrival-frro)
   - [Phase 4: In-Person Document Verification](#phase-4-in-person-verification)
5. [Key Technical Innovations & Engineering Highlights](#5-key-technical-innovations)
   - [Atomic Race-Safe Officer Assignment](#atomic-race-safe-officer-assignment)
   - [External API Integration with Resilient Fallbacks](#external-api-integration)
   - [Cloud Deployment & 512MB RAM Optimization](#cloud-deployment-optimization)
6. [Presentation & Viva Voce Q&A Cheat Sheet](#6-presentation--viva-voce-qa)

---

## 1. Executive Summary

### The Real-World Problem
When a foreign national plans to travel to India:
1. **Pre-Arrival (Ministry of External Affairs / Bureau of Immigration):** They apply for an electronic visa (e-Visa) before travelling and receive an Electronic Travel Authorization (ETA).
2. **Post-Arrival (Ministry of Home Affairs / FRRO):** Certain categories (e.g., Student visas, long-term stayers) are legally required by the Government of India to register with the nearest **Foreigners Regional Registration Office (FRRO)** within 14 days of arrival.

Historically, these two systems operated in data silos, requiring manual paper verification. 

### The Solution
This project implements an enterprise-grade, cloud-hosted, service-oriented architecture linking pre-arrival visa clearance with post-arrival FRRO registration using **SOAP (Simple Object Access Protocol)** web services, dynamic service discovery, GIS geocoding, automated public holiday scheduling, and role-based officer portals.

---

## 2. What is SOAP?

### 2.1 Definition
**SOAP** stands for **Simple Object Access Protocol**. It is an XML-based, strictly-typed protocol for exchanging structured information between decentralized, distributed systems over network protocols (most commonly HTTP).

### 2.2 SOAP vs REST: Why SOAP for Government & Enterprise Systems?

| Feature | SOAP (Used Internally Between Our Services) | REST (Used for External APIs like Nominatim) |
|---|---|---|
| **Protocol vs Style** | Formal protocol with strict specifications (W3C standard). | Architectural style (flexible, no single strict standard). |
| **Data Format** | **Strictly XML only**. | Primarily JSON, but can be plain text, XML, HTML, etc. |
| **Contract / Schema** | **Strict Contract-First:** Defined by XSD (XML Schema Definition) and WSDL (Web Services Description Language). Code cannot violate the contract. | Typically contract-optional (OpenAPI/Swagger exists, but no compile-time enforcement). |
| **Transport** | Transport-independent (can run over HTTP, HTTPS, SMTP, JMS). | Tied to HTTP/HTTPS verbs (GET, POST, PUT, DELETE). |
| **Error Handling** | Standardized **SOAP Fault** elements (`faultcode`, `faultstring`, `detail`). | HTTP status codes (400, 404, 500) with custom error bodies. |
| **Use Case in Project** | **Mission-critical inter-service communication:** Government department contracts where schemas, validation, and immutability must be 100% guaranteed. | Public read-only geocoding (OpenStreetMap Nominatim REST endpoint). |

### 2.3 Structure of a SOAP Message

Every SOAP call is an XML document containing four fundamental blocks:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:vis="http://mca.visareg/contracts/visa">
    <!-- Optional Header: Authentication, routing, transaction IDs -->
    <soapenv:Header/>

    <!-- Mandatory Body: Contains the request or response payload -->
    <soapenv:Body>
        <vis:submitApplicationRequest>
            <vis:applicantName>Mohamed Yasir A</vis:applicantName>
            <vis:passportNumber>P202517906</vis:passportNumber>
            <vis:visaType>STUDENT</vis:visaType>
            <vis:portOfArrival>Chennai</vis:portOfArrival>
        </vis:submitApplicationRequest>
    </soapenv:Body>
</soapenv:Envelope>
```

#### The 4 SOAP Elements:
1. `<soapenv:Envelope>`: The root element that identifies the XML document as a SOAP message.
2. `<soapenv:Header>`: Optional container for metadata (credentials, security tokens, message correlation IDs).
3. `<soapenv:Body>`: Contains the actual operational payload (e.g., `submitApplicationRequest`, `issueETARequest`).
4. `<soapenv:Fault>`: Standard error payload returned if an exception occurs during processing.

### 2.4 Contract-First Development (How We Built the Services)

In software engineering, there are two ways to build web services:
- **Code-First:** Write Java classes, then let a framework automatically generate XML/JSON. (Leads to fragile, coupled code).
- **Contract-First (Used in this project):** 
  1. Define the formal schema in **XSD** (`.xsd` files).
  2. Maven's `jaxb2-maven-plugin` compiles the XSD into immutable, type-safe **JAXB** Java classes.
  3. Spring Boot implements the endpoints using these generated classes.
  4. Spring Web Services exposes the live WSDL dynamically at runtime (e.g., `/ws/visa.wsdl`).

```
[contracts/src/main/resources/xsd/visa.xsd]
                    │
                    ▼  (mvn clean compile / jaxb2-maven-plugin)
[Generated JAXB Java Classes: SubmitApplicationRequest, ApplicationInfo, etc.]
                    │
                    ▼
[VisaEndpoint.java in visa-server implements SOAP logic]
                    │
                    ▼
[Exposed dynamically at http://localhost:8081/ws/visa.wsdl]
```

---

## 3. Architecture & Microservices Blueprint

```
                     ┌────────────────────────────────────────┐
                     │          USER / OFFICER BROWSER        │
                     │  (Public: /apply, /track, /register)   │
                     │  (Officer: /visa-officer, /frro)       │
                     └───────────────────┬────────────────────┘
                                         │ HTTP (Port 3000 / 10000)
                                         ▼
                     ┌────────────────────────────────────────┐
                     │             WEB DASHBOARD              │
                     │     (Spring Security, Thymeleaf UI)    │
                     └───────┬────────────────────────┬───────┘
                             │                        │
       1. findService() SOAP │                        │ 3. SOAP Requests
                             ▼                        ▼
                ┌────────────────────────┐      ┌────────────────────────┐
                │    REGISTRY SERVER     │◄─────┤      VISA SERVER       │
                │      (Port 8080)       │      │      (Port 8081)       │
                │ Hand-crafted In-Memory │      │  Table: 'applications' │
                │   Service Directory    │      └────────────────────────┘
                └────────────────────────┘                  ▲
                             ▲                              │ 4. SOAP getApplication()
       2. registerService()  │                              │    (Visa Verification)
                             │                  ┌───────────┴────────────┐
                             └──────────────────┤  REGISTRATION SERVER   │
                                                │      (Port 8082)       │
                                                │  Tables: 'registrations│
                                                │          'officers'    │
                                                └───────────┬────────────┘
                                                            │ REST (HTTP GET)
                                                            ▼
                                                ┌────────────────────────┐
                                                │  EXTERNAL REST APIs    │
                                                │  - Nominatim (Geocode) │
                                                │  - Calendarific / JSON │
                                                │    (Gazetted Holidays) │
                                                └────────────────────────┘
```

### 3.1 System Components

| Service | Port | Role | Database Tables Owned |
|---|---|---|---|
| **`contracts`** | — | Shared contract module containing XSD schemas and generated JAXB model classes. | None |
| **`registry-server`** | `8080` | SOAP Service Registry implementing UDDI pattern (`registerService`, `findService`). | In-memory `ConcurrentHashMap` |
| **`visa-server`** | `8081` | SOAP Visa Application Service handling submission, officer approval, status changes, and ETA issuing. | `applications` |
| **`registration-server`** | `8082` | SOAP Registration Service handling post-arrival registration, address geocoding, holiday calculation, and atomic officer claim. | `registrations`, `officers` |
| **`web-dashboard`** | `3000` / `10000` | Human-facing Web Portal (Thymeleaf, Spring Security BCrypt, Leaflet OpenStreetMap). Acts as a pure SOAP client. | `users` |

### 3.2 Microservice Isolation & Database Boundaries (Strict Rule)

A foundational rule of microservices is **Database-per-Service**:
- **`visa-server`** only connects to and queries the `applications` table.
- **`registration-server`** only connects to and queries the `registrations` and `officers` tables.
- **`web-dashboard`** only connects to the `users` table.

> **Crucial Concept for Examiners:** When a foreign national registers with FRRO on port 8082, how does `registration-server` know if the applicant's visa is valid?  
> **It NEVER queries the `applications` database table directly!**  
> Instead, it queries the Service Registry for `VisaApplicationService`, makes a network SOAP call (`getApplicationRequest`) to port 8081, and inspects the cryptographically validated SOAP response.

### 3.3 Dynamic Service Registry Pattern

In traditional monolithic systems, URLs are hardcoded (`localhost:8081`). If an IP changes, the system breaks.  
In this project:
1. Only `registry.url` (`http://localhost:8080/ws`) is configured in properties.
2. Every backend service listens for Spring Boot's `ApplicationReadyEvent`. On startup, it calls `registerService("VisaApplicationService", "http://localhost:8081/ws")`.
3. If the Registry is not yet up, services execute an exponential retry loop.
4. When any client needs to make a SOAP call, it calls `findService("VisaApplicationService")` to obtain the live endpoint dynamically.

---

## 4. End-to-End Workflow: The Life of a Visa

Here is the exact positive workflow demonstrated in the Report §14 presentation:

```
[Applicant]                [Visa Officer]             [Applicant in India]       [FRRO Officer]
     │                           │                             │                        │
     │ 1. /apply (Student Visa)  │                             │                        │
     ├──────────────────────────►│                             │                        │
     │                           │ 2. /visa-officer (Approve)  │                        │
     │                           ├────────────────────────────►│                        │
     │                           │    ETA Issued: ETA-VISA001  │                        │
     │                           │                             │                        │
     │ 3. /track (View ETA)      │                             │                        │
     │◄──────────────────────────┴─────────────────────────────┤                        │
     │                                                         │ 4. /register           │
     │                                                         │    ("T Nagar, Chennai")│
     │                                                         ├───────────────────────►│
     │                                                         │    - Geocodes to Chn   │
     │                                                         │    - SOAP Visa Check   │
     │                                                         │    - Next Working Day  │
     │                                                         │    - Assigns FRRO01    │
     │                                                         │                        │
     │                                                         │                        │ 5. /frro
     │                                                         │                        │    - View Map
     │                                                         │                        │    - Click VERIFY
     │                                                         │                        ├──────────────┐
     │                                                         │                        │ Status:      │
     │                                                         │                        │ REGISTERED   │
     │                                                         │                        │ Officer:     │
     │                                                         │                        │ AVAILABLE    │
     │                                                         │                        │◄─────────────┘
```

### Phase 1: Pre-Arrival e-Visa Application
1. Applicant visits `/apply` on the Web Dashboard.
2. Form fields:
   - Name: `Mohamed Yasir A`
   - Passport: `P202517906`
   - Visa Type: `STUDENT`
   - Port of Arrival: `Chennai`
3. Web Dashboard calls `findService("VisaApplicationService")` &rarr; invokes SOAP `submitApplicationRequest`.
4. `visa-server` inserts a new row in `applications`:
   - Status: `SUBMITTED`
   - Generated ID: `VISA001`

### Phase 2: Visa Officer Review & ETA Issuance
1. Visa Officer logs into `/login` with credentials `visaofficer01` / `password123`.
2. Spring Security authenticates using BCrypt hash against the `users` table and detects authority `ROLE_VISA_OFFICER`.
3. Officer sees pending applications at `/visa-officer` (populated via SOAP `listApplicationsRequest(status='SUBMITTED')`).
4. Officer clicks **APPROVE & ISSUE ETA**:
   - `visa-server` updates status to `APPROVED`.
   - `issueETA()` generates ETA reference `ETA-VISA001`.
   - Sets validity period: `valid_until = Current Date + 180 Days` (e.g. `2027-05-17`).
5. Applicant tracks application on `/track?id=VISA001` &rarr; sees status `APPROVED` with official ETA badge.

### Phase 3: Post-Arrival FRRO Registration & Automated Orchestration
When the student lands in Chennai, they must register their address within 14 days at `/register`.
- e-Visa ID: `VISA001`
- Stay Address: `T Nagar, Chennai`

The backend **`RegistrationService.createRegistration()`** triggers an intelligent 4-step orchestration pipeline:
1. **GIS Geocoding (Nominatim REST API):**  
   Sends address query to OpenStreetMap Nominatim. Extracts exact geographic coordinates (`lat: 13.0378`, `lon: 80.2318`) and normalizes the administrative district (`Chennai`).
2. **Cross-Service SOAP Visa Verification:**  
   Calls Registry &rarr; invokes SOAP `getApplication("VISA001")`.  
   *Security Checks:* Status must be `APPROVED` and `valid_until` must not be expired. If unapproved or expired, transaction is aborted and a SOAP fault is thrown.
3. **Smart Appointment Scheduling (Holiday & Weekend Guard):**  
   Calculates the next official working day in timezone `Asia/Kolkata`.
   - Evaluates tomorrow's date.
   - If Saturday or Sunday &rarr; rolls over to Monday.
   - Queries `HolidayProvider` (Calendarific Indian gazetted holidays). If the target day is a national holiday (e.g., Republic Day), skips to the subsequent working day.
4. **Atomic Officer Claim:**  
   Searches for an available `FRRO_OFFICER` within jurisdiction `Chennai`.  
   Claims Officer `FRRO01` (`Officer Ramesh`) atomically and transitions registration status to `APPOINTMENT_SCHEDULED`.

### Phase 4: In-Person Document Verification
1. FRRO Officer logs into `/login` (`frroofficer01` / `password123`) &rarr; redirected to `/frro`.
2. Officer views assigned verification appointments.
3. Officer clicks **View Map** &rarr; interactive Leaflet.js OpenStreetMap displays a geographic pin at the student's exact residential coordinates in T Nagar.
4. Officer inspects original passport and visa, then clicks **VERIFY DOCUMENTS**:
   - SOAP `completeRegistrationRequest` executes.
   - Registration status transitions to **`REGISTERED`**.
   - Officer `FRRO01` is released back to **`AVAILABLE`** to accept new applicants.

---

## 5. Key Technical Innovations & Engineering Highlights

### 5.1 Atomic Race-Safe Officer Assignment
In a high-concurrency immigration center, two foreigners could submit registrations at the exact same millisecond. If both claim the same officer, a race condition occurs.
- **Solution:** We enforce atomic database-level row claiming inside an isolated transaction:
```sql
UPDATE officers 
SET status = 'ASSIGNED' 
WHERE id = ? AND status = 'AVAILABLE';
```
- If another concurrent transaction claimed the officer first, MySQL returns `0 rows affected`.
- The service catches this, rolls back, and attempts to claim the next officer in that district. Persists nothing on failure.

### 5.2 Resilient External API Integrations (Defense in Depth)

| External API | Purpose | Resilient Fallback Strategy |
|---|---|---|
| **OpenStreetMap Nominatim** | Converts street addresses to `(lat, lon, district)` | Complies with OSM 1 req/sec rate limit. If public OSM blocks cloud datacenter egress IPs (HTTP 403), the service automatically activates an **in-memory gazetteer fallback** resolving standard Indian city coordinates without failing user registration. |
| **Calendarific API** | Fetches live Indian gazetted public holidays | Nager.Date has zero coverage for India. Calendarific covers India. To avoid external rate limits, results are cached in-memory per year. If the API key is missing or offline, it falls back to a bundled central government gazetted holiday dataset (`holidays-IN-2026.json`). |

### 5.3 Render Free Tier 512MB RAM Optimization
Deploying 4 separate Spring Boot Java 21 applications + MySQL 8 on standard servers requires ~1.5 GB RAM. However, Render's Free tier imposes a hard **512 MB memory limit** (exceeding it triggers an instant OOM kill).

**How we solved it:**
- **The Problem:** 4 separate Hotspot JVM processes mean 4x JVM native runtime (~140MB) + 4x Metaspace classloader data (~260MB) + MySQL (~130MB) = ~530MB before any application code runs!
- **The Solution:** We architected an **All-in-One unified runner** (`AllInOneApplication.java`) that launches all 4 Spring Boot application contexts inside a **single shared JVM process**:
  - Shares the classloader and Metaspace (~95 MB total).
  - Configured with `-Xms64m -Xmx220m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k`.
  - All 4 microservices still bind to their distinct ports (`8080`, `8081`, `8082`, `10000`) and communicate strictly over network sockets via SOAP.
  - Total container footprint: **~360 MB / 512 MB**, running with 150 MB of safe headroom!

---

## 6. Presentation & Viva Voce Q&A Cheat Sheet

Prepare for these exact questions from professors and evaluators:

### Q1: What is the main difference between SOAP and REST?
> **Answer:** "REST is an architectural style primarily using JSON over HTTP, best suited for public CRUD APIs. SOAP is a formal, strictly-typed protocol using XML envelopes. SOAP enforces contract-first development using XSD schemas and WSDL definitions, ensuring compile-time type safety, standardized SOAP Fault error handling, and transport independence. In our project, all internal communication between immigration services is strictly SOAP."

### Q2: Why did you build your own Registry Server instead of using Eureka or Consul?
> **Answer:** "Our academic requirement was to demonstrate contract-first SOAP and the fundamental Service-Oriented Architecture (SOA) directory lookup pattern (UDDI). We built a pure SOAP Registry Server on port 8080 exposing `registerService` and `findService` operations with a thread-safe `ConcurrentHashMap`. Services register themselves on startup and clients discover URLs dynamically."

### Q3: How do you prevent one microservice from modifying another service's data?
> **Answer:** "We follow the Database-per-Service architectural pattern. `visa-server` only has access to the `applications` table, and `registration-server` only has access to `registrations` and `officers`. When `registration-server` needs to verify a visa, it cannot query the SQL database directly; it must make a SOAP call to `VisaApplicationService` on port 8081."

### Q4: How is concurrency handled during officer assignment?
> **Answer:** "We use an atomic SQL update: `UPDATE officers SET status='ASSIGNED' WHERE id=? AND status='AVAILABLE'` executed within a Spring `@Transactional` boundary. If two requests arrive simultaneously, MySQL's row-level locking ensures only one succeeds (1 row updated). The second receives 0 rows updated and automatically loops to claim the next available officer."

### Q5: How does the system handle weekends and public holidays for appointments?
> **Answer:** "Our `WorkingDayService` uses `java.time.LocalDate` in the `Asia/Kolkata` timezone. It evaluates the subsequent calendar day; if it falls on Saturday or Sunday, it rolls over to Monday. It then checks against our `HolidayProvider` (powered by Calendarific with a local gazetted holiday fallback). If that Monday is a public holiday, it recursively advances to the next business day."

### Q6: Where is this deployed and how can we verify it?
> **Answer:** "It is deployed live on a cloud container at [https://visa-registration-system.onrender.com](https://visa-registration-system.onrender.com). Evaluators can apply for a student visa, log in as Visa Officer `visaofficer01` (`password123`) to approve and issue the ETA, submit an FRRO registration for an address in Chennai, and log in as FRRO Officer `frroofficer01` (`password123`) to inspect the Leaflet OpenStreetMap pin and verify documents."
