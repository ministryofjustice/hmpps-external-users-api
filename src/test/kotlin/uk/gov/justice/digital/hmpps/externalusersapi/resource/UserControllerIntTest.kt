package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class UserSearch {
    @Test
    fun `User search endpoint returns user data`() {
      webTestClient
        .get().uri("/users/search?name=test2&groups=&roles=")
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
        .get().uri("/users/search?name=test2&groups=&roles=&status=INACTIVE")
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
        .get().uri("/users/search?name=AUTH_DISABLED&groups=&roles=")
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
        .get().uri("/users/search?name=test2&groups=&roles=")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json("user_search_group_manager.json".readFile())
    }

    @Test
    fun `User search endpoint returns correct data when filtering by groups and roles`() {
      webTestClient
        .get().uri("/users/search?groups=SITE_1_GROUP_1&roles=LICENCE_RO")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json("user_search_groups_roles.json".readFile())
    }
  }

  @Nested
  inner class UsersByEmail {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/users?email=testy@testing.co.uk")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Is accessible to authorised user without roles`() {
      webTestClient.get().uri("/users?email=testy@testing.co.uk")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Responds with no content when email not present`() {
      webTestClient.get().uri("/users")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `Responds with no content when email empty`() {
      webTestClient.get().uri("/users?email=")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `Responds with content when email matches`() {
      webTestClient.get().uri("/users?email=auth_test2@digital.justice.gov.uk")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].userId").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$[0].userId").isEqualTo("5105a589-75b3-4ca0-9433-b96228c1c8f3")
        .jsonPath("$[0].username").isEqualTo("AUTH_ADM")
        .jsonPath("$[0].email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$[0].firstName").isEqualTo("Auth")
        .jsonPath("$[0].lastName").isEqualTo("Adm")
        .jsonPath("$[0].locked").isEqualTo(false)
        .jsonPath("$[0].enabled").isEqualTo(true)
        .jsonPath("$[0].verified").isEqualTo(true)
        .jsonPath("$[0].lastLoggedIn").isNotEmpty
        .jsonPath("$[0].inactiveReason").isEmpty
    }
  }
}
