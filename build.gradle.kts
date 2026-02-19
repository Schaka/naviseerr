import net.nemerosa.versioning.VersioningExtension
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun
import org.gradle.api.tasks.bundling.Jar
import org.siouan.frontendgradleplugin.infrastructure.gradle.FrontendExtension
import org.springframework.boot.gradle.tasks.aot.ProcessAot

plugins {

    id("idea")
    id("org.springframework.boot") version "4.0.2"
    id("org.springframework.boot.aot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("net.nemerosa.versioning") version "3.1.0"
    id("org.siouan.frontend-jdk21") version "10.0.0"

    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"

}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.spring.io/snapshot")
    maven("https://repo.spring.io/milestone")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-kotlinx-serialization-json")
    implementation("org.springframework.data:spring-data-commons")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("io.github.openfeign:feign-core:13.8")
    implementation("io.github.openfeign:feign-jackson3:13.8")
    implementation("io.github.openfeign:feign-jaxb-jakarta:13.8")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    implementation("io.github.openfeign:feign-httpclient:13.8")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:1.0.0")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-java-time:1.0.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.apache.lucene:lucene-core:10.3.2")
    implementation("org.apache.lucene:lucene-queryparser:10.3.2")
    implementation("org.apache.lucene:lucene-analysis-common:10.3.2")


    implementation("org.slf4j:jcl-over-slf4j")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

configure<SpringBootExtension> {
    buildInfo()
}

configure<IdeaModel> {
    module {
        inheritOutputDirs = true
    }
}

configure<FrontendExtension> {
    nodeDistributionProvided.set(false)
    nodeVersion.set("24.13.1")
    nodeInstallDirectory.set(project.layout.projectDirectory.dir("node"))
    corepackVersion.set("latest")

    installScript.set("install")
    assembleScript.set("run build")

    packageJsonDirectory.set(project.layout.projectDirectory.dir("src/main/frontend"))
    cacheDirectory.set(project.layout.projectDirectory.dir(".frontend-gradle-plugin"))
}

val processFrontendResources by tasks.registering(Copy::class) {
    dependsOn("assembleFrontend")
    from(project.layout.projectDirectory.dir("src/main/frontend/dist/spa"))
    into(project.layout.buildDirectory.dir("resources/main/static"))
}

tasks.named("processResources") {
    dependsOn(processFrontendResources)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<BootJar> {
    archiveClassifier.set("")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_25.toString()
    targetCompatibility = JavaVersion.VERSION_25.toString()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        jvmTarget = JvmTarget.JVM_25
        javaParameters = true
    }
}

configure<VersioningExtension> {
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

    val repositoryOwner = System.getenv("REPOSITORY_OWNER") ?: "schaka"
    val containerImageName = "ghcr.io/${repositoryOwner.lowercase()}/${project.name}"

    val imageType = System.getenv("IMAGE_TYPE") ?: "jvm"
    val platform = System.getenv("TARGET_PLATFORM") ?: "amd64"
    val baseTag = "$imageType-$platform"

    val containerImageTags = listOf("$containerImageName:$baseTag", "$containerImageName:$baseTag-$shortCommit", "$containerImageName:$baseTag-$branch")

    project.extra["docker.image.name"] = containerImageName
    project.extra["docker.image.version"] = branch
    project.extra["docker.image.source"] = build.projectSourceRoot()
    project.extra["docker.image.tags"] = containerImageTags
}

tasks.withType<BootRun> {
    jvmArgs(
        arrayOf(
            "-Dspring.config.additional-location=optional:/config/application.yml",
        )
    )
}

tasks.withType<ProcessAot> {
    args(
        "-Dspring.config.additional-location=optional:/config/application.yml",
    )
}

tasks.withType<BootBuildImage> {

    docker.publishRegistry.url = "ghcr.io"
    docker.publishRegistry.username = System.getenv("REPOSITORY_OWNER") ?: "INVALID_USER"
    docker.publishRegistry.password = System.getenv("GITHUB_TOKEN") ?: "INVALID_PASSWORD"

    builder = "paketobuildpacks/ubuntu-noble-builder-buildpackless"
    buildpacks = listOf(
        "paketobuildpacks/environment-variables",
        "paketobuildpacks/adoptium",
        "paketobuildpacks/java",
    )
    imageName = project.extra["docker.image.name"] as String
    version = project.extra["docker.image.version"] as String
    tags = project.extra["docker.image.tags"] as List<String>
    createdDate = "now"

    environment = mapOf(
        "BP_NATIVE_IMAGE" to "false",
        "BP_JVM_AOTCACHE_ENABLED" to "false",
        "BP_SPRING_AOT_ENABLED" to "false",
        "BP_JVM_VERSION" to "25",
        "LC_ALL" to "en_US.UTF-8",
        "BPE_LC_ALL" to "en_US.UTF-8",
        "BPE_BPL_JAVA_NMT_ENABLED" to "false",
        "BPE_BPL_JVM_THREAD_COUNT" to "15",
        "BPE_BPL_JVM_HEAD_ROOM" to "1",
        "BPE_BPL_JVM_LOADED_CLASS_COUNT" to "15000",
        "TRAINING_RUN_JAVA_TOOL_OPTIONS" to "-XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders -Dspring.profiles.active=leyden",
        "BPE_SPRING_CONFIG_ADDITIONAL_LOCATION" to "optional:/config/application.yml",
        "BPE_PREPEND_JAVA_TOOL_OPTIONS" to "-XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders",
        "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
        "BPE_APPEND_JAVA_TOOL_OPTIONS" to "-XX:ReservedCodeCacheSize=30M -Xss200K -Xlog:cds=info -Xlog:aot=info -Xlog:class+path=info",
    )
}
