/**
 * Copyright 2021-2022 The kage Authors. All rights reserved. Use of this source code is governed by
 * either an Apache 2.0 or MIT license at your discretion, that can be found in the LICENSE-APACHE
 * or LICENSE-MIT files respectively.
 */
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import info.solidsoft.gradle.pitest.PitestTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.animalsniffer)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kover)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.pitest) apply false
  alias(libs.plugins.spotless)
}

group = requireNotNull(project.findProperty("GROUP"))

version = requireNotNull(project.findProperty("VERSION_NAME"))

kotlin {
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class) abiValidation()

  jvm {
    compilerOptions {
      moduleName = "kage"
      jvmTarget = JvmTarget.JVM_17
    }
  }

  sourceSets {
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.assertk)
    }

    jvmMain.dependencies {
      implementation(project.dependencies.platform(libs.junit.bom.get()))
      implementation(libs.bouncycastle.bcprov)
      implementation(libs.hkdf)
      implementation(libs.kotlinresult)
    }

    jvmTest.dependencies {
      implementation(libs.junit.jupiter)
      implementation(libs.junit.jupiter.api)
      runtimeOnly(libs.junit.jupiter.engine)
      runtimeOnly(libs.junit.platform.launcher)
    }
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
  @Suppress("UnstableApiUsage") pomFromGradleProperties()
  configure(
    KotlinMultiplatform(
      javadocJar = JavadocJar.Dokka("dokkaGenerate"),
      sourcesJar = SourcesJar.Sources(),
    )
  )
}

// PIT is JVM-only, and its Gradle plugin requires the Java plugin that KMP rejects. Keep the plugin
// on the build-script classpath without applying it, then wire its task type only to the JVM
// target.
val pitestJvmClasspath = configurations.create("pitestJvmClasspath")

val pitestJvm =
  tasks.register<PitestTask>("pitestJvm") {
    group = "verification"
    description = "Runs PIT mutation analysis against the KMP JVM target."
    dependsOn("jvmTestClasses")

    launchClasspath.from(pitestJvmClasspath)
    additionalClasspath.from(configurations.named("jvmTestRuntimeClasspath"))
    additionalClasspath.from(layout.buildDirectory.dir("classes/kotlin/jvm/test"))
    mutableCodePaths.from(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
    sourceDirs.from("src/jvmMain/kotlin")

    testPlugin.set("junit5")
    targetClasses.set(setOf("kage.*"))
    targetTests.set(setOf("kage.*"))
    avoidCallsTo.set(setOf("kotlin.jvm.internal"))
    mutators.set(setOf("STRONGER"))
    threads.set(Runtime.getRuntime().availableProcessors())
    outputFormats.set(setOf("XML", "HTML"))
    mutationThreshold.set(73)
    coverageThreshold.set(90)
    failWhenNoMutations.set(true)
    timestampedReports.set(false)
    useAdditionalClasspathFile.set(true)
    additionalClasspathFile.set(layout.buildDirectory.file("tmp/pitestJvm-classpath.txt"))
    reportDir.set(layout.buildDirectory.dir("reports/pitestJvm"))
    historyInputLocation.set(layout.buildDirectory.file("reports/pitestJvm/history.bin"))
    historyOutputLocation.set(layout.buildDirectory.file("reports/pitestJvm/history.bin"))
    defaultFileForHistoryData.set(layout.buildDirectory.file("reports/pitestJvm/history.bin"))
    jvmPath.set(file("${System.getProperty("java.home")}/bin/java"))
    rootDir = projectDir
  }

tasks.named("check") { dependsOn(pitestJvm) }

// AnimalSniffer protects the published JVM API. Test code intentionally uses newer JDK helpers.
tasks.named("animalsnifferJvmTest") { enabled = false }

spotless {
  val ktfmtVersion = "0.64"
  kotlin {
    ktfmt(ktfmtVersion).googleStyle()
    target("**/*.kt")
    targetExclude("**/build/")
    licenseHeaderFile("spotless.license", "package")
  }
  kotlinGradle {
    ktfmt(ktfmtVersion).googleStyle()
    target("**/*.kts")
    licenseHeaderFile("spotless.license", "package |import|enableFeaturePreview")
  }
}

dependencies {
  signature(variantOf(libs.animalsniffer.signature.android) { artifactType("signature") })
  add(pitestJvmClasspath.name, libs.pitest.command.line)
  add(pitestJvmClasspath.name, libs.pitest.junit5)
  add(pitestJvmClasspath.name, libs.pitest.kotlin)
}

tasks.withType<Test>().configureEach {
  maxParallelForks = Runtime.getRuntime().availableProcessors() * 2
  testLogging { events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
  useJUnitPlatform()
}
