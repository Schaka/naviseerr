package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.containers.*
import com.github.schaka.naviseerr.lidarr.setup.LidarrSetup
import com.github.schaka.naviseerr.navidrome.setup.NavidromeSetup
import org.slf4j.LoggerFactory
import org.testcontainers.lifecycle.Startables
import java.nio.file.Files
import java.nio.file.Path


class LocalDevEnvironment {

    private val log = LoggerFactory.getLogger(LocalDevEnvironment::class.java)
    private val projectRoot: Path = Path.of(System.getProperty("user.dir"))
    private val localRuntime: Path = projectRoot.resolve("local-runtime")
    private val musicLibrary: Path = projectRoot.resolve("music-library")

    private var navidromePort: Int = 0
    private var lidarrPort: Int = 0
    private var slskdPort: Int = 0
    private var taggingPort: Int = 0
    private var lidarrApiKey: String = ""
    private var jdbcUrl: String = ""

    fun start() {
        createSharedDirectories()

        val network = createLocalDevNetwork()
        val postgres = PostgresContainer(localRuntime, network)
        val navidrome = NavidromeContainer(musicLibrary, localRuntime, network)
        val lidarr = LidarrContainer(musicLibrary, localRuntime, network)
        val slskd = SlskdContainer(musicLibrary, localRuntime, network)
        val tagging = TaggingContainer(musicLibrary, localRuntime, network)

        Startables.deepStart(postgres, navidrome, lidarr, slskd, tagging).join()

        navidromePort = navidrome.getMappedPort(4533)
        lidarrPort = lidarr.getMappedPort(8686)
        slskdPort = slskd.getMappedPort(5030)
        taggingPort = tagging.getMappedPort(8090)
        jdbcUrl = postgres.jdbcUrl

        val navidromeSetup = NavidromeSetup("http://localhost:$navidromePort")
        navidromeSetup.createAdmin()

        lidarrApiKey = lidarr.readApiKey()
        val lidarrSetup = LidarrSetup("http://localhost:$lidarrPort", lidarrApiKey)
        lidarrSetup.setupRootFolder()
        lidarrSetup.setupAuth()
        lidarrSetup.installTubifarry()
        lidarrSetup.setupSoulseekDelayProfile()
        lidarrSetup.setupSlskdDownloadClient("slskd", "naviseerr-local-dev")
        lidarrSetup.setupSlskdIndexer("slskd", "naviseerr-local-dev")

        System.setProperty("spring.datasource.url", postgres.jdbcUrl)
        System.setProperty("spring.datasource.username", "naviseerr")
        System.setProperty("spring.datasource.password", "naviseerr")
        System.setProperty("navidrome.url", "http://localhost:$navidromePort")
        System.setProperty("navidrome.admin-user", "admin")
        System.setProperty("navidrome.admin-pass", "admin")
        System.setProperty("lidarr.url", "http://localhost:$lidarrPort")
        System.setProperty("lidarr.api-key", lidarrApiKey)
        System.setProperty("slskd.url", "http://localhost:$slskdPort")
        System.setProperty("slskd.api-key", "naviseerr-local-dev")
        System.setProperty("slskd.download-dir", localRuntime.resolve("downloads").toAbsolutePath().toString())
        System.setProperty("tagging.url", "http://localhost:$taggingPort")
    }

    fun logStartupInfo() {
        log.info("Started Postgres at $jdbcUrl => Login via: naviseerr/naviseerr")
        log.info("Started Navidrome at http://localhost:$navidromePort => Login via: admin/admin")
        log.info("Started Lidarr at http://localhost:$lidarrPort => Login via: admin/admin | API-Key: $lidarrApiKey")
        log.info("Started Slskd at http://localhost:$slskdPort => Login via: admin/admin | API-Key: naviseerr-local-dev")
        log.info("Started Tagging service at http://localhost:$taggingPort")
    }

    private fun createSharedDirectories() {
        listOf(
            musicLibrary,
            localRuntime.resolve("downloads"),
            localRuntime.resolve("downloads/incomplete"),
        ).forEach(Files::createDirectories)
    }
}
