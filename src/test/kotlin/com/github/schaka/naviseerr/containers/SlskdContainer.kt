package com.github.schaka.naviseerr.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID

/**
 * slskd uses /app as its data directory in the official Docker image (binary is in PATH).
 * Soulseek network credentials come from environment variables if set, or from a persisted
 * credentials file in slskdDir. If neither exists, random UUIDs are generated and saved so
 * the same account is reused across restarts without creating a new Soulseek account each time.
 * Usernames are 20 hex chars (Soulseek limit is 24 alphanumeric); passwords are 32 hex chars.
 * Web UI credentials (admin/admin) are set in the managed slskd.yml written before startup.
 */
class SlskdContainer(musicLibrary: Path, localRuntime: Path, network: Network? = null) : GenericContainer<SlskdContainer>("slskd/slskd:latest") {

    private val slskdDir = localRuntime.resolve("slskd")

    init {
        Files.createDirectories(slskdDir)
        writeConfig()
        val (slskUsername, slskPassword) = resolveSlskCredentials()
        withExposedPorts(5030)
        withEnv("SLSKD_SLSK_USERNAME", slskUsername)
        withEnv("SLSKD_SLSK_PASSWORD", slskPassword)
        withEnv("SLSKD_API_KEY", "naviseerr-local-dev")
        withEnv("SLSKD_REMOTE_CONFIGURATION", "true")
        withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_ONLY)
        withFileSystemBind(localRuntime.resolve("downloads").toString(), "/downloads", BindMode.READ_WRITE)
        withFileSystemBind(slskdDir.toString(), "/app", BindMode.READ_WRITE)
        withCreateContainerCmdModifier { it.withUser("1000:1000") }
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
        network?.let { withNetwork(it).withNetworkAliases("slskd") }
    }

    private fun resolveSlskCredentials(): Pair<String, String> {
        val envUsername = System.getenv("SLSKD_SOULSEEK_USERNAME")
        val envPassword = System.getenv("SLSKD_SOULSEEK_PASSWORD")
        if (!envUsername.isNullOrBlank() && !envPassword.isNullOrBlank()) {
            return envUsername to envPassword
        }

        val credentialsFile = slskdDir.resolve("soulseek-credentials.txt")
        if (Files.exists(credentialsFile)) {
            val lines = Files.readAllLines(credentialsFile)
            if (lines.size >= 2) return lines[0] to lines[1]
        }

        val username = UUID.randomUUID().toString().replace("-", "").take(20)
        val password = UUID.randomUUID().toString().replace("-", "")
        Files.writeString(credentialsFile, "$username\n$password")
        return username to password
    }

    private fun writeConfig() {
        Files.writeString(
            slskdDir.resolve("slskd.yml"),
            """
            web:
              authentication:
                disabled: false
                username: admin
                password: admin
            shares:
              directories:
                - path: /music
            directories:
              downloads: /downloads
              incomplete: /downloads/incomplete
            """.trimIndent()
        )
    }
}
