package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class ChildGroupsControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class ChangeChildGroupName {
    @Test
    fun `Change group name`() {
      webTestClient
        .put().uri("/groups/child/CHILD_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new child group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Invalid group name`() {
      webTestClient
        .put().uri("/groups/child/SITE_9_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["userMessage"] as String).contains("Validation failure: groupName: size must be between 4 and 100")
          Assertions.assertThat(it["developerMessage"] as String).contains(" default message [groupName],100,4]; default message [size must be between 4 and 100]]")
        }
    }

    @Test
    fun `Change group name endpoint returns forbidden when does not have admin role`() {
      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("bob"))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(HttpStatus.FORBIDDEN.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("Denied")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Denied")
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
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(HttpStatus.NOT_FOUND.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("Child group not found: Unable to get child group: Not_A_Group with reason: notfound")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Unable to get child group: Not_A_Group with reason: notfound")
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/groups/child/CHILD_9")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class DeleteChildGroup {

    @Test
    fun `access forbidden without admin role`() {
      webTestClient.delete().uri("/groups/child/GC_DEL_1")
        .headers(setAuthorisation("bob", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.delete().uri("/groups/child/GC_DEL_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden with incorrect role`() {
      webTestClient.delete().uri("/groups/child/GC_DEL_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `group not found`() {
      webTestClient.delete().uri("/groups/child/UNKNOWN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "errorCode" to null,
              "moreInfo" to null,
              "userMessage" to "Child group not found: Unable to get child group: UNKNOWN with reason: notfound",
              "developerMessage" to "Unable to get child group: UNKNOWN with reason: notfound",
            ),
          )
        }
    }

    @Test
    fun `delete success`() {
      webTestClient.delete().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class CreateChildGroup {
    @Test
    fun `Create child group`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG",
              "groupName" to "Child groupie",
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create child group error`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "",
              "groupCode" to "",
              "groupName" to "",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Create child group endpoint returns forbidden when does not have admin role`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("bob"))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG3",
              "groupName" to "Child groupie 3",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
     {"userMessage":"Denied","developerMessage":"Denied"}
          """.trimIndent(),
        )
    }

    @Test
    fun `Create Child group length too short`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "",
              "groupCode" to "",
              "groupName" to "",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["userMessage"] as String).contains("groupCode: size must be between 2 and 30")
          Assertions.assertThat(it["userMessage"] as String).contains("groupName: size must be between 4 and 100")
        }
    }

    @Test
    fun `Create child group - group already exists`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1",
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "developerMessage" to "Unable to create child group: CG1 with reason: Child group code already exists",
              "userMessage" to "Unable to create child group: CG1 with reason: Child group code already exists",
              "errorCode" to null,
              "moreInfo" to null,
              "status" to HttpStatus.CONFLICT.value(),
            ),
          )
        }
    }

    @Test
    fun `Create child group - parent group doesnt exist`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "pg",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1",
            ),
          ),
        )
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "developerMessage" to "Unable to create group: CG1 with reason: ParentGroupNotFound",
              "userMessage" to "Group Not found: Unable to create group: CG1 with reason: ParentGroupNotFound",
              "errorCode" to null,
              "moreInfo" to null,
              "status" to HttpStatus.NOT_FOUND.value(),
            ),
          )
        }
    }

    @Test
    fun `Create Child Group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/groups/child")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChildGroupDetails {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/groups/child/CHILD_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/groups/child/CHILD_1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `child group not found`() {
      webTestClient
        .get().uri("/groups/child/bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to get child group: bob with reason: notfound",
              "userMessage" to "Child group not found: Unable to get child group: bob with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null,
            ),
          )
        }
    }

    @Test
    fun `Retrieve child group details`() {
      webTestClient
        .get().uri("/groups/child/CHILD_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "groupCode" to "CHILD_2",
              "groupName" to "Child - Site 2 - Group 1",
            ),
          )
        }
    }
  }
}
