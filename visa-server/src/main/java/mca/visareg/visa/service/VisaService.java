package mca.visareg.visa.service;

import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.visa.entity.Application;
import mca.visareg.visa.entity.ApplicationStatus;
import mca.visareg.visa.exception.VisaBusinessException;
import mca.visareg.visa.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class VisaService {

    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private final ApplicationRepository applicationRepository;

    public VisaService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public synchronized Application submitApplication(String applicantName, String passportNumber, String visaType, String portOfArrival) {
        if (applicantName == null || applicantName.trim().isEmpty()) {
            throw new VisaBusinessException("Applicant name is required");
        }
        if (passportNumber == null || passportNumber.trim().isEmpty()) {
            throw new VisaBusinessException("Passport number is required");
        }
        if (visaType == null || visaType.trim().isEmpty()) {
            throw new VisaBusinessException("Visa type is required");
        }

        String nextId = generateNextId();
        Application application = new Application(
                nextId,
                applicantName.trim(),
                passportNumber.trim(),
                visaType.trim().toUpperCase(),
                portOfArrival != null ? portOfArrival.trim() : null,
                ApplicationStatus.SUBMITTED.name()
        );

        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public Application getApplication(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new VisaBusinessException("Application not found: " + id));
    }

    @Transactional
    public Application updateApplicationStatus(String id, String newStatusStr) {
        Application application = getApplication(id);

        if (newStatusStr == null) {
            throw new VisaBusinessException("Status cannot be null");
        }

        String targetStatus = newStatusStr.trim().toUpperCase();
        if (!ApplicationStatus.SUBMITTED.name().equals(application.getStatus())) {
            throw new VisaBusinessException("Cannot update application in status: " + application.getStatus() + ". Only SUBMITTED applications can be reviewed.");
        }

        if (!ApplicationStatus.APPROVED.name().equals(targetStatus) && !ApplicationStatus.REJECTED.name().equals(targetStatus)) {
            throw new VisaBusinessException("Invalid target status: " + targetStatus + ". Allowed transitions: APPROVED, REJECTED.");
        }

        application.setStatus(targetStatus);
        return applicationRepository.save(application);
    }

    @Transactional
    public Application issueETA(String id, Integer validDays) {
        Application application = getApplication(id);

        if (!ApplicationStatus.APPROVED.name().equals(application.getStatus())) {
            throw new VisaBusinessException("Cannot issue ETA for application with status: " + application.getStatus() + ". Must be APPROVED.");
        }

        int days = (validDays != null && validDays > 0) ? validDays : 180;
        LocalDate now = LocalDate.now(KOLKATA_ZONE);
        LocalDate validUntil = now.plusDays(days);

        application.setEtaNumber("ETA-" + application.getId());
        application.setValidUntil(validUntil);

        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<Application> listApplications(String status) {
        if (status != null && !status.trim().isEmpty()) {
            return applicationRepository.findByStatus(status.trim().toUpperCase());
        }
        return applicationRepository.findAll();
    }

    public ApplicationInfo toDto(Application app) {
        ApplicationInfo info = new ApplicationInfo();
        info.setId(app.getId());
        info.setApplicantName(app.getApplicantName());
        info.setPassportNumber(app.getPassportNumber());
        info.setVisaType(app.getVisaType());
        info.setPortOfArrival(app.getPortOfArrival());
        info.setStatus(app.getStatus());
        info.setEtaNumber(app.getEtaNumber());
        if (app.getValidUntil() != null) {
            info.setValidUntil(toXmlDate(app.getValidUntil()));
        }
        return info;
    }

    private XMLGregorianCalendar toXmlDate(LocalDate date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth(),
                    DatatypeConstants.FIELD_UNDEFINED
            );
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Error converting LocalDate to XMLGregorianCalendar", e);
        }
    }

    private String generateNextId() {
        Optional<Application> top = applicationRepository.findTopByOrderByIdDesc();
        if (top.isEmpty()) {
            return "VISA001";
        }
        String lastId = top.get().getId();
        if (lastId != null && lastId.startsWith("VISA")) {
            try {
                int num = Integer.parseInt(lastId.substring(4));
                return String.format("VISA%03d", num + 1);
            } catch (NumberFormatException ignored) {
            }
        }
        long count = applicationRepository.count();
        return String.format("VISA%03d", count + 1);
    }
}
