package mca.visareg.registration.service;

import mca.visareg.registration.entity.Officer;
import mca.visareg.registration.entity.OfficerStatus;
import mca.visareg.registration.entity.OfficerType;
import mca.visareg.registration.entity.Registration;
import mca.visareg.registration.entity.RegistrationStatus;
import mca.visareg.registration.exception.RegistrationBusinessException;
import mca.visareg.registration.geocoding.GeocodingResult;
import mca.visareg.registration.repository.OfficerRepository;
import mca.visareg.registration.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RegistrationTxService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationTxService.class);

    private final RegistrationRepository registrationRepository;
    private final OfficerRepository officerRepository;

    public RegistrationTxService(RegistrationRepository registrationRepository, OfficerRepository officerRepository) {
        this.registrationRepository = registrationRepository;
        this.officerRepository = officerRepository;
    }

    @Transactional
    public Registration persistRegistrationWithOfficer(String applicationId,
                                                       String address,
                                                       GeocodingResult geo,
                                                       LocalDate appointmentDate,
                                                       String district) {
        Officer claimedOfficer = claimAvailableOfficer(district);

        String nextId = generateNextId();
        Registration reg = new Registration();
        reg.setId(nextId);
        reg.setApplicationId(applicationId);
        reg.setAddress(address);
        reg.setLatitude(geo.latitude());
        reg.setLongitude(geo.longitude());
        reg.setDistrict(district);
        reg.setAppointmentDate(appointmentDate);
        reg.setAssignedOfficer(claimedOfficer.getId());
        reg.setStatus(RegistrationStatus.APPOINTMENT_SCHEDULED.name());

        log.info("Created registration '{}': district='{}', officer='{}', appointment='{}'",
                nextId, district, claimedOfficer.getId(), appointmentDate);

        return registrationRepository.save(reg);
    }

    @Transactional
    public Registration scheduleAppointment(Registration reg, LocalDate appointmentDate) {
        String district = reg.getDistrict() != null ? reg.getDistrict() : "Chennai";
        Officer claimedOfficer = claimAvailableOfficer(district);

        reg.setAssignedOfficer(claimedOfficer.getId());
        reg.setAppointmentDate(appointmentDate);
        reg.setStatus(RegistrationStatus.APPOINTMENT_SCHEDULED.name());

        return registrationRepository.save(reg);
    }

    @Transactional
    public Registration completeRegistration(Registration reg) {
        if (reg.getAssignedOfficer() != null) {
            officerRepository.releaseOfficer(reg.getAssignedOfficer());
            log.info("Released officer '{}' back to AVAILABLE", reg.getAssignedOfficer());
        }

        reg.setStatus(RegistrationStatus.REGISTERED.name());
        return registrationRepository.save(reg);
    }

    @Transactional
    public Registration saveRegistration(Registration reg) {
        return registrationRepository.save(reg);
    }

    public Officer claimAvailableOfficer(String district) {
        List<Officer> candidates = officerRepository.findByJurisdictionAndTypeAndStatus(
                district,
                OfficerType.FRRO_OFFICER.name(),
                OfficerStatus.AVAILABLE.name()
        );

        if (candidates == null || candidates.isEmpty()) {
            throw new RegistrationBusinessException("No FRRO officer available in " + district);
        }

        for (Officer candidate : candidates) {
            int claimed = officerRepository.claimOfficer(candidate.getId());
            if (claimed > 0) {
                log.info("Claimed officer '{}' ({}) in district '{}'", candidate.getId(), candidate.getName(), district);
                return candidate;
            }
        }

        throw new RegistrationBusinessException("No FRRO officer available in " + district);
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
