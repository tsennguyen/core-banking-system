package com.example.corebanking.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests running against a real PostgreSQL container. When Docker is
 * absent (e.g. local developer laptop), tests extending this class are gracefully disabled without
 * failing the build (ADR-018).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
public abstract class IntegrationTestBase {

    @Container @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
}
