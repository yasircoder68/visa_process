package mca.visareg.registration.repository;

import mca.visareg.registration.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {

    Optional<Registration> findTopByOrderByIdDesc();

    List<Registration> findByAssignedOfficer(String assignedOfficer);

    Optional<Registration> findByApplicationId(String applicationId);
}
