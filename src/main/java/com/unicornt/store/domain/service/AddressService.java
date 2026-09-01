package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;

import java.util.List;

/** Shipping address use cases, scoped to the owning user. */
public interface AddressService {

    List<AddressEntity> findByUser(String userEmail);

    /**
     * Returns one address, only if it belongs to the given user.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException otherwise
     */
    AddressEntity findByUserAndId(String userEmail, Long addressId);

    /** Registers an address; the first address of a user becomes the default one. */
    AddressEntity create(String userEmail, AddressEntity address);

    void delete(String userEmail, Long addressId);
}
