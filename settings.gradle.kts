/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.FileInputStream
import java.util.*

pluginManagement {
    includeBuild("build-tools/publishing")
    includeBuild("build-tools/convention-plugins")
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
            gradlePluginPortal()
        }
    }
}
rootProject.name = "spring-integration-gemfire"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val properties = Properties()
            properties.load(FileInputStream("gradle.properties"))
            versionOverrideFromProperties(this, properties)
        }
    }
}

private fun versionOverrideFromProperty(versionCatalogBuilder: VersionCatalogBuilder, propertyName: String, propertiesFile: Properties): String {
    val propertyValue = providers.systemProperty(propertyName).getOrElse(propertiesFile.getProperty(propertyName))
    return versionCatalogBuilder.version(propertyName, propertyValue)
}

private fun versionOverrideFromProperties(versionCatalogBuilder: VersionCatalogBuilder, properties: Properties) {
    versionOverrideFromProperty(versionCatalogBuilder, "gemfireVersion", properties)
    versionOverrideFromProperty(versionCatalogBuilder, "springDataGemFireVersion", properties)
    versionOverrideFromProperty(versionCatalogBuilder, "springIntegrationVersion", properties)
}
