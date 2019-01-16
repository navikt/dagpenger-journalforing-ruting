import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("application")
    kotlin("jvm") version "1.3.11"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("java-library")
    id("info.solidsoft.pitest") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

apply {
    plugin("com.diffplug.gradle.spotless")
    plugin("info.solidsoft.pitest")
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor/")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kittinunf/maven")
}

application {
    applicationName = "dagpenger-journalforing-ruting"
    mainClassName = "no.nav.dagpenger.journalføring.ruting.JournalføringRuting"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val kotlinLoggingVersion = "1.6.22"
val fuelVersion = "1.15.0"
val kafkaVersion = "2.0.0"
val confluentVersion = "4.1.2"
val prometheusVersion = "0.6.0"
val ktorVersion = "1.0.0"
val log4j2Version = "2.11.1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.dagpenger:streams:0.2.4-SNAPSHOT")
    implementation("no.nav.dagpenger:events:0.1.9-SNAPSHOT")
    implementation("no.nav.dagpenger:dagpenger-metrics:0.1.0-SNAPSHOT")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-gson:$fuelVersion")

    api("org.apache.kafka:kafka-clients:$kafkaVersion")
    api("org.apache.kafka:kafka-streams:$kafkaVersion")
    api("io.confluent:kafka-streams-avro-serde:$confluentVersion")

    compile("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.15")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
    testImplementation("com.github.tomakehurst:wiremock:2.19.0")
    testImplementation("no.nav:kafka-embedded-env:2.0.1")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint()
    }
}

pitest {
    threads = 4
    pitestVersion = "1.4.3"
    coverageThreshold = 80
    avoidCallsTo = setOf("kotlin.jvm.internal")
    timestampedReports = false
    targetClasses = setOf("no.nav.dagpenger.*")
}

tasks.getByName("test").finalizedBy("pitest")

tasks.withType<Test> {
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
