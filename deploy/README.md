# Azure VM Deployment Guide — Visa Registration System

Distributed SOAP & Web Services Visa Application and FRRO Coordination System deployed on Microsoft Azure Ubuntu Linux VM.

---

## 1. Virtual Machine & Network Security Group (NSG) Setup

### Azure VM Configuration
* **OS:** Ubuntu 24.04 LTS (or Ubuntu 22.04 LTS)
* **Size:** 
  * `Standard_B2s` (2 vCPU, 4 GiB RAM) — **Recommended**
  * `Standard_B1ms` (1 vCPU, 2 GiB RAM) — Supported with `-Xms64m -Xmx256m` and 2 GB swap
* **Authentication:** SSH public key or password (default user: `azureuser`)
* *Important:* Remember to **Stop (deallocate)** the VM from the Azure Portal when not demoing to save Student credits.

### Network Security Group (Inbound Security Rules)
| Priority | Name | Port | Protocol | Source | Purpose |
|---|---|---|---|---|---|
| 1000 | `Allow-SSH` | `22` | TCP | `Your IP` | SSH & WinSCP / SFTP file transfers |
| 1010 | `Allow-Dashboard` | `3000` | TCP | `Any` / `Internet` | Public Web Dashboard & Map UI |
| 1020 | `Allow-Registry-SOAP` | `8080` | TCP | `Your IP` (or `Any`) | Service Registry WSDL & Demo |
| 1030 | `Allow-Visa-SOAP` | `8081` | TCP | `Your IP` (or `Any`) | Visa Service WSDL & Demo |
| 1040 | `Allow-Registration-SOAP` | `8082` | TCP | `Your IP` (or `Any`) | Registration Service WSDL & Demo |
| — | *MySQL* | `3306` | TCP | — | **Never expose to internet** (Localhost only) |

---

## 2. Automated Server Setup (Run on VM)

Connect to your Azure VM via SSH:
```bash
ssh azureuser@<VM-PUBLIC-IP>
```

Clone the repository or transfer the `deploy/` and `db/` folders to the VM:
```bash
# Run the automated setup script
sudo bash deploy/setup-vm.sh
```

What `setup-vm.sh` does automatically:
1. Installs OpenJDK 21 headless, MySQL Server 8, and curl.
2. Creates the `visareg` database and `visareg` MySQL user.
3. Imports `db/schema.sql` (applications, registrations, officers, users).
4. Imports `db/seed.sql` (sample FRRO and Visa officers).
5. Creates the application home directory at `/opt/visareg`.
6. Generates the `/opt/visareg/env` configuration template with restricted permissions (`chmod 600`).
7. Copies and registers all 4 systemd service unit files into `/etc/systemd/system/`.

---

## 3. Upload Application JARs

All 4 production JARs are pre-built in `deploy/jars/`:
* `registry-server.jar`
* `visa-server.jar`
* `registration-server.jar`
* `web-dashboard.jar`

Transfer the 4 jars into `/opt/visareg/` using **WinSCP** or **SCP**:
```powershell
# From local Windows terminal:
scp deploy/jars/*.jar azureuser@<VM-PUBLIC-IP>:/opt/visareg/
```

Ensure ownership is set correctly on the VM:
```bash
sudo chown -R azureuser:azureuser /opt/visareg
```

---

## 4. Configure Environment Secrets

Edit `/opt/visareg/env` on the VM:
```bash
sudo nano /opt/visareg/env
```

Ensure the following variables are set:
```bash
# VM Public IP for external service discovery
APP_PUBLIC_HOST=<YOUR_AZURE_VM_PUBLIC_IP>

# Database credentials
DB_HOST=localhost
DB_PORT=3306
DB_NAME=visareg
DB_USERNAME=visareg
DB_PASSWORD=visareg123

# Service Registry endpoint
REGISTRY_URL=http://localhost:8080/ws

# External API configuration
NOMINATIM_EMAIL=your_email@example.com
HOLIDAY_API_KEY=your_optional_calendarific_key

# Officer initial password seeds
VISA_OFFICER_PASSWORD=password123
FRRO_OFFICER_PASSWORD=password123
```
Secure the permissions:
```bash
sudo chmod 600 /opt/visareg/env
```

---

## 5. Start and Enable Services

Enable and launch all 4 services via systemd:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now visareg-registry visareg-visa visareg-registration visareg-dashboard
```

Verify service statuses:
```bash
sudo systemctl status visareg-registry visareg-visa visareg-registration visareg-dashboard
```

Check live logs to confirm registration with the Service Registry:
```bash
journalctl -u visareg-visa -f
journalctl -u visareg-registration -f
journalctl -u visareg-dashboard -f
```

You should see:
`Successfully registered VisaApplicationService with Registry!`
`Successfully registered RegistrationService with Registry!`

---

## 6. Verification and Live Demo

Open your web browser (or mobile phone browser):
```
http://<VM-PUBLIC-IP>:3000
```

1. **Submit Application:** Navigate to `/apply`, submit a `STUDENT` visa arriving at `Chennai`.
2. **Review & Approve:** Log in at `/login` as `visaofficer01` / `password123`, navigate to `/visa-officer`, and approve the application to issue the ETA.
3. **Post-Arrival FRRO Registration:** Navigate to `/register`, enter the Visa Application ID and `T Nagar, Chennai`.
4. **Officer Review & Verification:** Log in as `frroofficer01` / `password123`, open `/frro`, view the assigned appointment, open the Leaflet OpenStreetMap view, and click **VERIFY**.
5. **WSDL Verification:**
   * Registry: `http://<VM-PUBLIC-IP>:8080/ws/registry.wsdl`
   * Visa Server: `http://<VM-PUBLIC-IP>:8081/ws/visa.wsdl`
   * Registration Server: `http://<VM-PUBLIC-IP>:8082/ws/registration.wsdl`
