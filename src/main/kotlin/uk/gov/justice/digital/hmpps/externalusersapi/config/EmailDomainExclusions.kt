package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class EmailDomainExclusions(
  @Value("\${application.email.domain.exclude}") val exclude: Set<String>,
) {
  fun contains(domain: String): Boolean = exclude.contains(domain)
}
