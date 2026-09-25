package mca.visareg.visa.repository;

import mca.visareg.visa.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    List<Application> findByStatus(String status);
    Optional<Application> findTopByOrderByIdDesc();
}
