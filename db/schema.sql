-- Database Schema for Visa Registration System
-- MCA(SS) Cloud Computing Mini Project

CREATE TABLE IF NOT EXISTS officers (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    jurisdiction VARCHAR(80),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS applications (
    id VARCHAR(20) PRIMARY KEY,
    applicant_name VARCHAR(100) NOT NULL,
    passport_number VARCHAR(20) NOT NULL,
    visa_type VARCHAR(20) NOT NULL,
    port_of_arrival VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    eta_number VARCHAR(20),
    valid_until DATE
);

CREATE TABLE IF NOT EXISTS registrations (
    id VARCHAR(20) PRIMARY KEY,
    application_id VARCHAR(20) NOT NULL,
    address VARCHAR(150) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    district VARCHAR(80),
    appointment_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    assigned_officer VARCHAR(20),
    FOREIGN KEY (application_id) REFERENCES applications(id),
    FOREIGN KEY (assigned_officer) REFERENCES officers(id)
);

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    officer_id VARCHAR(20),
    FOREIGN KEY (officer_id) REFERENCES officers(id)
);
