plugins {

  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
  kotlin("plugin.spring") version "1.9.22"
}

dependencyCheck {
  // Suppression till can upgrade to 3.2.5
  suppressionFiles.add("spring-suppressions.xml")
}
// Temporarily pin as can't upgrade to latest gradle plugin
ext["netty.version"] = "4.1.108.Final"

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

// Temporarily kept at 0.9.2 as get class java.lang.Long cannot be cast to class java.lang.Integer when upgrading to 1.0.0.RELEASE
val r2dbcPostgresVersion by extra("0.9.2.RELEASE")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-audit-sdk:1.0.0")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")

  implementation("org.hibernate.reactive:hibernate-reactive-core:2.2.2.Final")

  implementation("org.apache.commons:commons-text:1.11.0")
  implementation("com.google.guava:guava:33.1.0-jre")

  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:r2dbc-postgresql:$r2dbcPostgresVersion")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.7.3")
  implementation("io.opentelemetry:opentelemetry-api")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.4.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.wiremock:wiremock-standalone:3.4.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.19.7")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
