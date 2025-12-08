package com.gdn.project.waroenk.cart.client;

import com.gdn.project.waroenk.cart.entity.AddressSnapshot;
import com.gdn.project.waroenk.common.Id;
import com.gdn.project.waroenk.member.AddressData;
import com.gdn.project.waroenk.member.AddressServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * gRPC client for communicating with the Member microservice.
 * Handles address retrieval for checkout finalization.
 */
@Slf4j
@Component
public class MemberGrpcClient {

    @GrpcClient("member-service")
    private AddressServiceGrpc.AddressServiceBlockingStub addressStub;

    /**
     * Get address by ID and convert to AddressSnapshot
     */
    public Optional<AddressSnapshot> getAddressById(String addressId) {
        try {
            Id request = Id.newBuilder()
                    .setValue(addressId)
                    .build();
            AddressData response = addressStub.findAddressById(request);
            return Optional.of(toAddressSnapshot(response));
        } catch (StatusRuntimeException e) {
            log.warn("Failed to get address by id {}: {}", addressId, e.getStatus());
            return Optional.empty();
        }
    }

    /**
     * Convert AddressData from member service to AddressSnapshot for checkout
     */
    private AddressSnapshot toAddressSnapshot(AddressData data) {
        return AddressSnapshot.builder()
                .recipientName(data.getLabel()) // Using label as recipient name
                .phone(null) // Phone not in AddressData, would need user info
                .street(data.getStreet())
                .city(data.getCity())
                .province(data.getProvince())
                .district(data.getDistrict())
                .subDistrict(data.getSubDistrict())
                .country(data.getCountry())
                .postalCode(data.getPostalCode())
                .notes(data.getDetails())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .build();
    }
}

