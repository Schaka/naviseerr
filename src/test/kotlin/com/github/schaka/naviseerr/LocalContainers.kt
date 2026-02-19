package com.github.schaka.naviseerr

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startables
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

object LocalContainers {

    private val projectRoot: Path = Path.of(System.getProperty("user.dir"))
    private val localRuntime: Path = projectRoot.resolve("local-runtime")
    private val musicLibrary: Path = projectRoot.resolve("music-library")

    private const val PG_DB = "naviseerr"
    private const val PG_USER = "naviseerr"
    private const val PG_PASSWORD = "naviseerr"

    private val log = LoggerFactory.getLogger(LocalContainers::class.java)

    init {
        createDirectories()
        writeSlskdConfigIfAbsent()

        val postgres = createPostgres()
        val navidrome = createNavidrome()
        val lidarr = createLidarr()
        val slskd = createSlskd()

        Startables.deepStart(postgres, navidrome, lidarr, slskd).join()

        setupNavidrome(navidrome)
        val lidarrApiKey = readLidarrApiKey(lidarr)
        setupLidarr(lidarr, lidarrApiKey)

        val navidromePort = navidrome.getMappedPort(4533)
        val lidarrPort = lidarr.getMappedPort(8686)
        val slskdPort = slskd.getMappedPort(5030)

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl())
        System.setProperty("spring.datasource.username", PG_USER)
        System.setProperty("spring.datasource.password", PG_PASSWORD)
        System.setProperty("navidrome.url", "http://localhost:$navidromePort")
        System.setProperty("navidrome.admin-user", "admin")
        System.setProperty("navidrome.admin-pass", "admin")
        System.setProperty("lidarr.url", "http://localhost:$lidarrPort")
        System.setProperty("lidarr.api-key", lidarrApiKey)
        System.setProperty("slskd.url", "http://localhost:$slskdPort")
        System.setProperty("slskd.api-key", "naviseerr-local-dev")
        System.setProperty("slskd.download-dir", musicLibrary.resolve("temp-downloads").toAbsolutePath().toString())

