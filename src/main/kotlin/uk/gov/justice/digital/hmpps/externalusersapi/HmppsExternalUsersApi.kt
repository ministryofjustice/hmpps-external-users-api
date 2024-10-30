package uk.gov.justice.digital.hmpps.externalusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsExternalUsersApi

fun main(args: Array<String>) {
  runApplication<HmppsExternalUsersApi>(*args)
}
