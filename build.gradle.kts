import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {

  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
  kotlin("plugin.spring") version "2.3.0"
}

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-gson")
  implementation("org.springframework.security:spring-security-access")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.0")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  implementation("org.hibernate.reactive:hibernate-reactive-core:4.2.2.Final")

  implementation("org.apache.commons:commons-text:1.15.0")
  implementation("com.google.guava:guava:33.5.0-jre")

  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.7.10")
  implementation("io.opentelemetry:opentelemetry-api")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
  testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-gson-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.1")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xwhen-guards", "-Xannotation-default-target=param-property")
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}
