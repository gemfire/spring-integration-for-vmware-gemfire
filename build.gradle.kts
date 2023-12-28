/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions

buildscript {
    dependencies {
        classpath("com.google.cloud:google-cloud-storage:2.30.1")
    }
}
plugins {
    id("java-library")
    id("gemfire-repo-artifact-publishing")
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
        exclude(module="commons-logging")
    }
    compileOnly(libs.gemfire.cq)

    testImplementation(platform(libs.junit.bom))

    testImplementation("org.springframework.integration:spring-integration-test-support")
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
            username = providers.gradleProperty("gemfireRepoUsername").get()
            password = providers.gradleProperty("gemfireRepoPassword").get()
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
    return getBaseVersion(project.findProperty("springIntegrationVersion").toString())
}

private fun getGemFireBaseVersion(): String {
    return getBaseVersion(project.findProperty("gemfireVersion").toString())
}

private fun getBaseVersion(version: String): String {
    val split = version.split(".")
    if (split.size < 2) {
        throw RuntimeException("version is malformed")
    }
    return "$split[0].$split[1]"
}

tasks.register("copyJavadocsToBucket") {
    val javadocJarTask = tasks.named("javadocJar")
    dependsOn(javadocJarTask)
    doLast {
        val storage = StorageOptions.newBuilder().setProjectId(project.findProperty("docsGCSProject").toString())
            .build().getService()
        val javadocJarFiles = javadocJarTask.get().outputs.files
        val blobId = BlobId.of(
            project.findProperty("docsGCSBucket").toString(),
            "${project.findProperty("pomProjectArtifactName")}/${project.version}/${javadocJarFiles.singleFile.name}"
        )
        val blobInfo = BlobInfo.newBuilder(blobId).build()
        storage.createFrom(blobInfo, javadocJarFiles.singleFile.toPath())
    }
}
