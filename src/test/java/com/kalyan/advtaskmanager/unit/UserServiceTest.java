package com.kalyan.advtaskmanager.unit;

import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.UserRepository;
import com.kalyan.advtaskmanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * UserServiceTest — UNIT TESTS (Mockito, no Spring context)
 *
 * Validates UserService business logic in complete isolation.
 * All dependencies (UserRepository, PasswordEncoder) are mocked.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Unit Tests")
class UserServiceTest {

    @Mock  UserRepository  userRepository;
    @Mock  PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("plain123")
                .role(Role.USER)
                .build();
    }

    // ─── TC-US-001 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-001 | createUser encodes plain-text password before saving")
    void createUser_encodesPasswordBeforeSave() {
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$encoded");
        when(userRepository.save(any())).thenReturn(sampleUser);

        userService.createUser(sampleUser);

        // Encoder must have been called once
        verify(passwordEncoder, times(1)).encode("plain123");
        verify(userRepository, times(1)).save(sampleUser);
    }

    // ─── TC-US-002 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-002 | createUser does NOT re-encode already BCrypt password")
    void createUser_skipsEncodingIfAlreadyBCrypt() {
        sampleUser.setPassword("$2a$10$alreadyHashed");
        when(userRepository.save(any())).thenReturn(sampleUser);

        userService.createUser(sampleUser);

        // Encoder must NOT be called — password is already hashed
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ─── TC-US-003 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-003 | getAllUsers returns list from repository")
    void getAllUsers_returnsRepositoryList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john@test.com");
    }

    // ─── TC-US-004 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-004 | getAllUsers returns empty list when no users exist")
    void getAllUsers_returnsEmptyListWhenNone() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    // ─── TC-US-005 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-005 | getUserById returns user for existing ID")
    void getUserById_returnsUserForValidId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john@test.com");
    }

    // ─── TC-US-006 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-006 | getUserById throws RuntimeException for non-existing ID")
    void getUserById_throwsForNonExistingId() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ─── TC-US-007 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-007 | deleteUser calls repository.deleteById for existing user")
    void deleteUser_callsDeleteForExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    // ─── TC-US-008 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-008 | deleteUser throws RuntimeException when user not found")
    void deleteUser_throwsWhenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");

        verify(userRepository, never()).deleteById(any());
    }

    // ─── TC-US-009 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-009 | deleteAllUsers delegates to repository.deleteAll")
    void deleteAllUsers_callsRepositoryDeleteAll() {
        userService.deleteAllUsers();

        verify(userRepository, times(1)).deleteAll();
    }

    // ─── TC-US-010 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-US-010 | createUser with null password does not throw NPE")
    void createUser_withNullPassword_doesNotCrash() {
        sampleUser.setPassword(null);
        when(userRepository.save(any())).thenReturn(sampleUser);

        // Should not throw — null check exists in service
        assertThatCode(() -> userService.createUser(sampleUser))
                .doesNotThrowAnyException();
    }
}

