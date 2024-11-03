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
import org.springframework.boot.gradle.tasks.aot.ProcessAot
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {

    id("idea")
    id("org.springframework.boot") version "3.4.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.6"
    id("net.nemerosa.versioning") version "3.1.0"
    id("org.graalvm.buildtools.native") version "0.10.3"
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
    implementation("org.flywaydb:flyway-core:10.15.0")
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
    version.set("3.19.11")  // the default (can be omitted)
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

// Required until GraalVM/Paketo builders receive a fix
sourceSets {
    main {
        java {
            srcDir("src/main")
            srcDir("src/java.base")
        }
        kotlin {
            srcDir("src/kotlin")
        }
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


/*
 * Hack required until
 * - https://github.com/paketo-buildpacks/native-image/issues/344
 * - https://github.com/oracle/graal/issues/9879
 * are fixed.
 *
 * We're copying over patches to the JDK and forcing them into the native image at build time.
 */
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_22.toString()
    targetCompatibility = JavaVersion.VERSION_22.toString()

    options.compilerArgs.addAll(
        listOf("--add-modules=jdk.incubator.vector")
    )

    options.javaModuleVersion

    finalizedBy("copyPatches")
}

tasks.register<Copy>("copyPatches") {
    dependsOn("build")
    mustRunAfter("compileJava")

    from(layout.buildDirectory.dir("classes/java/main"))
    include("**/*.*")
    into(layout.buildDirectory.dir("resources/main/java.base"))
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

    project.extra["docker.image.name"] = containerImageName
    project.extra["docker.image.version"] = branch
    project.extra["docker.image.source"] = build.projectSourceRoot()
    project.extra["docker.image.tags"] = containerImageTags

    val platform = System.getenv("TARGET_PLATFORM") ?: "amd64"
    val nativeBaseTag = "native-$platform"
    val nativeImageName = "ghcr.io/${containerImageName}:$nativeBaseTag"
    val nativeImageTags = listOf("$nativeImageName-$branch")

    project.extra["native.image.name"] = nativeImageName
    project.extra["native.image.tags"] = nativeImageTags

}

tasks.withType<BootRun> {
    jvmArgs(
        arrayOf(
            "--add-modules=jdk.incubator.vector",
            "-Dspring.config.additional-location=optional:file:/config/application.yaml,optional:file:/workspace/application.yaml",
            "-Dsun.jnu.encoding=UTF-8",
            "-Dfile.encoding=UTF-8"
        )
    )
}

tasks.withType<ProcessAot> {
    args(
        "--add-modules=jdk.incubator.vector",
        "-Dspring.config.additional-location=optional:file:/config/application.yaml,optional:file:/workspace/application.yaml",
        "-Dsun.jnu.encoding=UTF-8",
        "-Dfile.encoding=UTF-8"
    )
}

tasks.withType<BootBuildImage> {

    docker.publishRegistry.url = "ghcr.io"
    docker.publishRegistry.username = System.getenv("USERNAME") ?: "INVALID_USER"
    docker.publishRegistry.password = System.getenv("GITHUB_TOKEN") ?: "INVALID_PASSWORD"

    builder = "paketobuildpacks/builder-jammy-buildpackless-tiny"
    buildpacks = listOf(
        "paketobuildpacks/environment-variables",
        "paketobuildpacks/java-native-image",
        "paketobuildpacks/health-checker"
    )
    imageName = project.extra["native.image.name"] as String
    version = project.extra["docker.image.version"] as String
    tags = project.extra["native.image.tags"] as List<String>
    createdDate = "now"

    // It would also be possible to set this in the graalVmNative block, but we don't want to overwrite Spring's settings
    environment = mapOf(
        "BP_NATIVE_IMAGE" to "true",
        "BPL_SPRING_AOT_ENABLED" to "true",
        "BP_HEALTH_CHECKER_ENABLED" to "true",
        "BP_JVM_CDS_ENABLED" to "true",
        "BP_JVM_VERSION" to "23",
        "BPE_LANG" to "en_US.UTF-8",
        "BPE_LANGUAGE" to "LANGUAGE=en_US:en",
        "BPE_LC_ALL" to "en_US.UTF-8",
        "BP_NATIVE_IMAGE_BUILD_ARGUMENTS" to "-march=compatibility -H:+AddAllCharsets -J--add-modules=jdk.incubator.vector -J--patch-module=java.base=/workspace/BOOT-INF/classes/java.base"
    )
}