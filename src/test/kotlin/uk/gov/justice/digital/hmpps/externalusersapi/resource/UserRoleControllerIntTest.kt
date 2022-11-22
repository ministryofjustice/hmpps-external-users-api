package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserRoleControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GetUserRolesByUserId {
    @Test
    fun `User Roles by userId endpoint returns user roles`() {
      checkRolesForUserId("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8", listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
    }

    @Test
    fun `User Roles by UserId endpoint returns user roles not allowed`() {
      webTestClient
        .get().uri("/users/5105A589-75B3-4CA0-9433-B96228C1C8F3/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `User Roles by userId endpoint returns user roles - Auth Admin`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).containsExactlyInAnyOrderElementsOf(listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
        }
    }

    @Test
    fun `User Roles by userId endpoint returns user roles - Group Manager`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).containsExactlyInAnyOrderElementsOf(listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
        }
    }

    @Test
    fun `User Roles by userId endpoint returns forbidden - user not in group manager group`() {
      webTestClient
        .get().uri("/users/9E84F1E4-59C8-4B10-927A-9CF9E9A30792/roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
              {
              "userMessage":"User group relationship exception: Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups",
              "developerMessage":"Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups"
              }
          """
            .trimIndent()
        )
    }

    private fun checkRolesForUserId(userId: String, roles: List<String>) {
      webTestClient
        .get().uri("/users/$userId/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).containsExactlyInAnyOrderElementsOf(roles)
        }
    }
  }
}
