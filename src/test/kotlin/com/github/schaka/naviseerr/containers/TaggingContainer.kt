package com.github.schaka.naviseerr.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Path
import java.time.Duration

class TaggingContainer(musicLibrary: Path, localRuntime: Path, network: Network? = null) : GenericContainer<TaggingContainer>("ghcr.io/schaka/naviseerr-tagging:8-merge") {

    init {
        withExposedPorts(8090)
        withEnv("PUID", "1000")
        withEnv("PGID", "1000")
        // ACOUSTID_API_KEY is intentionally not set for local dev testing.
        // Set it via withEnv("ACOUSTID_API_KEY", "...") if you want real AcoustID fingerprint lookups.
        withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_WRITE)
        withFileSystemBind(localRuntime.resolve("downloads").toString(), "/downloads", BindMode.READ_WRITE)
        waitingFor(Wait.forHttp("/health").forPort(8090).withStartupTimeout(Duration.ofMinutes(2)))
        network?.let { withNetwork(it).withNetworkAliases("tagging") }
    }
}
