import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import net.nemerosa.versioning.VersioningExtension
import org.jooq.meta.jaxb.Logging
import nu.studer.gradle.jooq.JooqEdition
import java.nio.file.Files
import nu.studer.gradle.jooq.JooqExtension
import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.gradle.FlywayExtension
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.jooqGenerator
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {

    id("idea")
    id("org.springframework.boot") version "3.4.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.6"
    id("net.nemerosa.versioning") version "3.1.0"
    id("com.google.cloud.tools.jib") version "3.4.4"
    id("nu.studer.jooq") version "9.0"
    id("org.flywaydb.flyway") version "10.20.0"

    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"

}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.spring.io/snapshot")
    maven("https://repo.spring.io/milestone")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.data:spring-data-commons")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.lucene:lucene-core:10.0.0")
    implementation("org.apache.lucene:lucene-queryparser:10.0.0")
    implementation("org.apache.lucene:lucene-analysis-common:10.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    implementation("io.github.openfeign:feign-core:13.1")
    implementation("io.github.openfeign:feign-jackson:13.1")
    implementation("io.github.openfeign:feign-httpclient:13.1")

    implementation("org.slf4j:slf4j-api")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")
    jooqGenerator("com.h2database:h2")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

configure<FlywayExtension> {
    // this migration references a temporary database at compile time that's only used for generating JOOQ classes
    // Spring creates a migration at runtime, before the application starts and bootstraps any connections via JOOQ
    val tempDir = Files.createTempDirectory("naviseerr-build-db")
    url = "jdbc:h2:file:${tempDir}/naviseerr"
    schemas = arrayOf("PUBLIC")
    user = "naviseerr"
    password = "naviseerr-pw"
}

tasks.withType<FlywayMigrateTask> {
 // nothing for now
}

configure<JooqExtension> {
    version.set("3.19.14")  // the default (can be omitted)
    edition.set(JooqEdition.OSS)  // the default (can be omitted)

    configurations {
        create("main") {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = flyway.url
                    user = flyway.user
                    password = flyway.password
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        inputSchema = "PUBLIC"
                        outputSchema = "PUBLIC"
                        name = "org.jooq.meta.h2.H2Database"
                        includes = ".*"
                        excludes = "flyway_schema_history"
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "com.github.schaka.naviseerr.db"
                        directory = "build/generated-src/jooq/main"  // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.withType<JooqGenerate> {
    dependsOn("flywayMigrate")

    // declare Flyway migration scripts as inputs on the jOOQ task
    inputs.files(fileTree("src/main/resources/db/migration"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    //allInputsDeclared.set(true)
}

configure<SpringBootExtension> {
    buildInfo()
}

configure<IdeaModel> {
    module {
        inheritOutputDirs = true
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_22.toString()
    targetCompatibility = JavaVersion.VERSION_22.toString()

    options.compilerArgs.addAll(
        listOf("--add-modules=jdk.incubator.vector")
    )
}

tasks.withType<KotlinCompile> {

    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JVM_22
        javaParameters = true
    }
}

configure<VersioningExtension> {
    /**
     * Add GitHub CI branch name environment variable
     */
    branchEnv = listOf("GITHUB_REF_NAME")
}

extra {
    val build = getBuild()
    val versioning: VersioningExtension = extensions.getByName<VersioningExtension>("versioning")
    val branch = versioning.info.branch.replace("/", "-")
    val shortCommit = versioning.info.commit.take(8)

    project.extra["build.date-time"] = build.buildDateAndTime
    project.extra["build.date"] = build.formattedBuildDate()
    project.extra["build.time"] = build.formattedBuildTime()
    project.extra["build.revision"] = versioning.info.commit
    project.extra["build.revision.abbreviated"] = shortCommit
    project.extra["build.branch"] = branch
    project.extra["build.user"] = build.userName()

    val containerImageName = "schaka/${project.name}"
    val containerImageTags = mutableSetOf(shortCommit, branch)
    if (branch.startsWith("v")) {
        containerImageTags.add("stable")
    }

    project.extra["jib.image.name"] = containerImageName
    project.extra["jib.image.tags"] = containerImageTags

    val registryImageName = "ghcr.io/${containerImageName}"
    val registryImageTags = containerImageTags.map { "ghcr.io/${containerImageName}:$it" }.toMutableList()

    project.extra["docker.image.name"] = registryImageName
    project.extra["docker.image.version"] = branch
    project.extra["docker.image.source"] = build.projectSourceRoot()
    project.extra["docker.image.tags"] = registryImageTags

    //remove when there's a better way of producing both arm64 and amd64 images
    containerImageTags.add("arm64-$branch")
    registryImageTags.add("ghcr.io/${containerImageName}:amd64-$branch")

}

tasks.withType<BootRun> {
    jvmArgs(
        arrayOf(
            "--add-modules=jdk.incubator.vector",
            "-Dspring.config.additional-location=optional:file:/workspace/application.yaml",
            "-Dsun.jnu.encoding=UTF-8",
            "-Dfile.encoding=UTF-8",
            "-Dorg.jooq.no-logo=true"
        )
    )
}

jib {
    to {
        image = "ghcr.io/${project.extra["jib.image.name"]}"
        tags = project.extra["jib.image.tags"] as Set<String>

        auth {
            username = System.getenv("USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    from {
        image = "eclipse-temurin:23-jdk-noble"
        auth {
            username = System.getenv("DOCKERHUB_USER")
            password = System.getenv("DOCKERHUB_PASSWORD")
        }
        platforms {
            /*
            platform {
                architecture = "amd64"
                os = "linux"
            }
            */
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    container {
        jvmFlags = listOf(
            "-Dspring.config.additional-location=optional:file:/workspace/application.yaml",
            "-Dsun.jnu.encoding=UTF-8",
            "-Dfile.encoding=UTF-8",
            "-Dorg.jooq.no-logo=true",
            "--add-modules=jdk.incubator.vector",
            "-Xms256m",
            "-Xmx512m"
        )
        mainClass = "com.github.schaka.naviseerr.NaviseerrApplicationKt"
        ports = listOf("8080")
        format = ImageFormat.Docker // OCI not yet supported
        volumes = listOf("/database", "/workspace")

        labels.set(
            mapOf(
                "org.opencontainers.image.created" to "${project.extra["build.date"]}T${project.extra["build.time"]}",
                "org.opencontainers.image.revision" to project.extra["build.revision"] as String,
                "org.opencontainers.image.version" to project.version as String,
                "org.opencontainers.image.title" to project.name,
                "org.opencontainers.image.authors" to "Schaka <schaka@github.com>",
                "org.opencontainers.image.source" to project.extra["docker.image.source"] as String,
                "org.opencontainers.image.description" to project.description,
            )
        )

        // Exclude all "developmentOnly" dependencies, e.g. Spring devtools.
        configurationName.set("productionRuntimeClasspath")
    }
}

tasks.withType<BootBuildImage> {

    docker.publishRegistry.url = "ghcr.io"
    docker.publishRegistry.username = System.getenv("USERNAME") ?: "INVALID_USER"
    docker.publishRegistry.password = System.getenv("GITHUB_TOKEN") ?: "INVALID_PASSWORD"

    builder = "paketobuildpacks/builder-jammy-buildpackless-tiny"
    buildpacks = listOf(
        "paketobuildpacks/environment-variables",
        "paketobuildpacks/adoptium",
        "paketobuildpacks/java",
        "paketobuildpacks/health-checker"
    )
    imageName = project.extra["docker.image.name"] as String
    version = project.extra["docker.image.version"] as String
    tags = project.extra["docker.image.tags"] as List<String>
    createdDate = "now"

    // It would also be possible to set this in the graalVmNative block, but we don't want to overwrite Spring's settings
    environment = mapOf(
        "BP_HEALTH_CHECKER_ENABLED" to "true",
        "BPL_JVM_CDS_ENABLED" to "true",
        "BP_JVM_CDS_ENABLED" to "true",
        "CDS_TRAINING_JAVA_TOOL_OPTIONS" to "--add-modules=jdk.incubator.vector -Dspring.profiles.active=cds",
        "BP_JVM_VERSION" to "23",
        "BPE_LANG" to "en_US.UTF-8",
        "BPE_LANGUAGE" to "LANGUAGE=en_US:en",
        "BPE_LC_ALL" to "en_US.UTF-8",
        "BPE_APPEND_JAVA_OPTS" to "--add-modules=jdk.incubator.vector -Xmx512m -Xms256m -Dorg.jooq.no-logo=true"
    )
}