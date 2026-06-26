/*
 * Copyright $originalComment.match("Copyright \(c\) VMware, Inc. (\d+)", 1, "-", $today.year)$originalComment.match("Copyright (\d+)", 1, "-", $today.year)2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
  id("groovy-gradle-plugin")
  `kotlin-dsl`
}

repositories {
  addGemFireRepositories(
    providers,
    addMavenLocal = providers.gradleProperty("useMavenLocal").getOrElse("false").toBoolean(),
    addMavenCentral = providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()
  )
}

dependencies {
  implementation(libs.kotlin)
  implementation(gradleApi())
  implementation("org.jfrog.buildinfo:build-info-extractor-gradle:5.2.2")
}

gradlePlugin {
  plugins.register("gemfire-artifactory") {
    id = "gemfire-artifactory"
    implementationClass = "com.vmware.gemfire.gradle.ArtifactoryPlugin"
  }
}

fun RepositoryHandler.addGemFireRepositories(
  providers: ProviderFactory,
  addGradlePluginPortal: Boolean = false,
  addMavenLocal: Boolean = false,
  addMavenCentral: Boolean = false
) {
  if (addMavenLocal) mavenLocal()
  val configFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
    providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
  )
  val jsonString = File(configFilePath).readText(Charsets.UTF_8)
  val repos = groovy.json.JsonSlurper().parseText(jsonString) as Map<*, *>
  (repos["repositories"] as List<*>).filterNotNull().map { it as Map<*, *> }
    .forEach { entry ->
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
  if (addGradlePluginPortal) gradlePluginPortal()
  if (addMavenCentral) mavenCentral()
}
