plugins {
    groovy
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
}

group = "squad.nononotaa"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.playwright)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useSpock(libs.versions.spock)
            dependencies {
                implementation(libs.groovy)
                implementation(libs.bytebuddy)
                implementation(libs.objenesis)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            useSpock(libs.versions.spock)
            dependencies {
                implementation(project())
                implementation(libs.groovy)
                implementation(libs.groovy.json)
                implementation(libs.spock.spring)
                implementation(libs.spring.boot.starter.webmvc.test)
                implementation(libs.spring.boot.testcontainers)
                implementation(libs.testcontainers.mongodb)
                runtimeOnly(libs.junit.platform.launcher)
            }
            targets.all {
                testTask.configure {
                    shouldRunAfter(test)
                    val snapshot = rootProject.file("../frontend/apps/web/lib/api/openapi.json")
                    inputs.file(snapshot).withPropertyName("openApiSnapshot")
                    systemProperty("openapi.snapshot", snapshot.absolutePath)
                    systemProperty("openapi.update", providers.gradleProperty("updateOpenApi").isPresent)
                }
            }
        }
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.implementation.get())
}

tasks.withType<Test>().configureEach {
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

tasks.withType<GroovyCompile>().configureEach {
    groovyOptions.isParameters = true
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
    groovy {
        target("src/**/*.groovy")
        greclipse().configFile("config/spotless/greclipse.properties")
        importOrder()
        removeSemicolons()
    }
}
