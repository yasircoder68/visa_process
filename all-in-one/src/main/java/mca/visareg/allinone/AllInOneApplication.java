package mca.visareg.allinone;

import org.springframework.boot.builder.SpringApplicationBuilder;
import mca.visareg.registry.RegistryServerApplication;
import mca.visareg.visa.VisaServerApplication;
import mca.visareg.registration.RegistrationServerApplication;
import mca.visareg.dashboard.DashboardApplication;

public class AllInOneApplication {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("Starting Visa Registration System (All-in-One, 1 JVM Mode)");
        System.out.println("==========================================================");

        String dbUrl = "jdbc:mysql://localhost:3306/visareg?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";
        String dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null || dbUser.isBlank()) dbUser = "visareg";
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbPass == null || dbPass.isBlank()) dbPass = "visareg123";

        // 1. Registry Server on port 8080
        new Thread(() -> {
            try {
                new SpringApplicationBuilder(RegistryServerApplication.class)
                        .run(
                                "--server.port=8080",
                                "--spring.application.name=registry-server",
                                "--spring.webservices.path=/ws",
                                "--spring.jmx.enabled=false"
                        );
            } catch (Exception e) {
                System.err.println("Registry Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "registry-srv").start();

        waitForUrl("http://localhost:8080/ws/registry.wsdl", "Registry Server (8080)");

        // 2. Visa Application Server on port 8081
        final String user = dbUser;
        final String pass = dbPass;
        new Thread(() -> {
            try {
                new SpringApplicationBuilder(VisaServerApplication.class)
                        .run(
                                "--server.port=8081",
                                "--spring.application.name=visa-server",
                                "--spring.webservices.path=/ws",
                                "--spring.datasource.url=" + dbUrl,
                                "--spring.datasource.username=" + user,
                                "--spring.datasource.password=" + pass,
                                "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                                "--spring.jpa.hibernate.ddl-auto=validate",
                                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                                "--registry.url=http://localhost:8080/ws",
                                "--app.public-url=http://localhost:8081/ws",
                                "--spring.jmx.enabled=false"
                        );
            } catch (Exception e) {
                System.err.println("Visa Application Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "visa-srv").start();

        waitForUrl("http://localhost:8081/ws/visa.wsdl", "Visa Application Server (8081)");

        // 3. FRRO Registration Server on port 8082
        String nominatimEmail = System.getenv("NOMINATIM_EMAIL");
        if (nominatimEmail == null || nominatimEmail.isBlank()) nominatimEmail = "visareg-student@example.com";
        String holidayKey = System.getenv("HOLIDAY_API_KEY");
        if (holidayKey == null) holidayKey = "";

        final String nomEmail = nominatimEmail;
        final String holKey = holidayKey;
        new Thread(() -> {
            try {
                new SpringApplicationBuilder(RegistrationServerApplication.class)
                        .run(
                                "--server.port=8082",
                                "--spring.application.name=registration-server",
                                "--spring.webservices.path=/ws",
                                "--spring.datasource.url=" + dbUrl,
                                "--spring.datasource.username=" + user,
                                "--spring.datasource.password=" + pass,
                                "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                                "--spring.jpa.hibernate.ddl-auto=validate",
                                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                                "--nominatim.email=" + nomEmail,
                                "--holiday.api.key=" + holKey,
                                "--registry.url=http://localhost:8080/ws",
                                "--app.public-url=http://localhost:8082/ws",
                                "--spring.jmx.enabled=false"
                        );
            } catch (Exception e) {
                System.err.println("FRRO Registration Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "reg-srv").start();

        waitForUrl("http://localhost:8082/ws/registration.wsdl", "FRRO Registration Server (8082)");

        // 4. Web Dashboard on Render $PORT (e.g. 10000 or 3000)
        String port = System.getenv("PORT");
        if (port == null || port.isBlank()) {
            port = "3000";
        }
        System.out.println("==========================================================");
        System.out.println("Launching Web Dashboard on port " + port + "...");
        System.out.println("==========================================================");
        new SpringApplicationBuilder(DashboardApplication.class)
                .run(
                        "--server.port=" + port,
                        "--spring.application.name=web-dashboard",
                        "--spring.datasource.url=" + dbUrl,
                        "--spring.datasource.username=" + user,
                        "--spring.datasource.password=" + pass,
                        "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--registry.url=http://localhost:8080/ws",
                        "--spring.thymeleaf.cache=false",
                        "--spring.jmx.enabled=false"
                );
    }

    private static void waitForUrl(String urlStr, String name) {
        System.out.println("Waiting for " + name + " at " + urlStr + "...");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 90000) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URI(urlStr).toURL().openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                int code = conn.getResponseCode();
                if (code >= 200 && code < 400) {
                    System.out.println("  -> " + name + " is READY (HTTP " + code + ")!");
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.err.println("WARNING: " + name + " did not respond within 90s at " + urlStr);
    }
}
