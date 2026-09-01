package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.AddressRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
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
    public List<AddressEntity> findByUser(String userEmail) {
        return addressRepository.findByUserId(getUserId(userEmail));
    }

    @Override
    public AddressEntity findByUserAndId(String userEmail, Long addressId) {
        Long userId = getUserId(userEmail);
        return addressRepository.findById(addressId)
                .filter(address -> userId.equals(address.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    @Override
    @Transactional
    public AddressEntity create(String userEmail, AddressEntity address) {
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

    private void validate(AddressEntity address) {
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
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
