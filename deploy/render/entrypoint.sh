#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "Starting Visa Registration System on Render"
echo "=========================================================="

# 0. Tune MySQL for low-memory container environment
mkdir -p /etc/mysql/mysql.conf.d
cat << 'EOF' > /etc/mysql/mysql.conf.d/low-memory.cnf
[mysqld]
performance_schema = OFF
innodb_buffer_pool_size = 16M
innodb_log_buffer_size = 1M
max_connections = 25
table_open_cache = 64
key_buffer_size = 8M
innodb_undo_log_truncate = OFF
EOF

# 1. Start MySQL 8 service
echo "[1/2] Starting MySQL service..."
service mysql start

# Wait for MySQL to be ready
until mysqladmin ping --silent; do
    echo "  -> Waiting for MySQL..."
    sleep 1
done

DB_NAME="${DB_NAME:-visareg}"
DB_USER="${DB_USERNAME:-visareg}"
DB_PASS="${DB_PASSWORD:-visareg123}"

echo "Initializing Database & executing schema / seed..."
mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"

mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < /app/db/schema.sql
mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < /app/db/seed.sql
echo "  -> Database initialized."

# 2. Launch All-in-One Visa Registration System in 1 JVM
# Ports: 8080 (Registry), 8081 (Visa), 8082 (Registration), $PORT (Dashboard)
# SerialGC + TieredStopAtLevel=1 + Xmx220m total heap fits easily in 512MB RAM
TARGET_PORT="${PORT:-10000}"
export PORT="$TARGET_PORT"
echo "[2/2] Launching All-in-One Visa Registration System in 1 JVM (Dashboard on port $TARGET_PORT)..."
exec java -Xms64m -Xmx220m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -jar /app/jars/all-in-one.jar
