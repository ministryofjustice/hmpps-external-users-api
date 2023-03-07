package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class AddGroupByUserId {

    @Test
    fun `access forbidden without admin role`() {
      webTestClient.put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("bob", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden with incorrect role`() {
      webTestClient.put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `add group to a user success`() {
      callGetGroups(userId = "7CA04ED7-8275-45B2-AFB4-4FF51432D1EC")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_2')]")
        .doesNotExist()

      webTestClient
        .put().uri("/users/7CA04ED7-8275-45B2-AFB4-4FF51432D1EC/groups/site_1_group_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      callGetGroups(userId = "7CA04ED7-8275-45B2-AFB4-4FF51432D1EC")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_2')]")
        .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_2", "groupName" to "Site 1 - Group 2"))
    }

    @Test
    fun `add group to a user as group manager`() {
      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA030")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_2')]")
        .doesNotExist()

      webTestClient
        .put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA030/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent

      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA030")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_2')]")
        .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_2", "groupName" to "Site 1 - Group 2"))
    }

    @Test
    fun `does not add group if group Manager not member of group`() {
      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA02C")
        .jsonPath("[?(@.groupCode == 'PECS_DRB8')]")
        .doesNotExist()

      webTestClient
        .put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02C/groups/PECS_DRB8")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .json(
          """
              {
              "userMessage":"Add group failed for field group with reason: managerNotMember",
              "developerMessage":"Add group failed for field group with reason: managerNotMember"
              }
          """
            .trimIndent(),
        )

      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA02C")
        .jsonPath("[?(@.groupCode == 'PECS_DRB8')]")
        .doesNotExist()
    }

    @Test
    fun `does not add group if user not in group managers groups`() {
      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA44A")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_1')]")
        .doesNotExist()

      webTestClient
        .put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44A/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
              {
              "userMessage":"User not within your groups: Unable to maintain user: AUTH_RO_USER_TEST4 with reason: User not with your groups",
              "developerMessage":"Unable to maintain user: AUTH_RO_USER_TEST4 with reason: User not with your groups"
              }
        """
            .trimIndent(),
        )

      callGetGroups(userId = "90F930E1-2195-4AFD-92CE-0EB5672DA44A")
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_1')]")
        .doesNotExist()
    }
  }

  @Nested
  inner class RemoveGroupByUserId {

    @Test
    fun `remove group from user success`() {
      webTestClient
        .delete().uri("/users/7CA04ED7-8275-45B2-AFB4-4FF51432D1EC/groups/site_1_group_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient
        .put().uri("/users/7CA04ED7-8275-45B2-AFB4-4FF51432D1EC/groups/site_1_group_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `remove group as group manager`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02F/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient
        .put().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02F/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `does not remove group if group Manager not member of group`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA02C/groups/GC_DEL_4")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `does not remove group if group Manager and users last group`() {
      webTestClient
        .delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without admin role`() {
      webTestClient.delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("bob", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden with incorrect role`() {
      webTestClient.delete().uri("/users/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class GetUserGroups {

    @Test
    fun `User Groups by userId endpoint returns not found if no user`() {
      webTestClient
        .get().uri("/users/12345678-1234-1234-1234-123456789101/groups?children=false")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "User 12345678-1234-1234-1234-123456789101 not found",
              "userMessage" to "User not found: User 12345678-1234-1234-1234-123456789101 not found",
            ),
          )
        }
    }

    @Test
    fun `User Groups by userId endpoint returns user groups no children - admin user`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups?children=false")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"SITE_1_GROUP_2","groupName":"Site 1 - Group 2"}
        ]
          """.trimIndent(),
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups no children - group manager`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups?children=false")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"SITE_1_GROUP_2","groupName":"Site 1 - Group 2"}
        ]
          """.trimIndent(),
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups with children by default - admin user`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
          """.trimIndent(),
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups with children by default - group manager`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
          """.trimIndent(),
        )
    }

    @Test
    fun `User Groups by userId endpoint returns forbidden - user not in group manager group`() {
      webTestClient
        .get().uri("/users/9E84F1E4-59C8-4B10-927A-9CF9E9A30792/groups")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups",
              "userMessage" to "User not within your groups: Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups",
            ),
          )
        }
    }

    @Test
    fun `User Groups by userId endpoint returns user groups with children`() {
      callGetGroups("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8", children = true)
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
          """.trimIndent(),
        )
    }

    @Test
    fun `User Groups by userId endpoint not accessible without valid token`() {
      webTestClient
        .get().uri("/users/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  private fun callGetGroups(
    userId: String = "7CA04ED7-8275-45B2-AFB4-4FF51432D1EA",
    children: Boolean = false,
  ): BodyContentSpec = webTestClient
    .get().uri("/users/$userId/groups?children=$children")
    .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
    .exchange()
    .expectStatus().isOk
    .expectBody()
}
