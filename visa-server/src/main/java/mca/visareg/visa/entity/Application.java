package mca.visareg.visa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "passport_number", nullable = false, length = 20)
    private String passportNumber;

    @Column(name = "visa_type", nullable = false, length = 20)
    private String visaType;

    @Column(name = "port_of_arrival", length = 50)
    private String portOfArrival;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "eta_number", length = 20)
    private String etaNumber;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    public Application() {
    }

    public Application(String id, String applicantName, String passportNumber, String visaType, String portOfArrival, String status) {
        this.id = id;
        this.applicantName = applicantName;
        this.passportNumber = passportNumber;
        this.visaType = visaType;
        this.portOfArrival = portOfArrival;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getVisaType() {
        return visaType;
    }

    public void setVisaType(String visaType) {
        this.visaType = visaType;
    }

    public String getPortOfArrival() {
        return portOfArrival;
    }

    public void setPortOfArrival(String portOfArrival) {
        this.portOfArrival = portOfArrival;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEtaNumber() {
        return etaNumber;
    }

    public void setEtaNumber(String etaNumber) {
        this.etaNumber = etaNumber;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
}
