@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class GroupsControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GroupDetails {
    @Test
    fun `Group details endpoint returns details of group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("group_details_data.json".readFile())
    }

    @Test
    fun `Group details endpoint returns details of group when user is able to maintain group`() {
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("group_details_data.json".readFile())
    }

    @Test
    fun `Group details endpoint returns error when user is not allowed to maintain group`() {
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
              "userMessage" to "Auth maintain group relationship exception: Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `Group details endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Access is denied",
              "userMessage" to "Access is denied",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `Group details endpoint returns error when group not found user has ROLE_MAINTAIN_OAUTH_USERS`() {
      webTestClient
        .get().uri("/groups/bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Unable to get group: bob with reason: notfound",
              "userMessage" to "Group Not found: Unable to get group: bob with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `Group details endpoint returns error when group not found`() {
      webTestClient
        .get().uri("/groups/bob")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Unable to get group: bob with reason: notfound",
              "userMessage" to "Group Not found: Unable to get group: bob with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.get().uri("/groups/GLOBAL_SEARCH")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChangeChildGroupName {
    @Test
    fun `Change group name`() {
      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change group name endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("bob"))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(FORBIDDEN.value())
          assertThat(it["userMessage"] as String).startsWith("Access is denied")
          assertThat(it["developerMessage"] as String).startsWith("Access is denied")
        }
    }

    @Test
    fun `Change group name returns error when group not found`() {
      webTestClient
        .put().uri("/groups/child/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"] as String).startsWith("Group Not found: Unable to maintain group: Not_A_Group with reason: notfound")
          assertThat(it["developerMessage"] as String).startsWith("Unable to maintain group: Not_A_Group with reason: notfound")
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/groups/child/CHILD_9")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}
