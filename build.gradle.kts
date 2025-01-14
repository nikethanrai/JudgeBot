group = "me.ddivad"
version = Versions.BOT
description = "judgebot"

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("me.jakejmattson:DiscordKt:${Versions.DISCORDKT}")
    implementation("org.litote.kmongo:kmongo-coroutine:4.2.8")
    implementation("joda-time:joda-time:2.10.10")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    shadowJar {
        archiveFileName.set("Judgebot.jar")
        manifest {
            attributes(
                "Main-Class" to "me.ddivad.judgebot.MainKt"
            )
        }
    }
}

object Versions {
    const val BOT = "1.0.0"
    const val DISCORDKT = "0.23.0-SNAPSHOT"
}