        log.info("Started Navidrome at http://localhost:$navidromePort => Login via: admin/admin")
        log.info("Started Lidarr at http://localhost:$lidarrPort => Login via: admin/admin")
        log.info("Started Slskd at http://localhost:$slskdPort => Login via: admin/admin")
    }

    private fun createDirectories() {
        listOf(
            localRuntime.resolve("postgres"),
            localRuntime.resolve("navidrome"),
            localRuntime.resolve("lidarr"),
            localRuntime.resolve("slskd"),
            musicLibrary,
            musicLibrary.resolve("temp-downloads"),
            musicLibrary.resolve("temp-downloads/incomplete"),
        ).forEach(Files::createDirectories)
    }

    /**
     * slskd does not have env vars for all share/directory options, so a minimal config
     * is written on first run. Credentials are always supplied via environment variables.
     * The file is only written once — subsequent runs reuse the existing config.
     */
    private fun writeSlskdConfigIfAbsent() {
        val configFile = localRuntime.resolve("slskd/slskd.yml")
        if (!Files.exists(configFile)) {
            Files.writeString(
                configFile,
                """
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

    private fun createPostgres(): PostgreSQLContainer {
        return PostgreSQLContainer("postgres:17")
            .withDatabaseName(PG_DB)
            .withUsername(PG_USER)
            .withPassword(PG_PASSWORD)
            .withExposedPorts(5432)
            .withFileSystemBind(
                localRuntime.resolve("postgres").toString(),
                "/var/lib/postgresql/data",
                BindMode.READ_WRITE
            )
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
    }

    private fun createNavidrome(): GenericContainer<*> {
        return GenericContainer("deluan/navidrome:latest")
            .withEnv("ND_MUSICFOLDER", "/music")
            .withEnv("ND_DATAFOLDER", "/data")
            .withEnv("ND_LOGLEVEL", "info")
            .withEnv("ND_SESSIONTIMEOUT", "24h")
            .withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_ONLY)
            .withFileSystemBind(localRuntime.resolve("navidrome").toString(), "/data", BindMode.READ_WRITE)
            .withExposedPorts(4533)
            .waitingFor(Wait.forHttp("/ping").forPort(4533).withStartupTimeout(Duration.ofMinutes(2)))
    }

    private fun createLidarr(): GenericContainer<*> {
        return GenericContainer("lscr.io/linuxserver/lidarr:nightly")
            .withExposedPorts(8686)
            .withEnv("PUID", "1000")
            .withEnv("PGID", "1000")
            .withEnv("TZ", "UTC")
            .withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_WRITE)
            .withFileSystemBind(localRuntime.resolve("lidarr").toString(), "/config", BindMode.READ_WRITE)
            .waitingFor(Wait.forHttp("/ping").forPort(8686).withStartupTimeout(Duration.ofMinutes(5)))
    }

    /**
     * slskd uses /app as its data directory in the official Docker image (binary is in PATH).
     * Credentials are read from the host environment so they are never written to disk.
     */
    private fun createSlskd(): GenericContainer<*> {
        return GenericContainer("slskd/slskd:latest")
            .withExposedPorts(5030)
            .withEnv("SLSKD_SLSK_USERNAME", System.getenv("SLSKD_SOULSEEK_USERNAME") ?: "")
            .withEnv("SLSKD_SLSK_PASSWORD", System.getenv("SLSKD_SOULSEEK_PASSWORD") ?: "")
            .withEnv("SLSKD_API_KEY", "naviseerr-local-dev")
            .withEnv("SLSKD_REMOTE_CONFIGURATION", "true")
            .withFileSystemBind(musicLibrary.toString(), "/music", BindMode.READ_ONLY)
            .withFileSystemBind(musicLibrary.resolve("temp-downloads").toString(), "/downloads", BindMode.READ_WRITE)
            .withFileSystemBind(localRuntime.resolve("slskd").toString(), "/app", BindMode.READ_WRITE)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
    }

    /**
     * Creates the initial admin user on first run via Navidrome's bootstrap endpoint.
     * The endpoint is only accessible when no users exist, so subsequent runs will fail
     * gracefully since the user persists in local-runtime/navidrome/.
     */
    private fun setupNavidrome(navidrome: GenericContainer<*>) {
        val client = RestClient.create()
        val port = navidrome.getMappedPort(4533)
        try {
            client.post()
                .uri("http://localhost:$port/auth/createAdmin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("username" to "admin", "password" to "admin"))
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            // Admin already exists from a previous run — safe to ignore
        }
    }

    /**
     * Reads the auto-generated Lidarr API key from config.xml.
     * Lidarr writes its config before the HTTP server starts, but we retry briefly
     * in case of a race on first boot.
     */
    private fun readLidarrApiKey(lidarr: GenericContainer<*>): String {
        repeat(10) {
            try {
                val result = lidarr.execInContainer("cat", "/config/config.xml")
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

    /**
     * Adds the /music root folder to Lidarr on first run.
     * On subsequent runs the folder already exists, so the POST is skipped.
     */
    private fun setupLidarr(lidarr: GenericContainer<*>, apiKey: String) {
        val client = RestClient.create()
        val baseUrl = "http://localhost:${lidarr.getMappedPort(8686)}/api/v1"

        val existingFolders = client.get()
            .uri("$baseUrl/rootfolder")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: "[]"

        if (existingFolders.trim() == "[]") {
            client.post()
                .uri("$baseUrl/rootfolder")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "name" to "music",
                        "path" to "/music",
                        "defaultMetadataProfileId" to "1",
                        "defaultQualityProfileId" to "1",
                        "defaultMonitorOption"  to "all",
                        "defaultNewItemMonitorOption"  to "all",
                    )
                )
                .retrieve()
                .toBodilessEntity()
        }
    }
}
