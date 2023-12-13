import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML

plugins {
    java
    id("org.jetbrains.intellij")
    id("org.jetbrains.changelog")
    id("io.freefair.lombok")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "dev.flikas"
version = "222.17.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons", "commons-collections4", "4.4")
    implementation("org.apache.commons", "commons-lang3", "3.14.0")
    implementation("com.miguelfonseca.completely", "completely-core", "0.9.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testImplementation("org.mockito", "mockito-core", "2.12.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.1")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IC")
    version.set("2022.2")
    sameSinceUntilBuild.set(false)
    plugins.set(listOf("properties", "yaml", "maven", "gradle", "com.intellij.java"))
}

changelog {
    header.set(provider { "[${version.get()}] - ${date()}" })
}

tasks {
    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("")
        version.set(
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

        changeNotes.set(provider {
            changelog.run {
                getOrNull(version.get()) ?: getLatest()
            }.toHTML()
        })
    }

    signPlugin {
        cliVersion.set("0.1.8")
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