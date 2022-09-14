package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  basePackages = ["uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository"]
)
class AuthDbConfig
