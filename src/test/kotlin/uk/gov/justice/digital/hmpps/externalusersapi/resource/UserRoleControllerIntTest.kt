package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserRoleControllerIntTest : IntegrationTestBase() {

  val authAdmId = "5105A589-75B3-4CA0-9433-B96228C1C8F3"
  val authRoUserTest5Id = "90F930E1-2195-4AFD-92CE-0EB5672DA44B"
  val authRoUserTest6Id = "90F930E1-2195-4AFD-92CE-0EB5672DA02F"
  val authRoVaryUserId = "5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8"

  @Nested
  inner class GetUserRolesByUserId {

    @Test
    fun `User Roles by userId endpoint returns user roles`() {
      checkRolesForUserId(authRoVaryUserId, listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
    }

    @Test
    fun `User Roles by UserId endpoint returns user roles not allowed`() {
      webTestClient
        .get().uri("/users/$authAdmId/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `User Roles by userId endpoint returns user roles - Auth Admin`() {
      webTestClient
        .get().uri("/users/$authRoVaryUserId/roles")
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
        .get().uri("/users/$authRoVaryUserId/roles")
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
      val authMfaExpiredId = "9E84F1E4-59C8-4B10-927A-9CF9E9A30792"
      webTestClient
        .get().uri("/users/$authMfaExpiredId/roles")
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
            .trimIndent(),
        )
    }
  }

  @Nested
  inner class AddUserRolesByUserId {

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.post().uri("/users/$authRoUserTest5Id/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden without correct role`() {
      webTestClient.post().uri("/users/$authRoUserTest5Id/roles")
        .headers(setAuthorisation("bob", listOf()))
        .body(BodyInserters.fromValue(listOf("ANY_OLD_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `User Roles add role by userId user not found`() {
      webTestClient
        .post().uri("/users/12345678-1234-1234-1234-123456789ABC/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(listOf("ANY_OLD_ROLE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .json(
          """
             {
               "userMessage":"User not found: User 12345678-1234-1234-1234-123456789abc not found",
               "developerMessage":"User 12345678-1234-1234-1234-123456789abc not found"
             }
            """
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles add role by userId endpoint adds a role that doesn't exist`() {
      webTestClient
        .post().uri("/users/$authRoUserTest5Id/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(listOf("ROLE_DOES_NOT_EXIST", "LICENCE_RO")))
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles fails to add role by userId endpoint adds a role to a user that already exists`() {
      webTestClient
        .post().uri("/users/$authRoUserTest6Id/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(listOf("GLOBAL_SEARCH", "LICENCE_RO")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody()
        .json(
          """
             {
               "userMessage":"User role error: Modify role failed for field role with reason: role.exists",
               "developerMessage":"Modify role failed for field role with reason: role.exists"
             }
            """
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles add role by userId endpoint not allowed to add a role to a user not in their group`() {
      webTestClient
        .post().uri("/users/$authAdmId/roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(listOf("GLOBAL_SEARCH", "LICENCE_RO")))
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles add role by userId POST endpoint adds a role to a user as superuser`() {
      val authAddRoleTest2Id = "90F930E1-2195-4AFD-92CE-0EB5672DA02E"
      webTestClient
        .post().uri("/users/$authAddRoleTest2Id/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(listOf("GLOBAL_SEARCH", "LICENCE_RO")))
        .exchange()
        .expectStatus().isNoContent

      checkRolesForUserId(authAddRoleTest2Id, listOf("GLOBAL_SEARCH", "LICENCE_RO"))

      // Tidy up - Reset user roles to original state
      removeRoleForUserId(authAddRoleTest2Id, "GLOBAL_SEARCH")
      removeRoleForUserId(authAddRoleTest2Id, "LICENCE_RO")
    }

    @Test
    fun `User Roles add role by userId POST endpoint adds a role to a user as group manager`() {
      val authRoUserTest2Id = "90F930E1-2195-4AFD-92CE-0EB5672DA02B"

      webTestClient
        .post().uri("/users/$authRoUserTest2Id/roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(listOf("LICENCE_RO")))
        .exchange()
        .expectStatus().isNoContent

      checkRolesForUserId(authRoUserTest2Id, listOf("GLOBAL_SEARCH", "LICENCE_RO"))

      // Tidy up - Reset user roles to original state
      removeRoleForUserId(authRoUserTest2Id, "LICENCE_RO")
    }
  }

  @Nested
  inner class RemoveUserRoleByUserId {

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.delete().uri("/users/$authRoUserTest5Id/roles/ANY_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden without correct role`() {
      webTestClient.delete().uri("/users/$authRoUserTest5Id/roles/ANY_ROLE")
        .headers(setAuthorisation("bob", listOf()))
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles remove role by userId user not found`() {
      webTestClient
        .delete().uri("/users/12345678-1234-1234-1234-123456789ABC/roles/ANY_ROLE")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .json(
          """
             {
               "userMessage":"User not found: User 12345678-1234-1234-1234-123456789abc not found",
               "developerMessage":"User 12345678-1234-1234-1234-123456789abc not found"
             }
            """
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint not allowed to remove a role from a user that isn't found`() {
      webTestClient
        .delete().uri("/users/$authRoUserTest6Id/roles/licence_bob")
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint removes a role from a user that isn't on the user`() {
      webTestClient
        .delete().uri("/users/$authRoUserTest6Id/roles/VIDEO_LINK_COURT_USER")
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles remove role by userId endpoint not allowed to remove a role from a user not in their group`() {
      webTestClient
        .delete().uri("/users/$authAdmId/roles/licence_ro")
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
            .trimIndent(),
        )
    }

    @Test
    fun `User Roles remove by userId role endpoint successfully removes a role from a user`() {
      webTestClient
        .delete().uri("/users/$authRoUserTest6Id/roles/licence_ro")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      checkRolesForUserId(authRoUserTest6Id, listOf("GLOBAL_SEARCH"))

      // Tidy up - Reset user roles to original state
      addRoleForUserId(authRoUserTest6Id, "LICENCE_RO")
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
  inner class AssignableRoles {

    @Test
    fun `access forbidden without valid token`() {
      webTestClient
        .get().uri("/users/$authRoUserTest5Id/assignable-roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access allowed without role`() {
      webTestClient
        .get().uri("/users/$authAdmId/assignable-roles")
        .headers(setAuthorisation("bob", listOf()))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].roleCode").isEmpty
    }

    @Test
    fun `Assignable User Roles by userId user not found`() {
      webTestClient
        .get().uri("/users/12345678-1234-1234-1234-123456789ABC/assignable-roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .json(
          """
             {
               "userMessage":"User not found: User 12345678-1234-1234-1234-123456789abc not found",
               "developerMessage":"User 12345678-1234-1234-1234-123456789abc not found"
             }
            """
            .trimIndent(),
        )
    }

    @Test
    fun `Assignable User Roles by userId endpoint returns all assignable user roles for a group for admin maintainer`() {
      webTestClient
        .get().uri("/users/$authRoVaryUserId/assignable-roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).hasSizeGreaterThan(5)
          assertThat(it).contains("PECS_COURT")
        }
    }

    @Test
    fun `Assignable User Roles by userId endpoint returns all assignable user roles for a group for group manager`() {
      val AUTH_RO_USER_TEST2_ID = "90F930E1-2195-4AFD-92CE-0EB5672DA02B"
      webTestClient
        .get().uri("/users/$AUTH_RO_USER_TEST2_ID/assignable-roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).containsExactlyInAnyOrder("LICENCE_RO", "LICENCE_VARY")
        }
    }
  }

  @Nested
  inner class SearchableRoles {

    @Test
    fun `access unauthorized without valid token`() {
      webTestClient
        .get().uri("/users/me/searchable-roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Searchable roles for group manager user returns their roles based on the groups they manage`() {
      webTestClient
        .get().uri("/users/me/searchable-roles")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER2", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """
       [{"roleCode":"PF_POLICE","roleName":"Pathfinder Police"}]
          """.trimIndent(),
        )
    }

    @Test
    fun `Searchable roles for user with MAINTAIN_OAUTH_USERS role returns all roles excluding OAUTH_ADMIN`() {
      webTestClient
        .get().uri("/users/me/searchable-roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).hasSizeGreaterThan(22)
          assertThat(it).contains("MAINTAIN_OAUTH_USERS")
          assertThat(it).doesNotContain("OAUTH_ADMIN")
        }
    }

    @Test
    fun `Searchable roles for user with MAINTAIN_OAUTH_USERS and OAUTH_ADMIN role returns all roles`() {
      webTestClient
        .get().uri("/users/me/searchable-roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_OAUTH_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).hasSizeGreaterThan(23)
          assertThat(it).contains("AUTH_GROUP_MANAGER")
          assertThat(it).contains("OAUTH_ADMIN")
        }
    }

    @Test
    fun `Searchable roles for User without MAINTAIN_OAUTH_USERS role and has no groups will not return any roles`() {
      webTestClient
        .get().uri("/users/me/searchable-roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).hasSize(0)
        }
    }
  }

  @Nested
  inner class ListOfRolesForUser {
    @Test
    fun `access unauthorized without valid token`() {
      webTestClient
        .get().uri("/users/username/EXT_USER/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }
    @Test
    fun `User Roles endpoint returns roles for user`() {
      webTestClient
        .get().uri("/users/username/EXT_USER/roles")
        .headers(setAuthorisation("EXT_USER", listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("ROLES_ADMIN")
          assertThat(it).contains("AUDIT_VIEWER")
        }
    }
    @Test
    fun `User Roles endpoint returns roles for user - PF_USER_ADMIN role`() {
      webTestClient
        .get().uri("/users/username/EXT_USER/roles")
        .headers(setAuthorisation("EXT_USER", listOf("ROLE_PF_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("ROLES_ADMIN")
          assertThat(it).contains("AUDIT_VIEWER")
        }
    }

    @Test
    fun `User Roles endpoint returns roles for user - INTEL_ADMIN role`() {
      webTestClient
        .get().uri("/users/username/EXT_USER/roles")
        .headers(setAuthorisation("EXT_USER", listOf("ROLE_INTEL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("ROLES_ADMIN")
          assertThat(it).contains("AUDIT_VIEWER")
        }
    }
    @Test
    fun `User Roles endpoint returns not found for unknown username`() {
      webTestClient
        .get().uri("/users/username/UNKNOWN/roles")
        .headers(setAuthorisation("EXT_USER", listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
  private fun addRoleForUserId(userId: String, roleCode: String) {
    webTestClient
      .post().uri("/users/$userId/roles")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .body(BodyInserters.fromValue(listOf(roleCode)))
      .exchange()
      .expectStatus().isNoContent
  }

  private fun removeRoleForUserId(userId: String, roleCode: String) {
    webTestClient
      .delete().uri("/users/$userId/roles/$roleCode")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNoContent
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
        assertThat(it).containsAll(roles)
      }
  }
}
