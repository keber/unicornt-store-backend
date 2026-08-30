package com.unicornt.store.service;

import com.unicornt.store.model.Address;

import java.util.List;

/** Shipping address use cases, scoped to the owning user. */
public interface AddressService {

    List<Address> findByUser(String userEmail);

    /**
     * Returns one address, only if it belongs to the given user.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException otherwise
     */
    Address findByUserAndId(String userEmail, Long addressId);

    /** Registers an address; the first address of a user becomes the default one. */
    Address create(String userEmail, Address address);

    void delete(String userEmail, Long addressId);
}
