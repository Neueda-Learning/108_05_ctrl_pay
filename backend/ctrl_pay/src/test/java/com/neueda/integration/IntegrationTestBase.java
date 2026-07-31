package com.neueda.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration testing with real MySQL database via TestContainers.
 * All integration tests should extend this class to get automatic MySQL setup/teardown.
 * 
 * Features:
 * - Automatic MySQL container lifecycle management
 * - Dynamic properties injection for Spring Boot
 * - Real database testing (NOT in-memory)
 * - Automatic schema initialization from schema.sql
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {
    
    /**
     * MySQL Test Container - automatically started/stopped by Testcontainers
     * Uses MySQL 8.0 Alpine image for small size and fast startup
     */
    @Container
    static MySQLContainer<?> container = new MySQLContainer<>("mysql:8.0.35-alpine")
        .withDatabaseName("ctrl_pay")
        .withUsername("root")
        .withPassword("test123")
        .withInitScript("schema.sql");  // Automatically runs schema initialization
    
    /**
     * Dynamically inject MySQL connection properties into Spring Boot application context.
     * This ensures the application connects to the test container instead of production DB.
     * 
     * @param registry Dynamic property registry for injecting properties
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
    }
    
    /**
     * Verify container started successfully before tests run
     */
    @BeforeAll
    static void verifyContainer() {
        assert container.isRunning() : "MySQL test container failed to start";
    }
}

