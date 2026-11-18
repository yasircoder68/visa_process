#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "Starting Visa Registration System on Render"
echo "=========================================================="

# 0. Tune MySQL for low-memory container environment (< 40MB RAM)
mkdir -p /etc/mysql/conf.d
cat << 'EOF' > /etc/mysql/conf.d/low-memory.cnf
[mysqld]
performance_schema = OFF
innodb_buffer_pool_size = 16M
innodb_log_buffer_size = 1M
max_connections = 25
table_open_cache = 64
key_buffer_size = 8M
EOF

# 1. Start MySQL 8 service
echo "[1/5] Starting MySQL service..."
service mysql start

# Wait for MySQL to be ready
until mysqladmin ping --silent; do
    echo "  -> Waiting for MySQL..."
    sleep 1
done

DB_NAME="${DB_NAME:-visareg}"
DB_USER="${DB_USERNAME:-visareg}"
DB_PASS="${DB_PASSWORD:-visareg123}"

echo "[2/5] Initializing Database & executing schema / seed..."
mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"

mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < /app/db/schema.sql
mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < /app/db/seed.sql
echo "  -> Database initialized."

# Optimized JVM flags: SerialGC + small thread stack (256k)
BASE_JVM_OPTS="-XX:+UseSerialGC -Xss256k"

# 2. Start Service Registry (8080)
echo "[3/5] Starting Service Registry on port 8080..."
java -Xms24m -Xmx64m $BASE_JVM_OPTS -jar /app/jars/registry-server.jar > /tmp/registry.log 2>&1 &
REGISTRY_PID=$!

until curl -s http://localhost:8080/ws/registry.wsdl > /dev/null; do
    sleep 1
done
echo "  -> Registry Server is up!"

# 3. Start Visa Server (8081)
echo "[4/5] Starting Visa Application Server on port 8081..."
java -Xms32m -Xmx80m $BASE_JVM_OPTS -jar /app/jars/visa-server.jar > /tmp/visa.log 2>&1 &
VISA_PID=$!

until curl -s http://localhost:8081/ws/visa.wsdl > /dev/null; do
    sleep 1
done
echo "  -> Visa Application Server is up and registered!"

# 4. Start Registration Server (8082)
echo "[5/5] Starting FRRO Registration Server on port 8082..."
java -Xms32m -Xmx80m $BASE_JVM_OPTS -jar /app/jars/registration-server.jar > /tmp/registration.log 2>&1 &
REG_PID=$!

until curl -s http://localhost:8082/ws/registration.wsdl > /dev/null; do
    sleep 1
done
echo "  -> Registration Server is up and registered!"

# 5. Start Web Dashboard in foreground (Render binds to $PORT)
TARGET_PORT="${PORT:-3000}"
echo "=========================================================="
echo "All backend services ready! Launching Web Dashboard on port $TARGET_PORT..."
echo "=========================================================="

export PORT="$TARGET_PORT"
exec java -Xms36m -Xmx80m $BASE_JVM_OPTS -jar /app/jars/web-dashboard.jar
