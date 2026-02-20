package com.github.schaka.naviseerr.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class LidarrContainer(musicLibrary: Path, localRuntime: Path, network: Network? = null) : GenericContainer<LidarrContainer>("lscr.io/linuxserver/lidarr:nightly") {

    init {
        Files.createDirectories(localRuntime.resolve("lidarr"))
        withExposedPorts(8686)
        withEnv("PUID", "1000")
        withEnv("PGID", "1000")
        withEnv("TZ", "UTC")
        withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_WRITE)
        withFileSystemBind(musicLibrary.resolve("temp-downloads").toString(), "/downloads", BindMode.READ_WRITE)
        withFileSystemBind(localRuntime.resolve("lidarr").toString(), "/config", BindMode.READ_WRITE)
        waitingFor(Wait.forHttp("/ping").forPort(8686).withStartupTimeout(Duration.ofMinutes(5)))
        network?.let { withNetwork(it).withNetworkAliases("lidarr") }
    }

    /**
     * Reads the auto-generated Lidarr API key from config.xml.
     * Lidarr writes its config before the HTTP server starts, but we retry briefly
     * in case of a race on first boot.
     */
    fun readApiKey(): String {
        repeat(10) {
            try {
                val result = execInContainer("cat", "/config/config.xml")
                if (result.exitCode == 0 && "<ApiKey>" in result.stdout) {
                    return result.stdout.substringAfter("<ApiKey>").substringBefore("</ApiKey>").trim()
                }
            } catch (e: Exception) {
                // Config not yet written
            }
            Thread.sleep(2_000)
        }
        throw IllegalStateException("Could not read Lidarr API key from /config/config.xml after waiting 20 seconds")
    }
}
