package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
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
              "userMessage":"User not within your groups: Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups",
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

  @Nested
  inner class RemoveUserRoleByUserId {

    @Test
    fun ` User Roles remove by userId role endpoint removes a role from a user`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02F/roles/licence_ro")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      checkRolesForUserId("90F930E1-2195-4AFD-92CE-0EB5672DA02F", listOf("GLOBAL_SEARCH"))
    }

    @Test
    fun `User Roles remove role by userId endpoint not allowed to remove a role from a user that isn't found`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02F/roles/licence_bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .json(
          """
             {
               "userMessage":"User role error: Modify role failed for field role with reason: role.notfound",
               "developerMessage":"Modify role failed for field role with reason: role.notfound"
             }
            """
            .trimIndent()
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint removes a role from a user that isn't on the user`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02F/roles/VIDEO_LINK_COURT_USER")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .json(
          """
             {
               "userMessage":"User role error: Modify role failed for field role with reason: role.missing",
               "developerMessage":"Modify role failed for field role with reason: role.missing"
             }
            """
            .trimIndent()
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint not allowed to remove a role from a user not in their group`() {
      webTestClient
        .delete().uri("/users/5105A589-75B3-4CA0-9433-B96228C1C8F3/roles/licence_ro")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
        .expectBody()
        .json(
          """
             {
               "userMessage":"User not within your groups: Unable to maintain user: AUTH_ADM with reason: User not with your groups",
               "developerMessage":"Unable to maintain user: AUTH_ADM with reason: User not with your groups"
             }
            """
            .trimIndent()
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint requires role`() {
      webTestClient
        .delete().uri("/users/5105A589-75B3-4CA0-9433-B96228C1C8F3/roles/licence_ro")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_GLOBAL_SEARCH")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
             {
               "userMessage":"Denied",
               "developerMessage":"Denied"
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
