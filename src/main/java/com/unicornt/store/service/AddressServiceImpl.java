package com.unicornt.store.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.model.Address;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.AddressRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Address> findByUser(String userEmail) {
        return addressRepository.findByUserId(getUserId(userEmail));
    }

    @Override
    public Address findByUserAndId(String userEmail, Long addressId) {
        Long userId = getUserId(userEmail);
        return addressRepository.findById(addressId)
                .filter(address -> userId.equals(address.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    @Override
    @Transactional
    public Address create(String userEmail, Address address) {
        Long userId = getUserId(userEmail);
        validate(address);
        address.setId(null);
        address.setUserId(userId);
        address.setDefault(addressRepository.findByUserId(userId).isEmpty());
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public void delete(String userEmail, Long addressId) {
        addressRepository.delete(findByUserAndId(userEmail, addressId));
    }

    private void validate(Address address) {
        if (address.getStreet() == null || address.getStreet().isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (address.getCity() == null || address.getCity().isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (address.getRegion() == null || address.getRegion().isBlank()) {
            throw new IllegalArgumentException("Region is required");
        }
    }

    private Long getUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
