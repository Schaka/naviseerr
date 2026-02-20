package com.github.schaka.naviseerr.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class PostgresContainer(localRuntime: Path, network: Network? = null) : PostgreSQLContainer("postgres:17") {

    init {
        Files.createDirectories(localRuntime.resolve("postgres"))
        withDatabaseName("naviseerr")
        withUsername("naviseerr")
        withPassword("naviseerr")
        withExposedPorts(5432)
        withFileSystemBind(
            localRuntime.resolve("postgres").toString(),
            "/var/lib/postgresql/data",
            BindMode.READ_WRITE
        )
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        network?.let { withNetwork(it).withNetworkAliases("postgres") }
    }
}
