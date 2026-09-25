package mca.visareg.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "application_id", nullable = false, length = 20)
    private String applicationId;

    @Column(name = "address", nullable = false, length = 150)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "district", length = 80)
    private String district;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "assigned_officer", length = 20)
    private String assignedOfficer;

    public Registration() {
    }

    public Registration(String id, String applicationId, String address, String district, String status) {
        this.id = id;
        this.applicationId = applicationId;
        this.address = address;
        this.district = district;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedOfficer() {
        return assignedOfficer;
    }

    public void setAssignedOfficer(String assignedOfficer) {
        this.assignedOfficer = assignedOfficer;
    }
}
