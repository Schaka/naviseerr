package com.github.schaka.build

import org.gradle.api.Project
import java.lang.System.getenv
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class Build(private val project: Project) {

    /**
     * The current build time.
     */
    val buildDateAndTime: OffsetDateTime = OffsetDateTime.now()

    /**
     * @return true, if it is a build on a CI system.
     */
    fun isCI(): Boolean {
        return getenv("CI") != null
    }

    fun branchName(): String {
        return getenv("GITHUB_REF_NAME")
    }

    fun commitHash(): String {
        return getenv("GITHUB_SHA") ?: "local"
    }

    /**
     * @return the HTTP(S) address of the project.
     */
    fun projectSourceRoot(): String {
        return getenv("CI_PROJECT_URL") ?: "${project.rootDir}"
    }

    /**
     * @return the name of the user who started the job.
     */
    fun userName(): String {
        return getenv("USERNAME") ?: System.getProperty("user.name")
    }

    /**
     * @return a token to authenticate with certain API endpoints.
     */
    fun jobToken(): String? {
        return getenv("GITHUB_TOKEN")
    }

    /**
     * @return the current build date in ISO format.
     */
    fun formattedBuildDate(): String {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(buildDateAndTime)
    }

    /**
     * @return the current build time without date in ISO format.
     */
    fun formattedBuildTime(): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").format(buildDateAndTime)
    }
}
