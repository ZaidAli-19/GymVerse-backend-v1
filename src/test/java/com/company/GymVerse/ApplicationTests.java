package com.company.GymVerse;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Autowired
    @Qualifier("centralEntityManagerFactory")
    private EntityManagerFactory centralEntityManagerFactory;

    @Autowired
    @Qualifier("tenantEntityManagerFactory")
    private EntityManagerFactory tenantEntityManagerFactory;

    @Autowired
    @Qualifier("centralTransactionManager")
    private PlatformTransactionManager centralTransactionManager;

    @Autowired
    @Qualifier("tenantTransactionManager")
    private PlatformTransactionManager tenantTransactionManager;

    @Test
    @DisplayName("AC-1.7.1: Application context loads and dual persistence infrastructure beans are active")
    void contextLoads() {
        assertThat(centralEntityManagerFactory)
                .as("Central EntityManagerFactory must be present")
                .isNotNull();

        assertThat(tenantEntityManagerFactory)
                .as("Tenant EntityManagerFactory must be present")
                .isNotNull();

        assertThat(centralTransactionManager)
                .as("Central TransactionManager must be present")
                .isNotNull();

        assertThat(tenantTransactionManager)
                .as("Tenant TransactionManager must be present")
                .isNotNull();
    }
}
