package mca.visareg.registration.service;

import mca.visareg.contracts.registration.RegistrationInfo;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.registration.client.VisaClient;
import mca.visareg.registration.entity.Officer;
import mca.visareg.registration.entity.Registration;
import mca.visareg.registration.entity.RegistrationStatus;
import mca.visareg.registration.exception.RegistrationBusinessException;
import mca.visareg.registration.repository.OfficerRepository;
import mca.visareg.registration.repository.RegistrationRepository;
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
public class RegistrationService {

    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private final RegistrationRepository registrationRepository;
    private final OfficerRepository officerRepository;
    private final VisaClient visaClient;

    public RegistrationService(RegistrationRepository registrationRepository,
                               OfficerRepository officerRepository,
                               VisaClient visaClient) {
        this.registrationRepository = registrationRepository;
        this.officerRepository = officerRepository;
        this.visaClient = visaClient;
    }

    @Transactional
    public Registration createRegistration(String applicationId, String address) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new RegistrationBusinessException("Application ID is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new RegistrationBusinessException("Address is required");
        }

        // 1. Verify Visa status and validity via SOAP before saving
        ApplicationInfo visaApp = visaClient.getApplication(applicationId.trim());
        validateVisaApplication(visaApp);

        // 2. Generate Registration ID
        String nextId = generateNextId();

        // 3. For Phase 4, use placeholder district until Nominatim integration in Phase 5
        String district = "Chennai";

        Registration reg = new Registration(nextId, applicationId.trim(), address.trim(), district, RegistrationStatus.DOCS_VERIFIED.name());
        return registrationRepository.save(reg);
    }

    @Transactional
    public Registration verifyDocuments(String registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationBusinessException("Registration not found: " + registrationId));

        ApplicationInfo visaApp = visaClient.getApplication(reg.getApplicationId());
        validateVisaApplication(visaApp);

        reg.setStatus(RegistrationStatus.DOCS_VERIFIED.name());
        return registrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public Registration getRegistration(String id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RegistrationBusinessException("Registration not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Registration> listRegistrations(String officerId) {
        if (officerId != null && !officerId.trim().isEmpty()) {
            return registrationRepository.findByAssignedOfficer(officerId.trim());
        }
        return registrationRepository.findAll();
    }

    public RegistrationInfo toDto(Registration reg) {
        RegistrationInfo info = new RegistrationInfo();
        info.setId(reg.getId());
        info.setApplicationId(reg.getApplicationId());
        info.setAddress(reg.getAddress());
        info.setLatitude(reg.getLatitude());
        info.setLongitude(reg.getLongitude());
        info.setDistrict(reg.getDistrict());
        info.setStatus(reg.getStatus());
        info.setAssignedOfficer(reg.getAssignedOfficer());

        if (reg.getAppointmentDate() != null) {
            info.setAppointmentDate(toXmlDate(reg.getAppointmentDate()));
        }

        if (reg.getAssignedOfficer() != null) {
            officerRepository.findById(reg.getAssignedOfficer())
                    .ifPresent(officer -> info.setOfficerName(officer.getName()));
        }

        return info;
    }

    private void validateVisaApplication(ApplicationInfo visaApp) {
        if (visaApp == null) {
            throw new RegistrationBusinessException("Visa application details could not be retrieved");
        }

        if (!"APPROVED".equalsIgnoreCase(visaApp.getStatus())) {
            throw new RegistrationBusinessException("Linked visa application is not APPROVED. Current status: " + visaApp.getStatus());
        }

        XMLGregorianCalendar validUntilXml = visaApp.getValidUntil();
        if (validUntilXml == null) {
            throw new RegistrationBusinessException("Visa application does not have a validity date");
        }

        LocalDate validUntil = LocalDate.of(
                validUntilXml.getYear(),
                validUntilXml.getMonth(),
                validUntilXml.getDay()
        );

        LocalDate today = LocalDate.now(KOLKATA_ZONE);
        if (validUntil.isBefore(today)) {
            throw new RegistrationBusinessException("Visa has expired on " + validUntil + ". Current date: " + today);
        }
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
            throw new RuntimeException("Error converting date", e);
        }
    }

    private String generateNextId() {
        Optional<Registration> top = registrationRepository.findTopByOrderByIdDesc();
        if (top.isEmpty()) {
            return "REG001";
        }
        String lastId = top.get().getId();
        if (lastId != null && lastId.startsWith("REG")) {
            try {
                int num = Integer.parseInt(lastId.substring(3));
                return String.format("REG%03d", num + 1);
            } catch (NumberFormatException ignored) {
            }
        }
        long count = registrationRepository.count();
        return String.format("REG%03d", count + 1);
    }
}
