package com.github.schaka.naviseerr.lidarr.setup

import com.fasterxml.jackson.databind.node.ArrayNode
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

class LidarrSetup(private val baseUrl: String, private val apiKey: String) {

    private val log = LoggerFactory.getLogger(LidarrSetup::class.java)
    private val client = RestClient.create()
    private val mapper = JsonMapper()

    fun setupRootFolder() {
        val existingFolders = client.get()
            .uri("$baseUrl/api/v1/rootfolder")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: "[]"

        if (existingFolders.trim() == "[]") {
            client.post()
                .uri("$baseUrl/api/v1/rootfolder")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "name" to "music",
                        "path" to "/music",
                        "defaultMetadataProfileId" to "1",
                        "defaultQualityProfileId" to "2",
                        "defaultMonitorOption" to "all",
                        "defaultNewItemMonitorOption" to "all",
                    )
                )
                .retrieve()
                .toBodilessEntity()
        }
    }

    /**
     * Enables Forms authentication in Lidarr and creates the initial admin/admin user.
     * The API key bypasses web auth entirely, so this works regardless of auth state.
     * On subsequent runs the user already exists and the POST to /user will fail gracefully.
     * If auth is already set to something other than None, setup is skipped entirely.
     */
    fun setupAuth() {
        try {
            val hostConfigRaw = client.get()
                .uri("$baseUrl/api/v1/config/host")
                .header("X-Api-Key", apiKey)
                .retrieve()
                .body(String::class.java) ?: return

            if (!hostConfigRaw.contains(Regex("\"authenticationMethod\"\\s*:\\s*\"none\""))) {
                return
            }

            val updatedConfig = hostConfigRaw
                .replace(Regex("\"authenticationMethod\"\\s*:\\s*\"none\""), "\"authenticationMethod\":\"forms\"")
                .replace(Regex("\"authenticationRequired\"\\s*:\\s*\"disabled\""), "\"authenticationRequired\":\"enabled\"")
                .replace(Regex("\"username\"\\s*:\\s*\"\""), "\"username\":\"admin\"")
                .replace(Regex("\"password\"\\s*:\\s*\"\""), "\"password\":\"admin\"")
                .replace(Regex("\"passwordConfirmation\"\\s*:\\s*\"\""), "\"passwordConfirmation\":\"admin\"")

            client.put()
                .uri("$baseUrl/api/v1/config/host")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedConfig)
                .retrieve()
                .toBodilessEntity()

        } catch (e: Exception) {
            log.warn("Lidarr auth setup incomplete: ${e.message} — configure admin/admin manually at $baseUrl")
        }
    }

    fun installTubifarry() {
        val existingPlugins = client.get()
            .uri("$baseUrl/api/v1/system/plugins")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: "[]"

        if (existingPlugins.trim() == "[]") {
            client.post()
                .uri("$baseUrl/api/v1/command")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "githubUrl" to "https://github.com/TypNull/Tubifarry",
                        "name" to "InstallPlugin",
                    )
                )
                .retrieve()
                .toBodilessEntity()

            Thread.sleep(30000)

            client.post()
                .uri("$baseUrl/api/v1/system/restart")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()

            waitForReady()
        }
    }

    /**
     * Adds SoulseekDownloadProtocol to every delay profile that doesn't already have it.
     * Lidarr's ProtocolSpecification rejects releases whose protocol isn't listed (and allowed)
     * in the delay profile matched by the artist's tags. The default profile only ships with
     * Usenet and Torrent, so Soulseek results are silently rejected without this step.
     * Must be called after installTubifarry() so the protocol is registered with Lidarr.
     */
    fun setupSoulseekDelayProfile() {
        val profilesRaw = client.get()
            .uri("$baseUrl/api/v1/delayprofile")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: return

        val profiles = mapper.readTree(profilesRaw)
        for (profile in profiles) {
            val items = profile.path("items")
            if (items.any { it.path("protocol").asString() == "SoulseekDownloadProtocol" }) continue

            val profileObj = profile.deepCopy()
            (profileObj.path("items") as ArrayNode).addObject().apply {
                put("name", "Soulseek")
                put("protocol", "SoulseekDownloadProtocol")
                put("allowed", true)
                put("delay", 0)
            }

            client.put()
                .uri("$baseUrl/api/v1/delayprofile/${profile.path("id").asInt()}")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(profileObj))
                .retrieve()
                .toBodilessEntity()
        }
    }

    /**
     * Configures slskd as a download client in Lidarr using the Tubifarry SlskdClient plugin.
     * Skipped if a download client with implementation "SlskdClient" already exists.
     * Field names match the SlskdProviderSettings schema from Tubifarry — update if the plugin changes them.
     */
    fun setupSlskdDownloadClient(slskdHost: String, slskdApiKey: String) {
        val existing = client.get()
            .uri("$baseUrl/api/v1/downloadclient")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: "[]"

        if ("SlskdClient" in existing) return

        client.post()
            .uri("$baseUrl/api/v1/downloadclient")
            .header("X-Api-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "enable" to true,
                    "protocol" to "SoulseekDownloadProtocol",
                    "priority" to 1,
                    "removeCompletedDownloads" to true,
                    "removeFailedDownloads" to true,
                    "name" to "slskd",
                    "fields" to listOf(
                        mapOf("name" to "baseUrl", "value" to "http://$slskdHost:5030"),
                        mapOf("name" to "apiKey", "value" to slskdApiKey),
                    ),
                    "implementationName" to "Slskd",
                    "implementation" to "SlskdClient",
                    "configContract" to "SlskdProviderSettings",
                    "tags" to emptyList<Int>(),
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    /**
     * Configures slskd as an indexer in Lidarr using the Tubifarry SlskdIndexer plugin.
     * Skipped if an indexer with implementation "SlskdIndexer" already exists.
     * Field names are assumed to mirror the download client schema — update if the plugin differs.
     */
    fun setupSlskdIndexer(slskdHost: String, slskdApiKey: String) {
        val existing = client.get()
            .uri("$baseUrl/api/v1/indexer")
            .header("X-Api-Key", apiKey)
            .retrieve()
            .body(String::class.java) ?: "[]"

        if ("SlskdIndexer" in existing) return

        client.post()
            .uri("$baseUrl/api/v1/indexer")
            .header("X-Api-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "enableRss" to false,
                    "enableAutomaticSearch" to true,
                    "enableInteractiveSearch" to true,
                    "protocol" to "SoulseekDownloadProtocol",
                    "priority" to 25,
                    "name" to "slskd",
                    "fields" to listOf(
                        mapOf("name" to "baseUrl", "value" to "http://$slskdHost:5030"),
                        mapOf("name" to "apiKey", "value" to slskdApiKey),
                    ),
                    "implementationName" to "Slskd",
                    "implementation" to "SlskdIndexer",
                    "configContract" to "SlskdSettings",
                    "tags" to emptyList<Int>(),
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    private fun waitForReady() {
        while (true) {
            Thread.sleep(1000)
            try {
                client.get()
                    .uri("$baseUrl/ping")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .toBodilessEntity()
                break
            } catch (e: Exception) {
                // not restarted yet
            }
        }
    }
}
