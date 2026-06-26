/*
 * Copyright $originalComment.match("Copyright \(c\) VMware, Inc. (\d+)", 1, "-", $today.year)$originalComment.match("Copyright (\d+)", 1, "-", $today.year)2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    includeBuild("build-tools/publishing")
    includeBuild("build-tools/convention-plugins")
    repositories {
        if (providers.gradleProperty("useMavenLocal").getOrElse("false").toBoolean()) {
            mavenLocal()
        }
        val repositoryConfigFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
            providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
        )
        val jsonString = File(repositoryConfigFilePath).readText(Charsets.UTF_8)
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
        if (providers.gradleProperty("useMavenCentral").getOrElse("true").toBoolean()) {
            gradlePluginPortal()
        }
    }
}

rootProject.name = "spring-integration-gemfire"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        addGemFireRepositories(
            providers,
            addMavenLocal = providers.gradleProperty("useMavenLocal").getOrElse("false").toBoolean(),
            addMavenCentral = providers.gradleProperty("useMavenCentral").getOrElse("true").toBoolean()
        )
    }
    versionCatalogs {
        create("libs") {
            overrideProperty("gemfireVersion")
            overrideProperty("springDataGemFireVersion")
            overrideProperty("springIntegrationVersion")
        }
    }
}

fun VersionCatalogBuilder.overrideProperty(property: String) {
    val value = System.getProperty(property)
        ?: (settings as? ExtensionAware)?.extensions?.extraProperties?.let {
            if (it.has(property)) it.get(property) as? String else null
        }
    if (value != null) {
        logger.debug("Overriding $property: $value")
        version(property, value)
    }
}

fun org.gradle.api.artifacts.dsl.RepositoryHandler.addGemFireRepositories(
    providers: org.gradle.api.provider.ProviderFactory,
    addGradlePluginPortal: Boolean = false,
    addMavenLocal: Boolean = false,
    addMavenCentral: Boolean = false
) {
    if (addMavenLocal) mavenLocal()
    val configFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
        providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
    )
    val jsonString = java.io.File(configFilePath).readText(Charsets.UTF_8)
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
