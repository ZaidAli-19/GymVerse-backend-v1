package com.company.GymVerse.security;

import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Story 3.1: CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should successfully load master user with username and password, with no artificial roles")
    void loadUserByUsername_Success_ReturnsUserDetails() {
        final UserAccount account = UserAccount.builder()
                .id(1L)
                .email("user@gymverse.com")
                .password("$2a$10$hashedPassword123")
                .build();

        when(userAccountRepository.findByEmail("user@gymverse.com")).thenReturn(Optional.of(account));

        final UserDetails userDetails = userDetailsService.loadUserByUsername("user@gymverse.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user@gymverse.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword123");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getAuthorities()).isEmpty(); // No artificial roles on top-layer master user!

        verify(userAccountRepository).findByEmail("user@gymverse.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist in repository")
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@gymverse.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@gymverse.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when email is null or blank")
    void loadUserByUsername_NullOrBlankEmail_ThrowsUsernameNotFoundException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Email must not be null or empty");

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Email must not be null or empty");
    }
}
