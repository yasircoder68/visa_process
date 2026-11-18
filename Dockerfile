# ====================================================================
# Visa Registration System — Render Dockerfile (Option A: Single Web Service)
# Runs MySQL 8 + Registry (8080) + Visa (8081) + Registration (8082) + Dashboard ($PORT)
# ====================================================================

FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Kolkata

# Install OpenJDK 21, MySQL Server, curl, procps
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    mysql-server \
    curl \
    procps \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy database schema and seed files
COPY db/ /app/db/

# Copy pre-built application JARs
COPY deploy/jars/ /app/jars/

# Copy entrypoint script
COPY deploy/render/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Default port (Render overrides with $PORT, e.g. 10000)
ENV PORT=10000
EXPOSE ${PORT}

ENTRYPOINT ["/app/entrypoint.sh"]
