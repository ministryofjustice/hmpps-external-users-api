@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class GroupsControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class Groups {
    @Test
    fun `All Groups endpoint not accessible without valid token`() {
      webTestClient.get().uri("/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `All Groups endpoint returns forbidden when does not have  role `() {
      webTestClient
        .get().uri("/groups")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied"
            )
          )
        }
    }

    @Test
    fun `All Groups endpoint returns all possible groups`() {
      webTestClient
        .get().uri("/groups")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("[?(@.groupCode == 'SITE_1_GROUP_1')]")
        .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))
        .jsonPath("[*].groupCode").value<List<String>> {
          assertThat(it).hasSizeGreaterThan(2)
        }
    }
  }
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
              "userMessage" to "Group not within your groups: Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
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
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
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
    fun `Group details endpoint returns not found when group not found`() {
      webTestClient
        .get().uri("/groups/bob")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
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
    fun `Group details endpoint returns forbidden when group manager not in group`() {
      webTestClient
        .get().uri("/groups/bob")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Unable to maintain group: bob with reason: Group not with your groups",
              "userMessage" to "Group not within your groups: Unable to maintain group: bob with reason: Group not with your groups",
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
  inner class ChangeGroupName {
    @Test
    fun `Change group name`() {
      webTestClient
        .put().uri("/groups/SITE_9_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Invalid group name`() {
      webTestClient
        .put().uri("/groups/SITE_9_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {

          assertThat(it["userMessage"] as String).startsWith("Validation failure:")

          assertThat(it["developerMessage"] as String).contains("default message [size must be between 4 and 100]")
          assertThat(it["userMessage"] as String).contains("groupName: size must be between 4 and 100")
        }
    }

    @Test
    fun `Change group name endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .put().uri("/groups/SITE_9_GROUP_1")
        .headers(setAuthorisation("bob"))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(FORBIDDEN.value())
          assertThat(it["userMessage"] as String).contains("Denied")
          assertThat(it["developerMessage"] as String).contains("Denied")
        }
    }

    @Test
    fun `Change group name returns error when group not found`() {
      webTestClient
        .put().uri("/groups/Not_A_Group")
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
      webTestClient.put().uri("/groups/SITE_9_GROUP_1")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class CreateGroup {
    @Test
    fun `Create group`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG",
              "groupName" to " groupie"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create group error`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "",
              "groupName" to ""
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("groupCode: size must be between 2 and 30")
          assertThat(it["userMessage"] as String).contains("groupName: size must be between 4 and 100")
        }
    }

    @Test
    fun `Create group length too long`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "12345".repeat(6) + "x", "groupName" to "12345".repeat(20) + "y",
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("groupCode: size must be between 2 and 30")
          assertThat(it["userMessage"] as String).contains("groupName: size must be between 4 and 100")
        }
    }

    @Test
    fun `Create group length too short`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "x", "groupName" to "123",
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("groupCode: size must be between 2 and 30")
          assertThat(it["userMessage"] as String).contains("groupName: size must be between 4 and 100")
        }
    }

    @Test
    fun `Create group failed regex`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "a-b", "groupName" to "a\$here",
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("groupCode: must match \"^[0-9A-Za-z_]*\"")
          assertThat(it["userMessage"] as String).contains("groupName: must match \"^[0-9A-Za-z- ,.()'&]*\$")
        }
    }

    @Test
    fun `Create group endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("bob"))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG3",
              "groupName" to " groupie 3"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
      {"userMessage":"Denied","developerMessage":"Denied"}
          """.trimIndent()
        )
    }

    @Test
    fun `Create group - group already exists`() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG1",
              "groupName" to " groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isOk

      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG1",
              "groupName" to " groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.CONFLICT.value(),
              "errorCode" to null,
              "moreInfo" to null,
              "userMessage" to "Group already exists: Unable to create group: CG1 with reason: group code already exists",
              "developerMessage" to "Unable to create group: CG1 with reason: group code already exists"
            )
          )
        }
    }

    @Test
    fun `Create group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class DeleteGroupCode {
    @Test
    fun `Delete Group - no child groups and no members`() {
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Group - no child groups but has members`() {
      webTestClient.delete().uri("/groups/GC_DEL_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Group - has child groups`() {
      webTestClient.delete().uri("/groups/GC_DEL_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.CONFLICT.value(),
              "errorCode" to null,
              "moreInfo" to null,
              "userMessage" to "Unable to delete group: GC_DEL_3 with reason: child group exists",
              "developerMessage" to "Unable to delete group: GC_DEL_3 with reason: child group exists"
            )
          )
        }
    }

    @Test
    fun `Delete Child Group endpoint returns forbidden when does not have admin role`() {
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
      {"userMessage":"Denied","developerMessage":"Denied"}
          """.trimIndent()
        )
    }
    @Test
    fun `Delete Child Group details endpoint not accessible without valid token`() {
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}
