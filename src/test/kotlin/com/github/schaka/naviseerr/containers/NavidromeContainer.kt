package com.github.schaka.naviseerr.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class NavidromeContainer(musicLibrary: Path, localRuntime: Path, network: Network? = null) : GenericContainer<NavidromeContainer>("deluan/navidrome:latest") {

    init {
        Files.createDirectories(localRuntime.resolve("navidrome"))
        withEnv("ND_MUSICFOLDER", "/music")
        withEnv("ND_DATAFOLDER", "/data")
        withEnv("ND_LOGLEVEL", "info")
        withEnv("ND_SESSIONTIMEOUT", "24h")
        withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_ONLY)
        withFileSystemBind(localRuntime.resolve("navidrome").toString(), "/data", BindMode.READ_WRITE)
        withExposedPorts(4533)
        waitingFor(Wait.forHttp("/ping").forPort(4533).withStartupTimeout(Duration.ofMinutes(2)))
        network?.let { withNetwork(it).withNetworkAliases("navidrome") }
    }
}
