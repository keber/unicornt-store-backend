package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.AddressRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test of the address business rules, with no Spring context. */
@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    private static final String USER_EMAIL = "buyer@unicornt.test";
    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    // --- object mothers ---------------------------------------------------

    private static UserEntity aUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(USER_EMAIL);
        return user;
    }

    private static AddressEntity anAddress() {
        AddressEntity address = new AddressEntity();
        address.setStreet("221B Baker Street");
        address.setCity("London");
        address.setRegion("Greater London");
        address.setZipCode("NW1 6XE");
        return address;
    }

    private void givenKnownUser() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(aUser(USER_ID)));
    }

    // --- findByUser -----------------------------------------------------------

    @Nested
    @DisplayName("findByUser")
    class FindByUser {

        @Test
        @DisplayName("delegates to the repository with the resolved user id")
        void delegatesToRepository() {
            givenKnownUser();
            List<AddressEntity> expected = List.of(anAddress(), anAddress());
            when(addressRepository.findByUserId(USER_ID)).thenReturn(expected);

            assertThat(addressService.findByUser(USER_EMAIL)).isSameAs(expected);
        }

        @Test
        @DisplayName("an unknown user raises ResourceNotFoundException")
        void unknownUserThrows() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.findByUser(USER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + USER_EMAIL);
        }
    }

    // --- findByUserAndId ----------------------------------------------------

    @Nested
    @DisplayName("findByUserAndId")
    class FindByUserAndId {

        @Test
        @DisplayName("returns the address when it belongs to the requesting user")
        void ownedAddressIsReturned() {
            givenKnownUser();
            AddressEntity address = anAddress();
            address.setId(3L);
            address.setUserId(USER_ID);
            when(addressRepository.findById(3L)).thenReturn(Optional.of(address));

            assertThat(addressService.findByUserAndId(USER_EMAIL, 3L)).isSameAs(address);
        }

        @Test
        @DisplayName("an address owned by another user raises ResourceNotFoundException")
        void addressOfAnotherUserThrows() {
            givenKnownUser();
            AddressEntity address = anAddress();
            address.setId(3L);
            address.setUserId(OTHER_USER_ID);
            when(addressRepository.findById(3L)).thenReturn(Optional.of(address));

            assertThatThrownBy(() -> addressService.findByUserAndId(USER_EMAIL, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Address not found: 3");
        }

        @Test
        @DisplayName("a missing address raises ResourceNotFoundException")
        void missingAddressThrows() {
            givenKnownUser();
            when(addressRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.findByUserAndId(USER_EMAIL, 404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Address not found: 404");
        }

        @Test
        @DisplayName("an unknown user raises ResourceNotFoundException")
        void unknownUserThrows() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.findByUserAndId(USER_EMAIL, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + USER_EMAIL);
        }
    }

    // --- create -----------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        static Stream<Arguments> invalidFields() {
            return Stream.of(
                    Arguments.of("street", null, "Street is required"),
                    Arguments.of("street", "   ", "Street is required"),
                    Arguments.of("city", null, "City is required"),
                    Arguments.of("city", "   ", "City is required"),
                    Arguments.of("region", null, "Region is required"),
                    Arguments.of("region", "   ", "Region is required")
            );
        }

        @ParameterizedTest(name = "{0} = [{1}] -> {2}")
        @MethodSource("invalidFields")
        @DisplayName("a null or blank required field raises IllegalArgumentException and never saves")
        void invalidFieldRejected(String field, String value, String expectedMessage) {
            givenKnownUser();
            AddressEntity address = anAddress();
            switch (field) {
                case "street" -> address.setStreet(value);
                case "city" -> address.setCity(value);
                case "region" -> address.setRegion(value);
                default -> throw new IllegalStateException("unexpected field " + field);
            }

            assertThatThrownBy(() -> addressService.create(USER_EMAIL, address))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage);

            verify(addressRepository, never()).save(any());
        }

        @Test
        @DisplayName("the first address of a user is stored as the default one")
        void firstAddressBecomesDefault() {
            givenKnownUser();
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(addressRepository.save(any(AddressEntity.class))).thenAnswer(call -> call.getArgument(0));

            AddressEntity result = addressService.create(USER_EMAIL, anAddress());

            assertThat(result.isDefault()).isTrue();
            assertThat(result.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("a subsequent address of a user is not the default one")
        void subsequentAddressIsNotDefault() {
            givenKnownUser();
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(anAddress()));
            when(addressRepository.save(any(AddressEntity.class))).thenAnswer(call -> call.getArgument(0));

            AddressEntity result = addressService.create(USER_EMAIL, anAddress());

            assertThat(result.isDefault()).isFalse();
        }

        @Test
        @DisplayName("any client-supplied id is discarded before persisting")
        void idIsForcedToNullBeforeSave() {
            givenKnownUser();
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(addressRepository.save(any(AddressEntity.class))).thenAnswer(call -> call.getArgument(0));

            AddressEntity address = anAddress();
            address.setId(555L);

            addressService.create(USER_EMAIL, address);

            ArgumentCaptor<AddressEntity> captor = ArgumentCaptor.forClass(AddressEntity.class);
            verify(addressRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
        }

        @Test
        @DisplayName("an unknown user raises ResourceNotFoundException and never saves")
        void unknownUserThrows() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.create(USER_EMAIL, anAddress()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + USER_EMAIL);

            verify(addressRepository, never()).save(any());
        }
    }

    // --- delete ---------------------------------------------------------------

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("resolves the owned address and hands it to the repository for removal")
        void deletesOwnedAddress() {
            givenKnownUser();
            AddressEntity address = anAddress();
            address.setId(3L);
            address.setUserId(USER_ID);
            when(addressRepository.findById(3L)).thenReturn(Optional.of(address));

            addressService.delete(USER_EMAIL, 3L);

            verify(addressRepository).delete(address);
        }

        @Test
        @DisplayName("an address owned by another user raises ResourceNotFoundException and never deletes")
        void addressOfAnotherUserThrows() {
            givenKnownUser();
            AddressEntity address = anAddress();
            address.setId(3L);
            address.setUserId(OTHER_USER_ID);
            when(addressRepository.findById(3L)).thenReturn(Optional.of(address));

            assertThatThrownBy(() -> addressService.delete(USER_EMAIL, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Address not found: 3");

            verify(addressRepository, never()).delete(any(AddressEntity.class));
        }

        @Test
        @DisplayName("a missing address raises ResourceNotFoundException and never deletes")
        void missingAddressThrows() {
            givenKnownUser();
            when(addressRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.delete(USER_EMAIL, 404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Address not found: 404");

            verify(addressRepository, never()).delete(any(AddressEntity.class));
        }

        @Test
        @DisplayName("an unknown user raises ResourceNotFoundException and never deletes")
        void unknownUserThrows() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.delete(USER_EMAIL, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + USER_EMAIL);

            verify(addressRepository, never()).delete(any(AddressEntity.class));
        }
    }
}
