package uk.gov.justice.digital.hmpps.externalusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication()
@ComponentScan(basePackages = ["uk.gov.justice.digital.hmpps", "uk.gov.justice.digital.hmpps.hmppsauditsdk"])
class HmppsExternalUsersApi

fun main(args: Array<String>) {
  runApplication<HmppsExternalUsersApi>(*args)
}
