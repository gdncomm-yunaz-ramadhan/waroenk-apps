package com.gdn.project.waroenk.gateway.repository;

import com.gdn.project.waroenk.gateway.entity.ServiceRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRegistryRepository extends JpaRepository<ServiceRegistryEntity, UUID> {

    Optional<ServiceRegistryEntity> findByName(String name);

    List<ServiceRegistryEntity> findByActiveTrue();

    @Query("SELECT DISTINCT s FROM ServiceRegistryEntity s LEFT JOIN FETCH s.routes WHERE s.active = true")
    List<ServiceRegistryEntity> findByActiveTrueWithRoutes();

    Optional<ServiceRegistryEntity> findByNameAndActiveTrue(String name);

    @Query("SELECT s FROM ServiceRegistryEntity s WHERE s.active = true AND s.lastHeartbeat < :threshold")
    List<ServiceRegistryEntity> findStaleServices(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE ServiceRegistryEntity s SET s.lastHeartbeat = :timestamp WHERE s.name = :serviceName")
    int updateHeartbeat(@Param("serviceName") String serviceName, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE ServiceRegistryEntity s SET s.active = false WHERE s.name = :serviceName")
    int deactivateService(@Param("serviceName") String serviceName);

    @Modifying
    @Query("UPDATE ServiceRegistryEntity s SET s.active = false WHERE s.lastHeartbeat < :threshold")
    int deactivateStaleServices(@Param("threshold") LocalDateTime threshold);

    boolean existsByName(String name);
}


