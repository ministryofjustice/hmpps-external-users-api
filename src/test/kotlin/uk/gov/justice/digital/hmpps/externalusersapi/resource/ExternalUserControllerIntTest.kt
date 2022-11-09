package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class ExternalUserControllerIntTest : IntegrationTestBase() {

  @Test
  fun `User search endpoint returns user data`() {
    webTestClient
      .get().uri("/user/search?name=test2&groups=&roles=")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_search.json".readFile())
  }

  @Test
  fun `User search endpoint filters by status`() {
    webTestClient
      .get().uri("/user/search?name=test2&groups=&roles=&status=INACTIVE")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_search_no_results.json".readFile())
  }

  @Test
  fun `User search endpoint returns user data sorted by last name`() {
    webTestClient
      .get().uri("/user/search?name=AUTH_DISABLED&groups=&roles=")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_search_order_by_lastname.json".readFile())
  }

  @Test
  fun `User search endpoint returns user data for group managers`() {
    webTestClient
      .get().uri("/user/search?name=test2&groups=&roles=")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_search_group_manager.json".readFile())
  }
}
