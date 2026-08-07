package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.ChangePasswordRequest;
import com.unibank.bankingSystem.dto.UpdateProfileRequest;
import com.unibank.bankingSystem.dto.UserProfileResponse;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers KYC-style profile validation (postal code format, minimum age) and the
// change-password flow (must confirm current password, must meet length requirement).
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFullName("Original Name");
        user.setPassword("hashed-old-password");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@example.com");

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
    }

    private UpdateProfileRequest validRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Mansi Kanani");
        request.setPhoneNumber("416-555-0123");
        request.setDateOfBirth(LocalDate.now().minusYears(25));
        request.setAddressLine1("123 Main St");
        request.setCity("Toronto");
        request.setProvince("ON");
        request.setPostalCode("K1A 0B1");
        return request;
    }

    @Test
    void updateProfile_rejectsDateOfBirthInTheFuture() {
        UpdateProfileRequest request = validRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void updateProfile_rejectsUnderEighteen() {
        UpdateProfileRequest request = validRequest();
        request.setDateOfBirth(LocalDate.now().minusYears(16));

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("18");
    }

    @Test
    void updateProfile_rejectsInvalidPostalCode() {
        UpdateProfileRequest request = validRequest();
        request.setPostalCode("not-a-postal-code");

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("postal code");
    }

    @Test
    void updateProfile_acceptsPostalCodeWithOrWithoutSpace() {
        UpdateProfileRequest request = validRequest();
        request.setPostalCode("K1A0B1"); // no space - should still be valid

        UserProfileResponse response = userService.updateProfile(request);

        assertThat(response.getPostalCode()).isEqualTo("K1A0B1");
    }

    @Test
    void updateProfile_savesAllFieldsWhenValid() {
        UpdateProfileRequest request = validRequest();

        UserProfileResponse response = userService.updateProfile(request);

        assertThat(response.getFullName()).isEqualTo("Mansi Kanani");
        assertThat(response.getPhoneNumber()).isEqualTo("416-555-0123");
        assertThat(response.getCity()).isEqualTo("Toronto");
        assertThat(response.getProvince()).isEqualTo("ON");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        when(passwordEncoder.matches("wrong-password", "hashed-old-password")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void changePassword_rejectsTooShortNewPassword() {
        when(passwordEncoder.matches("correct-password", "hashed-old-password")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("correct-password");
        request.setNewPassword("short");

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void changePassword_updatesHash_whenCurrentPasswordCorrect() {
        when(passwordEncoder.matches("correct-password", "hashed-old-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("hashed-new-password");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("correct-password");
        request.setNewPassword("NewPassword1!");

        userService.changePassword(request);

        assertThat(user.getPassword()).isEqualTo("hashed-new-password");
        verify(userRepository).save(user);
    }
}
