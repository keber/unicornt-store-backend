package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressCreateRequest;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressResponse;

/** Translation between address entities and address DTOs. */
public final class AddressMapper {

    private AddressMapper() {
    }

    /** Builds an unsaved entity; the owning user and the default flag are set by the service. */
    public static AddressEntity toEntity(AddressCreateRequest request) {
        AddressEntity address = new AddressEntity();
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setRegion(request.region());
        address.setZipCode(request.zipCode());
        return address;
    }

    public static AddressResponse toResponse(AddressEntity address) {
        return new AddressResponse(address.getId(), address.getStreet(), address.getCity(),
                address.getRegion(), address.getZipCode(), address.isDefault(), address.getFullAddress());
    }
}
