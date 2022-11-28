package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User

@Service
class ManageUsersApiService(
  private val manageUsersWebClient: WebClient,
) {

  suspend fun sendEnableEmail(user: User, admin: String): ResponseEntity<Void> =
    manageUsersWebClient.post()
      .uri("/notify/enable-user")
      .body(
        BodyInserters.fromValue(
          mapOf(
            "username" to user.name,
            "firstName" to user.firstName,
            "admin" to admin,
            "email" to user.email
          )
        )
      )
      .retrieve()
      .awaitBodilessEntity()
}
