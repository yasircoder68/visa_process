package mca.visareg.registration.service;

import mca.visareg.contracts.registration.RegistrationInfo;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.registration.client.VisaClient;
import mca.visareg.registration.entity.Registration;
import mca.visareg.registration.entity.RegistrationStatus;
import mca.visareg.registration.exception.RegistrationBusinessException;
import mca.visareg.registration.geocoding.GeocodingResult;
import mca.visareg.registration.geocoding.GeocodingService;
import mca.visareg.registration.holiday.WorkingDayService;
import mca.visareg.registration.repository.OfficerRepository;
import mca.visareg.registration.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private final RegistrationRepository registrationRepository;
    private final OfficerRepository officerRepository;
    private final VisaClient visaClient;
    private final GeocodingService geocodingService;
    private final WorkingDayService workingDayService;
    private final RegistrationTxService registrationTxService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               OfficerRepository officerRepository,
                               VisaClient visaClient,
                               GeocodingService geocodingService,
                               WorkingDayService workingDayService,
                               RegistrationTxService registrationTxService) {
        this.registrationRepository = registrationRepository;
        this.officerRepository = officerRepository;
        this.visaClient = visaClient;
        this.geocodingService = geocodingService;
        this.workingDayService = workingDayService;
        this.registrationTxService = registrationTxService;
    }

    /**
     * Full orchestration sequence for createRegistration:
     * 1. Geocode address via Nominatim REST (outside transaction)
     * 2. Verify visa application via SOAP (outside transaction)
     * 3. Compute next working day appointment date (outside transaction)
     * 4. In @Transactional (via RegistrationTxService): claim AVAILABLE FRRO officer in district -> schedule appointment
     */
    public Registration createRegistration(String applicationId, String address) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new RegistrationBusinessException("Application ID is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new RegistrationBusinessException("Address is required");
        }

        // 1. Geocode address (REST) — outside transaction
        GeocodingResult geo = geocodingService.resolve(address.trim());
        String district = geo.district();

        // 2. Verify visa (SOAP) — outside transaction
        ApplicationInfo visaApp = visaClient.getApplication(applicationId.trim());
        validateVisaApplication(visaApp);

        // 3. Compute appointment date (skipping weekends & holidays)
        LocalDate appointmentDate = workingDayService.nextWorkingDay(LocalDate.now(KOLKATA_ZONE));

        // 4. In Transaction: claim officer and persist registration
        return registrationTxService.persistRegistrationWithOfficer(applicationId.trim(), address.trim(), geo, appointmentDate, district);
    }

    public Registration verifyDocuments(String registrationId) {
        Registration reg = getRegistration(registrationId);

        ApplicationInfo visaApp = visaClient.getApplication(reg.getApplicationId());
        validateVisaApplication(visaApp);

        reg.setStatus(RegistrationStatus.DOCS_VERIFIED.name());
        return registrationTxService.saveRegistration(reg);
    }

    public Registration scheduleAppointment(String registrationId) {
        Registration reg = getRegistration(registrationId);

        if (!RegistrationStatus.DOCS_VERIFIED.name().equals(reg.getStatus())
                && !RegistrationStatus.SUBMITTED.name().equals(reg.getStatus())) {
            throw new RegistrationBusinessException("Cannot schedule appointment for registration in status: " + reg.getStatus());
        }

        LocalDate appointmentDate = workingDayService.nextWorkingDay(LocalDate.now(KOLKATA_ZONE));
        return registrationTxService.scheduleAppointment(reg, appointmentDate);
    }

    public Registration completeRegistration(String registrationId) {
        Registration reg = getRegistration(registrationId);

        if (!RegistrationStatus.APPOINTMENT_SCHEDULED.name().equals(reg.getStatus())) {
            throw new RegistrationBusinessException("Cannot complete registration with status: " + reg.getStatus() + ". Must be APPOINTMENT_SCHEDULED.");
        }

        return registrationTxService.completeRegistration(reg);
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
}
