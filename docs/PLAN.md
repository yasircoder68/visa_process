# Visa Registration System — Build Plan

Place this file at `docs/PLAN.md`. `GEMINI.md` (repo root) points the Antigravity agent here.
Tick boxes as you go so the agent always knows the current phase.

**Current phase:** 2

---

## Decisions (read first — these fix gaps in the report)

The report describes the finished system, but a few things in it won't work as written or are
under-specified. Each decision below lists the report sections to update once implemented.

**D1 — Holiday API (blocker).** Nager.Date does not publish Indian holidays (India `IN` is listed under
"Missing" on its coverage page), so `PublicHolidays/2026/IN` returns nothing useful.
Chosen approach:
- Primary: **Calendarific** free tier (API key, covers India, limited monthly calls, attribution required).
  With a per-year in-memory cache this costs ~1 call per server restart.
- Fallback: bundled `holidays-IN-<year>.json` (central government gazetted holidays) used when the API
  key is missing or the call fails.
- Code sits behind `HolidayProvider` so the source can be swapped without touching scheduling logic.
- Report edits: §1, Fig 1, §4 step 11, §5.2, §12, §15 (bullet 2), §16, References.

**D2 — Web dashboard is its own app (port 3000).** The three servers are UI-less SOAP backends. The
dashboard (Thymeleaf + Spring Security) is the only thing the browser talks to; it calls the servers
over SOAP after discovering them through the registry.
- Report edits: §3 paragraph 1 and §16 ("Registration Server manages authenticated FRRO officer
  access" → "the Web Dashboard authenticates officers with Spring Security").

**D3 — Visa expiry.** §8 says "APPROVED and not expired" but `applications` has no expiry column.
Add `eta_number VARCHAR(20)` and `valid_until DATE`, both set by `issueETA()`.
- Report edits: §9.1, §9.5.

**D4 — Listing operations.** Officer dashboards need lists, but the report only has get-by-ID. Add
read-only SOAP ops `listApplications(status)` (visa) and `listRegistrations(officerId)` (registration).
- Report edits: §6 tables.

**D5 — Registration status flow.** The data flow (§4) jumps straight to APPOINTMENT_SCHEDULED; the
status diagram (§11) has DOCS_VERIFIED in between. Reconciled: `createRegistration` runs visa
verification (→ DOCS_VERIFIED) then scheduling (→ APPOINTMENT_SCHEDULED) in one call; the officer's
in-person VERIFY → REGISTERED. No report change needed beyond one sentence in §4 step 12.

**D6 — Database.** MySQL 8 on the same VM, port 3306 not exposed publicly. Add "MySQL 8" to §12.

---

## Target repo layout

```
visa-registration/
├── GEMINI.md
├── .gitignore                  # target/, *.env, deploy/env
├── pom.xml                     # parent: Java 21, Boot 3.x, module list
├── docs/
│   ├── PLAN.md
│   ├── Visa_Registration_Process_Report.pdf
│   └── soap-samples/           # request XMLs for curl testing
├── db/
│   ├── schema.sql              # report §9.5 + D3 columns
│   └── seed.sql                # officers
├── contracts/                  # XSDs → JAXB classes
│   └── src/main/resources/xsd/{registry,visa,registration}.xsd
├── registry-server/
├── visa-server/
├── registration-server/
├── web-dashboard/
└── deploy/
    ├── env.example
    ├── systemd/*.service
    └── deploy.sh
```

---

## Phase 0 — Environment setup
**Goal:** tools installed, empty repo opens cleanly in Antigravity.

- [x] JDK 21 (`java -version`), Maven 3.9+ (`mvn -v`)
- [x] MySQL 8 locally (or Docker `mysql:8`), create DB `visareg` and user `visareg`
- [x] Git repo created with the layout above (empty folders are fine), `GEMINI.md` at root, this file in `docs/`
- [x] Antigravity: open the repo root. If the agent doesn't seem to follow GEMINI.md, also copy it to
      `.agents/rules/project.md` (newer Antigravity versions read workspace rules from there)
- [x] Postman or SoapUI installed (optional; curl works too)

**Done when:** agent can summarise the project correctly from GEMINI.md without you explaining it.

---

## Phase 1 — Parent POM, contracts module, database
**Concepts:** Maven multi-module builds; contract-first SOAP (you write the XSD, tools generate Java);
XML namespaces; why every service shares one contracts jar.

**Tasks**
- [x] Parent `pom.xml`: `spring-boot-starter-parent` 3.x, `<java.version>21`, modules listed
- [x] `contracts` module with `jaxb2-maven-plugin` (org.codehaus.mojo, 3.x, jakarta namespace)
  - One `<execution>` per XSD, each with its own `packageName`
    (`mca.visareg.contracts.registry|visa|registration`)
  - `clearOutputDir=false` on the 2nd and 3rd executions, or they wipe each other's output
- [x] Write the three XSDs. Namespaces: `http://visareg.mca/registry`, `/visa`, `/registration`

| XSD | Request/response pairs |
|---|---|
| registry.xsd | registerService, findService |
| visa.xsd | submitApplication, getApplication, updateApplicationStatus, issueETA, listApplications |
| registration.xsd | createRegistration, verifyDocuments, scheduleAppointment, completeRegistration, listRegistrations |

- [x] Shared complex types: `ApplicationInfo` (all application columns), `RegistrationInfo`
      (all registration columns + officer name)
- [x] `db/schema.sql`: report §9.5 plus D3 columns (`eta_number`, `valid_until`)
- [x] `db/seed.sql`: one VISA_OFFICER (`VO01`) and 2–3 FRRO_OFFICERs. Leave `jurisdiction` as a
      placeholder until Phase 5 shows you Nominatim's real district string
- [x] Run both SQL files into MySQL manually

**Done when:** `mvn clean install` generates classes under `contracts/target/generated-sources` and
`SHOW TABLES;` lists 4 tables.

---

## Phase 2 — Registry Server (8080)
**Concepts:** what an `@Endpoint` is; `@PayloadRoot` routing by namespace + element name;
`DefaultWsdl11Definition` publishing a WSDL from an XSD; why the map must be thread-safe.

**Wiring**
| Piece | Detail |
|---|---|
| Endpoint | `RegistryEndpoint` → `registerService`, `findService` |
| Storage | `ConcurrentHashMap<String, String>` name → URL |
| WSDL | `http://localhost:8080/ws/registry.wsdl` |
| Unknown name | SOAP fault "Service not registered: X" |

**Tasks**
- [ ] Module deps: `spring-boot-starter-web-services`, `wsdl4j`, `contracts`
- [ ] `application.properties`: `server.port=8080`, `spring.webservices.path=/ws`
- [ ] WS config bean for the WSDL, endpoint class, custom exception with `@SoapFault(faultCode = CLIENT)`
- [ ] `docs/soap-samples/register.xml`, `find.xml`

**Done when:** curl register → curl find returns the URL; finding an unknown name returns a SOAP fault.

---

## Phase 3 — Visa Application Server (8081)
**Concepts:** JPA entity ↔ table mapping with `ddl-auto=validate`; mapping entities to JAXB DTOs;
reusable SOAP client (`WebServiceTemplate` + `Jaxb2Marshaller`); startup hooks.

**Wiring**
| Piece | Detail |
|---|---|
| Entity | `Application` → `applications` |
| Endpoint | `VisaEndpoint` → 5 ops from Phase 1 |
| Startup | `ApplicationReadyEvent` → registry `registerService("VisaApplicationService", "http://<host>:8081/ws")`, retry every 5 s |
| Config | `registry.url`, `app.public-url` (so Azure can register its real host) |

**Rules to implement**
- `submitApplication` → id `VISA###`, status SUBMITTED
- `updateApplicationStatus` → only SUBMITTED → APPROVED/REJECTED; anything else is a fault
- `issueETA` → only if APPROVED; sets `eta_number` (e.g. `ETA-VISA001`) and `valid_until`
  (e.g. +180 days)
- `getApplication` unknown id → fault

**Tasks**
- [ ] Put the registry client (`RegistryClient.register/find`) in a small shared package — the other
      two apps need it. Easiest: add it to `contracts` as a plain class
- [ ] Entity, repository, endpoint, startup registrar
- [ ] SOAP samples for each op

**Done when:** start registry then visa; submit → approve → issueETA via curl; registry `find` returns 8081.

---

## Phase 4 — Registration Server skeleton (8082)
**Concepts:** one SOAP service calling another; dynamic endpoint URI per call
(`marshalSendAndReceive(uri, request)`); translating a remote fault into your own fault.

**Wiring**
| Piece | Detail |
|---|---|
| Entities | `Registration`, `Officer` |
| Endpoint | `RegistrationEndpoint` → 5 ops (bodies can be stubs for now) |
| Visa check | `VisaClient.getApplication(id)` → URI from `registryClient.find("VisaApplicationService")` |
| Startup | registers `RegistrationService` |

**Tasks**
- [ ] Entities + repositories; `OfficerRepository` has a modifying query for the conditional claim
- [ ] `VisaClient`
- [ ] `verifyDocuments(registrationId)`: fetch application over SOAP; APPROVED and today ≤ `valid_until`
      → DOCS_VERIFIED, else fault
- [ ] Temporary: `createRegistration` accepts a hardcoded district, just to test the SOAP chain

**Done when:** with all three running, creating a registration for an APPROVED application reaches
DOCS_VERIFIED; for a SUBMITTED/REJECTED one you get a fault. Stop the visa server and confirm you get a
clean fault, not a stack trace.

---

## Phase 5 — External APIs
**Concepts:** `RestClient` + records for JSON; API etiquette (User-Agent, rate limit, caching);
provider interface pattern; why fallbacks matter for demos.

### 5a — Nominatim
- [ ] First, by hand: `curl -A "visareg-student/1.0 (you@email)" "https://nominatim.openstreetmap.org/search?q=T+Nagar,+Chennai&format=jsonv2&addressdetails=1&limit=1&countrycodes=in"`
      Save the response to `docs/samples/nominatim-tnagar.json`. Look at which `address` field holds the district
- [ ] Now set seed officer `jurisdiction` values to match that exact normalised string
- [ ] `GeocodingService.resolve(address)` → record `(lat, lon, district)`; no result → fault "Address not found"
- [ ] Simple rate guard: synchronized + ensure ≥1 s since last call

### 5b — Holidays (D1)
- [ ] Sign up for Calendarific, get key → env var `HOLIDAY_API_KEY`
- [ ] Hand-test one call for `country=IN&year=2026` and save the JSON sample before writing the record classes
- [ ] `HolidayProvider` interface: `Set<LocalDate> holidays(int year)`
- [ ] `CalendarificHolidayProvider` + `LocalFileHolidayProvider` (reads `holidays-IN-<year>.json`)
- [ ] Wrapper: try API → on error/no key use file; cache result per year
- [ ] `WorkingDayService.nextWorkingDay(LocalDate from)`: start tomorrow, skip Sat/Sun and holidays,
      loop across a year boundary correctly

**Done when:** unit test — given a known holiday on a Monday, `nextWorkingDay(the Friday before)` returns Tuesday.

---

## Phase 6 — Full orchestration & officer assignment
**Concepts:** `@Transactional` boundaries; why remote calls (SOAP/REST) should happen *before* you
open the DB transaction; optimistic "claim" updates instead of read-then-write.

**createRegistration sequence**
1. Geocode address (REST) — outside transaction
2. Verify visa (SOAP) — outside transaction
3. Compute appointment date
4. `@Transactional`: find AVAILABLE FRRO officers in district → claim first that returns 1 row →
   save registration as APPOINTMENT_SCHEDULED with lat/lon/district/date/officer
5. No officer → fault "No FRRO officer available in <district>"; nothing saved

**Also**
- [ ] `scheduleAppointment(registrationId)` for a DOCS_VERIFIED registration (reuses step 3–4)
- [ ] `completeRegistration(id)` → only from APPOINTMENT_SCHEDULED; sets REGISTERED, officer → AVAILABLE
- [ ] `listRegistrations(officerId)`

**Done when:** the report's §9.6 sequence happens through SOAP calls alone, and two quick
registrations for the same district with one officer → second one faults.

---

## Phase 7 — Web Dashboard (3000)
**Concepts:** Spring Security filter chain; custom `UserDetailsService`; role vs authority
(`hasRole("FRRO_OFFICER")` expects `ROLE_FRRO_OFFICER`); server-side rendering with Thymeleaf;
handling `SoapFaultClientException` as a friendly message.

**Pages**
| URL | Access | Calls |
|---|---|---|
| `/` | public | links to everything |
| `/apply` | public | `submitApplication` |
| `/track` | public | `getApplication` (shows status + ETA) |
| `/register` | public | `createRegistration` (shows officer + date) |
| `/login` | public | Spring Security form |
| `/visa-officer` | VISA_OFFICER | `listApplications(SUBMITTED)`, approve → `updateApplicationStatus` + `issueETA`, reject |
| `/frro` | FRRO_OFFICER | `listRegistrations(myOfficerId)`, VERIFY → `completeRegistration`, VIEW MAP |

**Tasks**
- [ ] `User` entity → `users`; `UserDetailsService` loads it; BCrypt encoder
- [ ] `UserSeeder` (`CommandLineRunner`): if `users` is empty, create `visaofficer01`/VO01 and
      `frroofficer02`/FRRO02 with passwords from env vars
- [ ] SOAP clients discovered via the registry (same `RegistryClient`)
- [ ] VIEW MAP: Leaflet page centred on the stored lat/lon with a marker and OSM attribution
- [ ] Plain, clean CSS — screenshots go in the report

**Done when:** you can do the whole §14 scenario in a browser, no curl.

---

## Phase 8 — Local end-to-end test (report §14 scenario)
- [ ] Fresh DB (re-run schema + seed)
- [ ] Start order registry → visa → registration → dashboard
- [ ] Apply: STUDENT visa, port of arrival Chennai
- [ ] Visa officer: approve → ETA shown on `/track`
- [ ] Register with "T Nagar, Chennai" → district resolved, officer assigned, date skips holiday/weekend
- [ ] FRRO officer: sees registration, VIEW MAP works, VERIFY → REGISTERED, officer AVAILABLE again
- [ ] Negative tests: unapproved application, nonsense address, no free officer, visa server stopped

Capture screenshots now (easier than on Azure): each page, a WSDL in the browser, one raw SOAP
request/response, registry `find` result.

---

## Phase 9 — Azure deployment
**Concepts:** VM sizing vs JVM memory; Network Security Groups; systemd services; env files.

**VM**
- Ubuntu 24.04 LTS. Size: B2s (4 GiB) is comfortable; B1ms (2 GiB) works with heap caps + 2 GB swap;
  B1s (1 GiB) is too small for 4 JVMs + MySQL
- Use Azure for Students credit. **Stop (deallocate) the VM when not demoing** — compute stops billing
  (the disk still does)

**NSG inbound rules**
| Port | Source | Why |
|---|---|---|
| 22 | your IP only | SSH / WinSCP |
| 3000 | any | dashboard |
| 8080–8082 | your IP (optional) | show WSDLs in the demo |
| 3306 | — | never open |

**Tasks**
- [ ] `sudo apt update && sudo apt install -y openjdk-21-jre-headless mysql-server`
      (install Maven too if you want to match report §13; otherwise build locally)
- [ ] Create DB + user, run `schema.sql` and `seed.sql`
- [ ] Build locally `mvn clean package`, upload the 4 jars to `/opt/visareg/` via WinSCP
- [ ] `/opt/visareg/env` (chmod 600): DB creds, `HOLIDAY_API_KEY`, `NOMINATIM_EMAIL`, officer passwords,
      `APP_PUBLIC_HOST=<vm-public-ip>`
- [ ] One systemd unit per service (template below), then
      `sudo systemctl enable --now visareg-registry visareg-visa visareg-registration visareg-dashboard`
- [ ] `journalctl -u visareg-visa -f` to confirm "registered with registry"

```ini
# /etc/systemd/system/visareg-visa.service
[Unit]
Description=Visa Application Server
After=network.target mysql.service visareg-registry.service
Wants=visareg-registry.service

[Service]
User=azureuser
WorkingDirectory=/opt/visareg
EnvironmentFile=/opt/visareg/env
ExecStart=/usr/bin/java -Xms64m -Xmx256m -XX:+UseSerialGC -jar /opt/visareg/visa-server.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

| Unit | Jar | After |
|---|---|---|
| visareg-registry | registry-server.jar | mysql |
| visareg-visa | visa-server.jar | registry |
| visareg-registration | registration-server.jar | registry, visa |
| visareg-dashboard | web-dashboard.jar | registration |

**Done when:** `http://<vm-ip>:3000` runs the Phase 8 scenario from your phone.

---

## Phase 10 — Report alignment & submission
- [ ] Apply every report edit listed under D1–D6
- [ ] §14: add Azure screenshots (portal VM page, `systemctl status`, dashboard over public IP)
- [ ] Calendarific attribution on the dashboard footer (free-tier requirement) and in References
- [ ] Push repo; `env` files not committed (`git log -p | grep -i password` returns nothing)
- [ ] Deallocate the VM after grading

---

## Handy Antigravity prompts
- "Read docs/PLAN.md. We're on Phase N. Explain the concepts and wiring for step X only. No code yet."
- "Wiring looks good. Implement step X only, then give the file table and test command."
- "Here's the error: … Diagnose before changing code."
- "Phase N done. Tick its boxes in PLAN.md and set Current phase to N+1."
