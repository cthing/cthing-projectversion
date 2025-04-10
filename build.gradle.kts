import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

repositories {
    mavenCentral()
}

plugins {
    `java-library`
    checkstyle
    jacoco
    `maven-publish`
    signing
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.versions)
}

val baseVersion = "2.0.1"
val isSnapshot = true

val isCIServer = System.getenv("CTHING_CI") != null
val now = System.currentTimeMillis()
val buildNumber = if (isCIServer) now.toString() else "0"
val buildDate: String = Instant.ofEpochMilli(now)
                               .atOffset(ZoneOffset.UTC)
                               .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

version = if (isSnapshot) "$baseVersion-$buildNumber" else baseVersion
group = "org.cthing"
description = "A version object for C Thing Software projects."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

// Dependency Restriction
//
// This project is a dependency of all C Thing Software projects. Therefore, to avoid circular
// dependencies, it should not depend on any C Thing Software project.
configurations.all {
    resolutionStrategy {
        eachDependency {
            val prohibitedGroups = listOf("org.cthing", "com.cthing")
            if (requested.group in prohibitedGroups) {
                throw GradleException("A dependency on '${requested.group}:${requested.name}' is prohibited.")
            }
        }
    }
}

dependencies {
    api(libs.jspecify)

    testImplementation(libs.assertJ)
    testImplementation(libs.equalsVerifier)
    testImplementation(libs.junitApi)
    testImplementation(libs.systemLambda)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)

    spotbugsPlugins(libs.spotbugsContrib)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("dev/checkstyle/checkstyle.xml")
    configDirectory = file("dev/checkstyle")
    isShowViolations = true
}

spotbugs {
    toolVersion = libs.versions.spotbugs
    ignoreFailures = false
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = file("dev/spotbugs/suppressions.xml")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<JavaCompile> {
        options.release = libs.versions.java.get().toInt()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }

    withType<Jar> {
        manifest.attributes(mapOf("Implementation-Title" to project.name,
                                  "Implementation-Vendor" to "C Thing Software",
                                  "Implementation-Version" to project.version))
    }

    withType<Javadoc> {
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
        with(options as StandardJavadocDocletOptions) {
            breakIterator(false)
            encoding("UTF-8")
            bottom("Copyright &copy; $year C Thing Software")
            addStringOption("Xdoclint:all,-missing", "-quiet")
            addStringOption("Werror", "-quiet")
            memberLevel = JavadocMemberLevel.PUBLIC
            outputLevel = JavadocOutputLevel.QUIET
        }
    }

    check {
        dependsOn(buildHealth)
    }

    spotbugsMain {
        reports.create("html").required = true
    }

    spotbugsTest {
        isEnabled = false
    }

    withType<JacocoReport> {
        dependsOn("test")
        with(reports) {
            xml.required = false
            csv.required = false
            html.required = true
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
        }
    }

    withType<Test> {
        useJUnitPlatform()

        // Required for com.github.stefanbirkner:system-lambda
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
        outputFormatter = "plain,xml,html"
        outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

val sourceJar by tasks.registering(Jar::class) {
    from(project.sourceSets["main"].allSource)
    archiveClassifier = "sources"
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.getByName("javadoc"))
    archiveClassifier = "javadoc"
}

publishing {
    publications {
        register("jar", MavenPublication::class) {
            from(components["java"])

            artifact(sourceJar)
            artifact(javadocJar)

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/cthing/${project.name}"
                organization {
                    name = "C Thing Software"
                    url = "https://www.cthing.com"
                }
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "baron"
                        name = "Baron Roberts"
                        email = "baron@cthing.com"
                        organization = "C Thing Software"
                        organizationUrl = "https://www.cthing.com"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/cthing/${project.name}.git"
                    developerConnection = "scm:git:git@github.com:cthing/${project.name}.git"
                    url = "https://github.com/cthing/${project.name}"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/cthing/${project.name}/issues"
                }
                ciManagement {
                    url = "https://github.com/cthing/${project.name}/actions"
                    system = "GitHub Actions"
                }
                properties.putAll(mapOf("cthing.build.date" to buildDate,
                                        "cthing.build.number" to buildNumber))
            }
        }
    }

    val repoUrl = if (isSnapshot) findProperty("cthing.nexus.snapshotsUrl") else findProperty("cthing.nexus.candidatesUrl")
    if (repoUrl != null) {
        repositories {
            maven {
                name = "CThingMaven"
                setUrl(repoUrl)
                credentials {
                    username = property("cthing.nexus.user") as String
                    password = property("cthing.nexus.password") as String
                }
            }
        }
    }
}

if (hasProperty("signing.keyId") && hasProperty("signing.password") && hasProperty("signing.secretKeyRingFile")) {
    signing {
        sign(publishing.publications["jar"])
    }
}
