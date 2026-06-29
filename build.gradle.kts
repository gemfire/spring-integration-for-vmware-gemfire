/*
 * Copyright $originalComment.match("Copyright \(c\) VMware, Inc. (\d+)", 1, "-", $today.year)$originalComment.match("Copyright (\d+)", 1, "-", $today.year)2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright $originalComment.match("Copyright \(c\) VMware, Inc. (\d+)", 1, "-", $today.year)$originalComment.match("Copyright (\d+)", 1, "-", $today.year)2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import nl.littlerobots.vcu.plugin.versionSelector
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import java.io.FileInputStream

buildscript {
  repositories {
    val repositoryConfigFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
      providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
    )

    val jsonString = File(repositoryConfigFilePath).readText(Charsets.UTF_8)
    val repositories = groovy.json.JsonSlurper().parseText(jsonString) as Map<*, *>
    (repositories["repositories"] as List<*>).filterNotNull().map { entry -> entry as Map<*, *> }
      .forEach { entry ->
        entry.apply {
          maven {
            url = uri(entry["url"]!! as String)
            if (!entry["username"]?.toString().isNullOrBlank()) {
              credentials {
                username = entry["username"] as String
                password = entry["password"] as String
              }
            }
          }
        }
      }
    if (providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()) {
      mavenCentral()
    }
  }
  dependencies {
    classpath("com.google.cloud:google-cloud-storage:2.30.2")
  }
}

plugins {
  id("java-library")
  id("gemfire-repo-artifact-publishing")
  alias(libs.plugins.littlerobots.version.catalog.update)
  id("gemfire-artifactory")
}

java {
  toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

java {
  withJavadocJar()
  withSourcesJar()
}

val baseGemFireVersion: String by project
val baseSpringVersion: String by project

tasks.named<Javadoc>("javadoc") {
  title =
    "Spring Integration ${baseSpringVersion} for VMware GemFire ${baseGemFireVersion} Java API Reference"
  isFailOnError = false
}

publishingDetails {
  artifactName.set("spring-integration-${baseSpringVersion}-gemfire-${baseGemFireVersion}")
  longName.set("Spring Integration for VMware GemFire")
  description.set("Spring Integration For VMware GemFire")
}

dependencies {
  implementation(platform(libs.spring.integration.bom))
  api("org.springframework.integration:spring-integration-core")
  api(libs.spring.data.gemfire) {
    exclude(group = "org.springframework")
    exclude(module = "shiro-event")
    exclude(module = "shiro-lang")
    exclude(module = "shiro-crypto-hash")
    exclude(module = "shiro-crypto-cipher")
    exclude(module = "shiro-config-ogdl")
    exclude(module = "shiro-config-core")
    exclude(module = "shiro-cache")
    exclude(module = "commons-logging")
  }
  api(libs.commons.io)
  compileOnly(libs.gemfire.core) {
    exclude(module = "commons-logging")
  }
  compileOnly(libs.gemfire.cq)

  testImplementation(platform(libs.junit.bom))

  testImplementation("org.springframework.integration:spring-integration-test-support") {
    exclude(module = "mockito-core")
  }
  testImplementation(libs.gemfire.core) {
    exclude(module = "commons-logging")
  }
  testImplementation(libs.gemfire.cq)
  testImplementation("junit:junit")
  testImplementation(libs.assertj.core)
  testImplementation(libs.spring.test)
  testImplementation(libs.mockito.core)
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation(libs.log4j.over.slf4j)
  testImplementation(libs.logback.classic)
  testImplementation(libs.gemfire.testcontainers)
}

tasks.all {
  when (this) {
    is JavaForkOptions -> {
      jvmArgs("--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED")
    }
  }
}

tasks {
  test {
    useJUnitPlatform()
    val springTestGemfireDockerImage: String by project
    systemProperty("spring.test.gemfire.docker.image", springTestGemfireDockerImage)
  }
}

tasks.getByName<Test>("test") {
  forkEvery = 1
  maxParallelForks = 4
  val springTestGemfireDockerImage: String by project
  systemProperty("spring.test.gemfire.docker.image", springTestGemfireDockerImage)
}

tasks.register("copyJavadocsToBucket") {
  val javadocJarTask = tasks.named("javadocJar")
  dependsOn(javadocJarTask)
  doLast {
    val storage =
      StorageOptions.newBuilder().setProjectId(project.properties["docsGCSProject"].toString()).setCredentials(
        GoogleCredentials.fromStream(FileInputStream(project.properties["docsGCSProjectCredentials"].toString()))).build().getService()
    val javadocJarFiles = javadocJarTask.get().outputs.files
    val blobId = BlobId.of(
      project.properties["docsGCSBucket"].toString(),
      "${publishingDetails.artifactName.get()}/${project.version}/${javadocJarFiles.singleFile.name}"
    )
    val blobInfo = BlobInfo.newBuilder(blobId).build()
    storage.createFrom(blobInfo, javadocJarFiles.singleFile.toPath())
  }
}

versionCatalogUpdate {
  sortByKey = true
  keep {
    keepUnusedVersions = true
  }
  versionSelector {
    val allowMajor = project.hasProperty("updateMajor")
    val allowMinor = project.hasProperty("updateMinor")
    isAllowedUpdate(it.candidate.version, it.currentVersion, allowMajor, allowMinor)
  }
  versionCatalogs {
    create("publishCatalog") {
      catalogFile = file("gradle/publishing.versions.toml")
    }
  }
}

fun isAllowedUpdate(
  candidateVersion: String,
  currentVersion: String,
  allowMajor: Boolean,
  allowMinor: Boolean
): Boolean {
  val nonStableMarkers = listOf("alpha", "beta", "rc", "snapshot", "dev", "preview", "build", "milestone")
  if (nonStableMarkers.any { candidateVersion.contains(it, ignoreCase = true) }) return false
  if (candidateVersion.contains(Regex("""[.\-][Mm]\d"""))) return false

  val cleanCurrentVersion = if (currentVersion.startsWith("[") || currentVersion.startsWith("(")) {
    currentVersion.replace("[", "").replace("]", "").replace("(", "").replace(")", "")
      .split(",").first().trim()
  } else currentVersion

  if (allowMajor) return true

  fun parseMajorMinor(v: String): Pair<Int, Int>? {
    val parts = v.split(".")
    val major = parts.getOrNull(0)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return null
    val minor = parts.getOrNull(1)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return null
    return major to minor
  }

  val (currentMajor, currentMinor) = parseMajorMinor(cleanCurrentVersion) ?: return false
  val (candidateMajor, candidateMinor) = parseMajorMinor(candidateVersion) ?: return false

  if (currentMajor != candidateMajor) return false
  if (allowMinor) return true
  return currentMinor == candidateMinor
}
