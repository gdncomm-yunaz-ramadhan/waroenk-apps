package com.gdn.project.waroenk.gateway.repository;

import com.gdn.project.waroenk.gateway.entity.RouteRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRegistryRepository extends JpaRepository<RouteRegistryEntity, UUID> {

    @Query("SELECT r FROM RouteRegistryEntity r JOIN FETCH r.service s WHERE s.active = true")
    List<RouteRegistryEntity> findAllActiveRoutes();

    @Query("SELECT r FROM RouteRegistryEntity r WHERE r.httpMethod = :method AND r.path = :path")
    Optional<RouteRegistryEntity> findByHttpMethodAndPath(
            @Param("method") String httpMethod,
            @Param("path") String path);

    @Query("SELECT r FROM RouteRegistryEntity r JOIN r.service s WHERE s.name = :serviceName")
    List<RouteRegistryEntity> findByServiceName(@Param("serviceName") String serviceName);

    @Query("SELECT r.routeHash FROM RouteRegistryEntity r JOIN r.service s WHERE s.name = :serviceName")
    List<String> findRouteHashesByServiceName(@Param("serviceName") String serviceName);

    @Query("SELECT r FROM RouteRegistryEntity r WHERE r.routeHash IN :hashes")
    List<RouteRegistryEntity> findByRouteHashIn(@Param("hashes") List<String> hashes);

    @Modifying
    @Query("DELETE FROM RouteRegistryEntity r WHERE r.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") UUID serviceId);

    @Modifying
    @Query("DELETE FROM RouteRegistryEntity r WHERE r.service.name = :serviceName")
    void deleteByServiceName(@Param("serviceName") String serviceName);

    boolean existsByHttpMethodAndPath(String httpMethod, String path);

    @Query("SELECT COUNT(r) FROM RouteRegistryEntity r JOIN r.service s WHERE s.active = true")
    long countActiveRoutes();
}










