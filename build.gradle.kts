/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions

buildscript {
    dependencies {
        classpath("com.google.cloud:google-cloud-storage:2.30.2")
    }
}
plugins {
    id("java-library")
    id("gemfire-repo-artifact-publishing")
    id("com.github.ben-manes.versions") version "0.50.0"
    id("nl.littlerobots.version-catalog-update") version "0.8.4"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Javadoc>("javadoc") {
    title =
        "Spring Integration ${getSpringIntegrationBaseVersion()} for VMware GemFire ${getGemFireBaseVersion()} Java API Reference"
    isFailOnError = false
}

publishingDetails {
    artifactName.set("spring-integration-6.1-gemfire-${getGemFireBaseVersion()}")
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

    testImplementation("org.springframework.integration:spring-integration-test-support"){
        exclude(module="mockito-core")
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
}

tasks.all {
    when (this) {
        is JavaForkOptions -> {
            jvmArgs("--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED")
        }
    }
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven {
        credentials {
            username = property("gemfireRepoUsername") as String
            password = property("gemfireRepoPassword") as String
        }
        url = uri("https://commercial-repo.pivotal.io/data3/gemfire-release-repo/gemfire")
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

private fun getSpringIntegrationBaseVersion(): String {
    return getBaseVersion(property("springIntegrationVersion").toString())
}

private fun getGemFireBaseVersion(): String {
    return getBaseVersion(property("gemfireVersion").toString())
}

private fun getBaseVersion(version: String): String {
    val split = version.split(".")
    if (split.size < 2) {
        throw RuntimeException("version is malformed")
    }
    return "${split[0]}.${split[1]}"
}

tasks.register("copyJavadocsToBucket") {
    val javadocJarTask = tasks.named("javadocJar")
    dependsOn(javadocJarTask)
    doLast {
        val storage = StorageOptions.newBuilder().setProjectId(project.properties["docsGCSProject"].toString())
            .build().getService()
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
        // keep all libraries that aren't used in the project
        keepUnusedLibraries = true
        // keep all plugins that aren't used in the project
        keepUnusedPlugins = true
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
