# Visa Registration System — Agent Rules

## Project
MCA(SS) Cloud Computing mini project. Three Spring Boot SOAP services + a web dashboard that link the
pre-arrival e-Visa application with post-arrival FRRO registration. Deployed on one Azure Ubuntu VM.

- Source of truth: `docs/Visa_Registration_Process_Report.pdf`
- Build plan + decisions: `docs/PLAN.md` — read the **current phase** there before doing anything.

## How to work
- Execute. Don't explain concepts, don't narrate your plan, don't ask for approval before coding.
- Work through the current phase in PLAN.md to completion: write the code, build it, run its
  "Done when" check, fix failures yourself. Then tick the boxes, bump "Current phase", and stop.
- Final message per phase, nothing else:
  - Files changed (paths only)
  - Commands run + pass/fail
  - Anything I must do manually (install, env var, sign-up)
- Ask only when truly blocked, or when a change would contradict the report
  (names, ports, statuses, tables) or add something outside PLAN.md.
- Before writing classes for an external API, fetch a real response and save it in `docs/samples/`.

## Stack (fixed)
Java 21 · Spring Boot 3.x · Maven multi-module · Spring Web Services (contract-first XSD → JAXB) ·
Spring Security · Spring Data JPA · Thymeleaf · MySQL 8 (H2 MySQL-mode for tests) ·
Spring `RestClient` for external REST · Leaflet + OSM tiles for VIEW MAP.

## Modules & ports
| Module | Port | Role | Owns tables |
|---|---|---|---|
| contracts | – | XSDs + generated JAXB classes (shared) | – |
| registry-server | 8080 | SOAP `registerService` / `findService`, in-memory map | – |
| visa-server | 8081 | SOAP VisaApplicationService | applications |
| registration-server | 8082 | SOAP RegistrationService, geocoding, holidays, officer assignment | registrations, officers |
| web-dashboard | 3000 | Thymeleaf UI, Spring Security login, SOAP client of 8081/8082 | users |

## Hard rules
1. Service-to-service = SOAP only. REST only for external APIs.
2. Only `registry.url` is configured. Every other endpoint comes from `findService()`.
3. A service touches only its own tables. registration-server verifies visas ONLY via SOAP
   `getApplication()`, never by querying `applications`.
4. Each server calls `registerService()` on `ApplicationReadyEvent`, retrying until the registry answers.
5. Contract-first: edit XSD → rebuild `contracts` → then code. Elements: `<op>Request` / `<op>Response`.
6. `spring.webservices.path=/ws` (Boot default is `/services`). WSDL at `/ws/<name>.wsdl`.
7. Statuses are Java enums stored as strings, exactly:
   - Application: `SUBMITTED, APPROVED, REJECTED`
   - Registration: `SUBMITTED, DOCS_VERIFIED, APPOINTMENT_SCHEDULED, REGISTERED`
   - Officer: `AVAILABLE, ASSIGNED`
8. Officer claim is race-safe: `UPDATE ... SET status='ASSIGNED' WHERE id=? AND status='AVAILABLE'`
   inside `@Transactional`; 0 rows → try next officer.
9. Passwords BCrypt only; seed users from a startup runner, never plaintext SQL.
10. Secrets (DB password, holiday API key, Nominatim contact email) from env vars. Never commit them.
11. All date logic in `Asia/Kolkata`.
12. Business failures → SOAP fault with a clear message; persist nothing on failure.

## createRegistration orchestration
Nominatim geocode → `findService("VisaApplicationService")` → SOAP `getApplication()` must be APPROVED
and not past `valid_until` → `DOCS_VERIFIED` → next working day (skip Sat/Sun + holidays) → claim an
AVAILABLE `FRRO_OFFICER` in that district → `APPOINTMENT_SCHEDULED`.
Officer's VERIFY button → `completeRegistration()` → `REGISTERED`, officer back to `AVAILABLE`.
`verifyDocuments` / `scheduleAppointment` are separate SOAP ops, also reused internally.

## External APIs
- **Nominatim**: `/search?q=…&format=jsonv2&addressdetails=1&limit=1&countrycodes=in`. Real
  `User-Agent` with contact email, max 1 req/s, one call per registration. District = first non-null of
  `state_district`, `county`, `city`, `town`, normalised (trim, strip " District").
- **Holidays**: behind a `HolidayProvider` interface, cached per year. Nager.Date has NO India data —
  use the provider in PLAN.md → Decisions D1.

## Commands
- Build: `mvn -q clean install`
- Run: `mvn -pl <module> spring-boot:run`
- Start order: registry → visa → registration → dashboard
- WSDL check: `curl http://localhost:8081/ws/visa.wsdl`
- SOAP test: `curl -s -H "Content-Type: text/xml" -d @docs/soap-samples/<file>.xml http://localhost:<port>/ws`

## Don't
- Generate the whole project in one go.
- Use REST controllers between services, or Spring Cloud/Eureka (the hand-built registry is a requirement).
- Use `ddl-auto=create/update` on MySQL — schema lives in `db/schema.sql`; use `validate`.
