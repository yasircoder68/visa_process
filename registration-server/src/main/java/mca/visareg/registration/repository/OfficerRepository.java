package mca.visareg.registration.repository;

import mca.visareg.registration.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, String> {

    List<Officer> findByJurisdictionAndTypeAndStatus(String jurisdiction, String type, String status);

    @Modifying
    @Query("UPDATE Officer o SET o.status = 'ASSIGNED' WHERE o.id = :id AND o.status = 'AVAILABLE'")
    int claimOfficer(@Param("id") String id);

    @Modifying
    @Query("UPDATE Officer o SET o.status = 'AVAILABLE' WHERE o.id = :id")
    int releaseOfficer(@Param("id") String id);
}
