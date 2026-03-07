package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.containers.*
import com.github.schaka.naviseerr.lidarr.setup.LidarrSetup
import com.github.schaka.naviseerr.navidrome.setup.NavidromeSetup
import org.apache.commons.vfs2.FileType
import org.apache.commons.vfs2.VFS
import org.slf4j.LoggerFactory
import org.testcontainers.lifecycle.Startables
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream


class LocalDevEnvironment {

    private val log = LoggerFactory.getLogger(LocalDevEnvironment::class.java)
    private val projectRoot: Path = Path.of(System.getProperty("user.dir"))
    private val localRuntime: Path = projectRoot.resolve("local-runtime")
    private val musicLibrary: Path = projectRoot.resolve("music-library")

    private var navidromePort: Int = 0
    private var lidarrPort: Int = 0
    private var slskdPort: Int = 0
    private var lidarrApiKey: String = ""
    private var jdbcUrl: String = ""

    fun start() {
        createSharedDirectories()
        setupOneTagger()

        val network = createLocalDevNetwork()
        val postgres = PostgresContainer(localRuntime, network)
        val navidrome = NavidromeContainer(musicLibrary, localRuntime, network)
        val lidarr = LidarrContainer(musicLibrary, localRuntime, network)
        val slskd = SlskdContainer(musicLibrary, localRuntime, network)

        Startables.deepStart(postgres, navidrome, lidarr, slskd).join()

        navidromePort = navidrome.getMappedPort(4533)
        lidarrPort = lidarr.getMappedPort(8686)
        slskdPort = slskd.getMappedPort(5030)
        jdbcUrl = postgres.jdbcUrl

        NavidromeSetup("http://localhost:$navidromePort").createAdmin()

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
    }

    private fun setupOneTagger() {
        val oneTaggerPath = localRuntime.resolve("one-tagger")
        Files.createDirectories(oneTaggerPath)
        System.setProperty("ONE_TAGGER_PATH", oneTaggerPath.resolve("onetagger-cli").toAbsolutePath().toString())
        val fileUrl = URI("https://github.com/Marekkon5/onetagger/releases/download/1.7.0/OneTagger-linux-cli.tar.gz").toURL()
        val tarPath = oneTaggerPath.resolve("one-tagger.tar")


        Files.copy(GZIPInputStream(fileUrl.openStream()),
            tarPath,
                StandardCopyOption.REPLACE_EXISTING
            )

        val fsManager = VFS.getManager()
        val tar = fsManager.resolveFile("tar:${tarPath.toAbsolutePath().toUri()}")
        for (entry in tar) {
            // entries have a leading slash, need to construct path like this to avoid absolute path
            val extractTo = Paths.get(oneTaggerPath.toAbsolutePath().toString(), entry.name.path )
            if (entry.isReadable && entry.type === FileType.FILE) {
                Files.createDirectories(extractTo.parent)
                entry.content.use { content ->
                    content.inputStream.use { stream ->
                        Files.copy(stream, extractTo, StandardCopyOption.REPLACE_EXISTING)
                        extractTo.toFile().setExecutable(true)
                    }
                }
            }
        }
    }

    fun logStartupInfo() {
        log.info("Started Postgres at $jdbcUrl => Login via: naviseerr/naviseerr")
        log.info("Started Navidrome at http://localhost:$navidromePort => Login via: admin/admin")
        log.info("Started Lidarr at http://localhost:$lidarrPort => Login via: admin/admin | API-Key: $lidarrApiKey")
        log.info("Started Slskd at http://localhost:$slskdPort => Login via: admin/admin | API-Key: naviseerr-local-dev")
    }

    private fun createSharedDirectories() {
        listOf(
            musicLibrary,
            localRuntime.resolve("downloads"),
            localRuntime.resolve("downloads/incomplete"),
        ).forEach(Files::createDirectories)
    }
}
