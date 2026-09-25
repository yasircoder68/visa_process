-- Seed Data for Visa Registration System
-- Sample officers across districts (Chennai, Coimbatore, Madurai)

INSERT INTO officers (id, name, type, jurisdiction, status) VALUES
('VO01', 'Visa Officer 01', 'VISA_OFFICER', NULL, 'AVAILABLE'),
('FRRO01', 'Officer Ramesh', 'FRRO_OFFICER', 'Chennai', 'AVAILABLE'),
('FRRO02', 'Officer Priya', 'FRRO_OFFICER', 'Chennai', 'AVAILABLE'),
('FRRO03', 'Officer Suresh', 'FRRO_OFFICER', 'Coimbatore', 'AVAILABLE'),
('FRRO04', 'Officer Anitha', 'FRRO_OFFICER', 'Madurai', 'AVAILABLE')
ON DUPLICATE KEY UPDATE name=VALUES(name);
