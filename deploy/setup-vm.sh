#!/usr/bin/env bash
# ====================================================================
# Visa Registration System — Azure Ubuntu VM Automated Setup Script
# Target OS: Ubuntu 24.04 LTS / 22.04 LTS (Azure VM)
# Usage: sudo bash setup-vm.sh
# ====================================================================

set -euo pipefail

echo "=========================================================="
echo "  Visa Registration System — VM Environment Setup"
echo "=========================================================="

if [ "$EUID" -ne 0 ]; then
  echo "Error: Please run as root (sudo bash setup-vm.sh)" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# 1. Update packages and install dependencies
echo "[1/6] Updating apt packages and installing OpenJDK 21 & MySQL Server..."
apt update -y
apt install -y openjdk-21-jre-headless mysql-server curl ufw

# 2. Start and enable MySQL service
echo "[2/6] Ensuring MySQL service is active..."
systemctl enable --now mysql

# 3. Create Database & User
DB_NAME="visareg"
DB_USER="visareg"
DB_PASS="visareg123"

if [ -f "/opt/visareg/env" ]; then
  # Load DB password from env if exists
  source /opt/visareg/env || true
  DB_PASS="${DB_PASSWORD:-$DB_PASS}"
fi

echo "[3/6] Setting up MySQL database and credentials..."
mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"

# 4. Import Schema & Seed Data
echo "[4/6] Executing schema.sql and seed.sql..."
if [ -f "${ROOT_DIR}/db/schema.sql" ]; then
  mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "${ROOT_DIR}/db/schema.sql"
elif [ -f "/opt/visareg/schema.sql" ]; then
  mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "/opt/visareg/schema.sql"
fi

if [ -f "${ROOT_DIR}/db/seed.sql" ]; then
  mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "${ROOT_DIR}/db/seed.sql"
elif [ -f "/opt/visareg/seed.sql" ]; then
  mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "/opt/visareg/seed.sql"
fi

# 5. Create deployment directory & copy environment template
echo "[5/6] Setting up /opt/visareg directory..."
mkdir -p /opt/visareg
TARGET_USER="${SUDO_USER:-azureuser}"
chown -R "${TARGET_USER}:${TARGET_USER}" /opt/visareg

if [ ! -f "/opt/visareg/env" ] && [ -f "${SCRIPT_DIR}/env.template" ]; then
  cp "${SCRIPT_DIR}/env.template" /opt/visareg/env
  sed -i "s/your_secure_db_password_here/${DB_PASS}/g" /opt/visareg/env
  chmod 600 /opt/visareg/env
  chown "${TARGET_USER}:${TARGET_USER}" /opt/visareg/env
  echo "  -> Created /opt/visareg/env template (chmod 600)"
fi

# 6. Install systemd services
echo "[6/6] Installing systemd unit files..."
cp -f "${SCRIPT_DIR}/systemd/"*.service /etc/systemd/system/
systemctl daemon-reload

echo "=========================================================="
echo "  Setup Complete!"
echo "=========================================================="
echo "Next steps:"
echo "1. Place JAR files in /opt/visareg/:"
echo "   - registry-server.jar"
echo "   - visa-server.jar"
echo "   - registration-server.jar"
echo "   - web-dashboard.jar"
echo "2. Edit /opt/visareg/env and update APP_PUBLIC_HOST with your VM public IP."
echo "3. Start all services:"
echo "   sudo systemctl enable --now visareg-registry visareg-visa visareg-registration visareg-dashboard"
echo "4. Access the dashboard at http://<VM-PUBLIC-IP>:3000"
echo "=========================================================="
