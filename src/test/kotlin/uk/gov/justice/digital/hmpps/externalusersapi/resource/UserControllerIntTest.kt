package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
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
  inner class EnableUserByUserId {

    @Test
    fun `External User Enable endpoint enables user`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75255/enable")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Group manager Enable endpoint fails user not in group manager group forbidden`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/enable")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "userMessage" to "User not within your groups: Unable to maintain user: AUTH_STATUS2 with reason: User not with your groups",
              "developerMessage" to "Unable to maintain user: AUTH_STATUS2 with reason: User not with your groups",
              "moreInfo" to null,
              "errorCode" to null
            )
          )
        }
    }

    @Test
    fun `Fails to enable user with invalid user id`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75333/enable")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "userMessage" to "User not found: User fc494152-f9ad-48a0-a87c-9adc8bd75333 not found",
              "developerMessage" to "User fc494152-f9ad-48a0-a87c-9adc8bd75333 not found",
              "moreInfo" to null,
              "errorCode" to null
            )
          )
        }
    }

    @Test
    fun `External User Enable endpoint fails is not an admin user`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/enable")
        .headers(setAuthorisation("ITAG_USER", listOf()))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `External User Enable by userId endpoint fails is not an admin user`() {
      webTestClient
        .put().uri("/users/FC494152-F9AD-48A0-A87C-9ADC8BD75255/enable")
        .headers(setAuthorisation("ITAG_USER", listOf()))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `External User denied access with no authorization header`() {
      webTestClient
        .put().uri("/users/FC494152-F9AD-48A0-A87C-9ADC8BD75255/enable")
        .exchange()
        .expectStatus().isUnauthorized
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
        .expectStatus().isNoContent
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

  @Nested
  inner class UsersByUserName {
    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/users/user_name")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Is accessible to authorised user without roles`() {
      webTestClient.get().uri("/users/user_name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `Responds with no content when username not present`() {
      webTestClient.get().uri("/users")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `Responds with content when username matches`() {
      webTestClient.get().uri("/users/AUTH_ADM")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userId").isEqualTo("5105a589-75b3-4ca0-9433-b96228c1c8f3")
        .jsonPath("$.username").isEqualTo("AUTH_ADM")
        .jsonPath("$.email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$.firstName").isEqualTo("Auth")
        .jsonPath("$.lastName").isEqualTo("Adm")
        .jsonPath("$.locked").isEqualTo(false)
        .jsonPath("$.enabled").isEqualTo(true)
        .jsonPath("$.verified").isEqualTo(true)
        .jsonPath("$.lastLoggedIn").isNotEmpty
        .jsonPath("$.inactiveReason").isEmpty
    }
  }

  @Nested
  inner class MyAssignableGroups {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/users/me/assignable-groups")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Is accessible to authorised user without roles`() {
      webTestClient.get().uri("/users/me/assignable-groups")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Should respond with authorised user groups`() {
      webTestClient.get().uri("/users/me/assignable-groups")
        .headers(setAuthorisation(user = "AUTH_GROUP_MANAGER"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].groupCode").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$.[0].groupCode").isEqualTo("SITE_1_GROUP_1")
        .jsonPath("$.[0].groupName").isEqualTo("Site 1 - Group 1")
    }

    @Test
    fun `Should respond with all groups when authorised user holds maintain role`() {
      webTestClient.get().uri("/users/me/assignable-groups")
        .headers(setAuthorisation(user = "AUTH_ADM", roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].groupCode").value<List<String>> { assertThat(it).hasSize(78) }
    }
  }
}
