/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
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
  id("commercial-repositories")
  alias(libs.plugins.ben.manes.versions)
  alias(libs.plugins.littlerobots.version.catalog.update)
  id("gemfire-artifactory")
}

java {
  toolchain { languageVersion = JavaLanguageVersion.of(17) }
  withJavadocJar()
  withSourcesJar()
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.named<Javadoc>("javadoc") {
  title =
    "Spring Integration ${baseSpringIntegrationVersion} for VMware GemFire ${baseGemFireVersion} Java API Reference"
  isFailOnError = false
}

val baseGemFireVersion: String by project
val baseSpringIntegrationVersion: String by project

publishingDetails {
  artifactName.set("spring-integration-${baseSpringIntegrationVersion}-gemfire-${baseGemFireVersion}")
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
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation(libs.log4j.over.slf4j)
  testImplementation(libs.logback.classic)
  testImplementation(libs.gemfire.testcontainers)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
  val additionalMavenRepoURLs = project.findProperty("additionalMavenRepoURLs").toString()
  if (!additionalMavenRepoURLs.isNullOrBlank() && additionalMavenRepoURLs.isNotEmpty()) {
    additionalMavenRepoURLs.split(",").forEach {
      project.repositories.maven {
        this.url = uri(it)
      }
    }
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
  // These options will be set as default for all version catalogs
  sortByKey = true
  // Referenced that are pinned are not automatically updated.
  // They are also not automatically kept however (use keep for that).
  pin {
  }
  keep {
    keepUnusedVersions = true
  }
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    !isPatch(candidate.version, currentVersion)
  }
}

fun isPatch(candidateVersion: String, currentVersion: String): Boolean {
  val candidateSplit = candidateVersion.split(".")
  val currentSplit = currentVersion.split(".")

  if (candidateSplit.size == currentSplit.size && currentSplit.size == 3) {
    if (candidateSplit[0] != currentSplit[0]) {
      return false
    }
    if (candidateSplit[1] != currentSplit[1]) {
      return false
    }
  }
  return true
}
