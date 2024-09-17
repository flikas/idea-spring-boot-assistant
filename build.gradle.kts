import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    java
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
    id("io.freefair.lombok")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

group = "dev.flikas"
version = "242.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("org.jetbrains.plugins.gradle")
        testFramework(TestFrameworkType.Platform)
        jetbrainsRuntime("21.0.4b598.4")
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }

    implementation("org.apache.commons", "commons-collections4", "4.4")
    implementation("org.apache.commons", "commons-lang3", "3.14.0")
    implementation("com.miguelfonseca.completely", "completely-core", "0.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito", "mockito-core", "2.12.0")
}

changelog {
    header.set(provider { "[${version.get()}] - ${date()}" })
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "242"
                untilBuild = provider { null }
            }
        }
    }
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }

    patchPluginXml {
        pluginVersion.set(
            project.version.toString().run {
                val pieces = split('-')
                if (pieces.size > 1) {
                    //if this is not a release version, generate a sub version number from count of hours from 2021-10-01.
                    pieces[0] + "." + (System.currentTimeMillis() / 1000 - 1633046400) / 60 / 60
                } else {
                    pieces[0]
                }
            }
        )

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString(
                separator = "\n",
                postfix = "\nProject [document](https://github.com/flikas/idea-spring-boot-assistant/#readme)\n"
            ).run { markdownToHTML(this) }
        )

        changeNotes = provider {
            changelog.renderItem(
                changelog
                    .getLatest()
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        }
    }

    signPlugin {
        val chain = rootProject.file("chain.crt")
        if (chain.exists()) {
            certificateChainFile.set(chain)
        } else {
            certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        }
        val private = rootProject.file("private.pem")
        if (private.exists()) {
            privateKeyFile.set(rootProject.file("private.pem"))
        } else {
            privateKey.set(System.getenv("PRIVATE_KEY"))
        }
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        if (!version.toString().contains('-')) {
            dependsOn("patchChangelog")
        }
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(version.toString().split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}