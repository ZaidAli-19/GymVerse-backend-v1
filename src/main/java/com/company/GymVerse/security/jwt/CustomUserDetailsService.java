package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Spring Security UserDetailsService implementation loading master UserAccount credentials
 * directly from the central database for authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email must not be null or empty");
        }

        final UserAccount account = userAccountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        log.debug("Found central user account [id={}, email={}]", account.getId(), account.getEmail());

        return User.builder()
                .username(account.getEmail())
                .password(account.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
