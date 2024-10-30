package uk.gov.justice.digital.hmpps.externalusersapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.sqs.ConditionalOnAuditQueueDefinition
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService

@Configuration
class AuditServiceConfig {
  @Bean
  @ConditionalOnMissingBean
  @Conditional(ConditionalOnAuditQueueDefinition::class)
  fun hmppsAuditService(
    hmppsQueueService: HmppsQueueService,
    objectMapper: ObjectMapper,
    @Value("\${spring.application.name:}") applicationName: String?,
  ) = HmppsAuditService(hmppsQueueService, objectMapper, applicationName)
}
