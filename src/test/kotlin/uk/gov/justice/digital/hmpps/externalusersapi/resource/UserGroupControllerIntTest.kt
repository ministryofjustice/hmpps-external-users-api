package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class Groups {
    @Test
    fun `User Groups by userId endpoint returns user groups no children - admin user`() {
      webTestClient
        .get().uri("/users/id/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups?children=false")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"SITE_1_GROUP_2","groupName":"Site 1 - Group 2"}       
        ]
          """.trimIndent()
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups no children - group manager`() {
      webTestClient
        .get().uri("/users/id/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups?children=false")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"SITE_1_GROUP_2","groupName":"Site 1 - Group 2"}       
        ]
          """.trimIndent()
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups with children by default - admin user`() {
      webTestClient
        .get().uri("/users/id/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
          """.trimIndent()
        )
    }

    @Test
    fun `User Groups by userId endpoint returns user groups with children by default - group manager`() {
      webTestClient
        .get().uri("/users/id/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
          """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
          """.trimIndent()
        )
    }

    @Test
    fun `User Groups by userId endpoint returns forbidden - user not in group manager group`() {
      webTestClient
        .get().uri("/users/id/9E84F1E4-59C8-4B10-927A-9CF9E9A30792/groups")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups",
              "userMessage" to "User group relationship exception: Unable to maintain user: AUTH_MFA_EXPIRED with reason: User not with your groups"
            )
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
          """.trimIndent()
        )
    }

    @Test
    fun `User Groups by userId endpoint not accessible without valid token`() {
      webTestClient
        .get().uri("/users/id/5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  private fun callGetGroups(
    userId: String = "7CA04ED7-8275-45B2-AFB4-4FF51432D1EA",
    children: Boolean = false
  ): BodyContentSpec = webTestClient
    .get().uri("/users/id/$userId/groups?children=$children")
    .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
    .exchange()
    .expectStatus().isOk
    .expectBody()
}
