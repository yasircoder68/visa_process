# Visa & FRRO Registration System — Server & Command Guide

This document contains all server URLs, service endpoints, officer credentials, and start-to-end commands for running, testing, managing, and shutting down the application both locally and in cloud production (Render).

---

## 1. Live Production Deployment (Render)

| Item | Details |
|---|---|
| **Public Web Portal** | [https://visa-registration-system.onrender.com](https://visa-registration-system.onrender.com) |
| **Apply e-Visa** | [https://visa-registration-system.onrender.com/apply](https://visa-registration-system.onrender.com/apply) |
| **Track Application / ETA** | [https://visa-registration-system.onrender.com/track](https://visa-registration-system.onrender.com/track) |
| **Post-Arrival FRRO Registration** | [https://visa-registration-system.onrender.com/register](https://visa-registration-system.onrender.com/register) |
| **Officer Portal Login** | [https://visa-registration-system.onrender.com/login](https://visa-registration-system.onrender.com/login) |
| **Render Cloud Dashboard** | [https://dashboard.render.com/web/srv-dbufps0m7kps73c9ddr0](https://dashboard.render.com/web/srv-dbufps0m7kps73c9ddr0) |
| **GitHub Repository** | [https://github.com/yasircoder68/visa_process](https://github.com/yasircoder68/visa_process) |

---

## 2. Test Officer Credentials

All passwords are encrypted with BCrypt. Seeded credentials for portal evaluation:

| Role | Username | Password | Jurisdiction / Note |
|---|---|---|---|
| **Visa Officer** | `visaofficer01` | `password123` | e-Visa approval & ETA generation |
| **FRRO Officer** | `frroofficer01` | `password123` | Chennai (Primary, FRRO01) |
| **FRRO Officer** | `frroofficer02` | `password123` | Chennai (Secondary, FRRO02) |
| **FRRO Officer** | `frroofficer03` | `password123` | Coimbatore (FRRO03) |
| **FRRO Officer** | `frroofficer04` | `password123` | Madurai (FRRO04) |

---

## 3. Local Development (4 Microservices Architecture)

In local development, the 4 modules run as independent Spring Boot applications communicating strictly over SOAP network sockets.

### 3.1 Local Endpoints & Ports

| Service | Port | Endpoint / WSDL URL | Role |
|---|---|---|---|
| **Registry Server** | `8080` | `http://localhost:8080/ws/registry.wsdl` | Dynamic service registration & lookup |
| **Visa Server** | `8081` | `http://localhost:8081/ws/visa.wsdl` | SOAP VisaApplicationService (`applications` table) |
| **Registration Server** | `8082` | `http://localhost:8082/ws/registration.wsdl` | SOAP RegistrationService, Geocoding, Officer claim |
| **Web Dashboard** | `3000` | `http://localhost:3000/` | Public web UI, Thymeleaf, Spring Security |

---

### 3.2 Prerequisites (Local)
1. **Java 21+** (`java -version`)
2. **Maven 3.9+** (`mvn -version`)
3. **MySQL 8.0** running locally on port 3306 with database `visareg`:
   ```bash
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS visareg; CREATE USER IF NOT EXISTS 'visareg'@'localhost' IDENTIFIED BY 'visareg123'; GRANT ALL PRIVILEGES ON visareg.* TO 'visareg'@'localhost'; FLUSH PRIVILEGES;"
   mysql -u visareg -pvisareg123 visareg < db/schema.sql
   mysql -u visareg -pvisareg123 visareg < db/seed.sql
   ```

---

### 3.3 Build Commands
```bash
# Full build and compile contracts (XSD -> JAXB)
mvn clean install

# Fast build skipping tests
mvn clean package -DskipTests
```

---

### 3.4 Start Commands (Strict Startup Order)

Open 4 separate terminal windows and run the commands in this exact sequence:

#### Step 1: Start Registry Server (Port 8080)
```powershell
mvn -pl registry-server spring-boot:run
```
*Wait until you see:* `Tomcat started on port 8080 (http)`

#### Step 2: Start Visa Server (Port 8081)
```powershell
mvn -pl visa-server spring-boot:run
```
*Wait until you see:* `Successfully registered VisaApplicationService with Registry!`

#### Step 3: Start Registration Server (Port 8082)
```powershell
mvn -pl registration-server spring-boot:run
```
*Wait until you see:* `Successfully registered RegistrationService with Registry!`

#### Step 4: Start Web Dashboard (Port 3000)
```powershell
mvn -pl web-dashboard spring-boot:run
```
*Wait until you see:* `Tomcat started on port 3000 (http)`

---

### 3.5 Stop Commands (Local Teardown)

#### Graceful Stop
Press `Ctrl + C` in each of the 4 terminal windows.

#### Force Stop / Kill Stale Processes by Port (PowerShell)
```powershell
# Stop any process listening on ports 8080, 8081, 8082, 3000
8080, 8081, 8082, 3000 | ForEach-Object {
    $procId = (Get-NetTCPConnection -LocalPort $_ -ErrorAction SilentlyContinue).OwningProcess
    if ($procId) {
        Write-Host "Stopping process PID $procId on port $_..."
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}
```

---

## 4. Local Docker Container Run (All-in-One 512MB Mode)

Matches the exact production container deployed on Render.

### 4.1 Build Docker Image
```bash
docker build -t visa-registration-system:latest -f deploy/Dockerfile .
```

### 4.2 Start Container
```bash
docker run -d --name visa-container -p 10000:10000 --memory=512m visa-registration-system:latest
```
Access dashboard locally at: `http://localhost:10000`

### 4.3 View Container Logs
```bash
docker logs -f visa-container
```

### 4.4 Stop & Remove Container
```bash
docker stop visa-container
docker rm visa-container
```

---

## 5. Render CLI Management Commands

Render CLI executable location:
`C:\Users\user0\Documents\cloud_project\render_cli\render.exe`

Service ID: `srv-dbufps0m7kps73c9ddr0`

### 5.1 Check Service Status & Deployment History
```powershell
& "C:\Users\user0\Documents\cloud_project\render_cli\render.exe" deploys list srv-dbufps0m7kps73c9ddr0
```

### 5.2 Stream Live Server Logs
```powershell
& "C:\Users\user0\Documents\cloud_project\render_cli\render.exe" logs --resources srv-dbufps0m7kps73c9ddr0 --tail
```

### 5.3 Trigger Manual Deployment
```powershell
& "C:\Users\user0\Documents\cloud_project\render_cli\render.exe" deploys create srv-dbufps0m7kps73c9ddr0
```

### 5.4 Restart Live Web Service
```powershell
& "C:\Users\user0\Documents\cloud_project\render_cli\render.exe" services restart srv-dbufps0m7kps73c9ddr0
```

---

## 6. Verification & Automated Test Commands

### 6.1 Test Live Cloud Deployment (Report §14 Scenario)
Runs full end-to-end test against the live Render deployment:
```powershell
powershell -ExecutionPolicy Bypass -File scratch/test_live_render.ps1
```

### 6.2 Test Local Environment (Report §14 Scenario)
Runs full end-to-end test against local 4-service cluster:
```powershell
powershell -ExecutionPolicy Bypass -File scratch/run_phase8_tests.ps1
```

### 6.3 Test WSDL Endpoints via cURL
```powershell
# Registry WSDL
curl.exe -i -s "http://localhost:8080/ws/registry.wsdl"

# Visa WSDL
curl.exe -i -s "http://localhost:8081/ws/visa.wsdl"

# Registration WSDL
curl.exe -i -s "http://localhost:8082/ws/registration.wsdl"
```
