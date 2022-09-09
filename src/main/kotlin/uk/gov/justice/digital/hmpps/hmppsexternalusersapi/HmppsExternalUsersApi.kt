package uk.gov.justice.digital.hmpps.hmppsexternalusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsExternalUsersApi

fun main(args: Array<String>) {
  runApplication<HmppsExternalUsersApi>(*args)
}
