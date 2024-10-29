package uk.gov.justice.digital.hmpps.externalusersapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService

@Configuration
class AuditServiceConfig(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${spring.application.name}") private val applicationName: String,
) {
  @Bean
  fun hmppsAuditService(): HmppsAuditService {
    return HmppsAuditService(hmppsQueueService, objectMapper, applicationName)
  }
}
