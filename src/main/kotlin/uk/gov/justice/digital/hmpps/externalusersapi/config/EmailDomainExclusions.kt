package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "application.email.domain.exclude")
data class EmailDomainExclusions(
  @Value("\${application.email.domain.exclude}") val exclude: Set<String>,) {

  fun contains(domain: String): Boolean {
    return exclude.contains(domain)
  }
}