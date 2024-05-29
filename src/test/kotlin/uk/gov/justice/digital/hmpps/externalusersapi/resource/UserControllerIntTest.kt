package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class HasPassword {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/password/present")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Not accessible without correct role`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/password/present")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ADD_SENSITIVE_CASE_NOTES")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Not accessible to group manager when no groups in common with user`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/password/present")
        .headers(setAuthorisation("CA_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `User not found`() {
      webTestClient.get().uri("/users/id/C0999EE9-99BF-999F-999C-AA99C9DF99F9/password/present")
        .headers(setAuthorisation("CA_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `User with password`() {
      val hasPassword = webTestClient.get().uri("/users/id/608955ae-52ed-44cc-884c-011597a77949/password/present")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(Boolean::class.java)
        .returnResult().responseBody

      assertNotNull(hasPassword)
      assertTrue(hasPassword)
    }

    @Test
    fun `User without password`() {
      val hasPassword = webTestClient.get().uri("/users/id/c0279ee3-76bf-487f-833c-aa47c5df22f8/password/present")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(Boolean::class.java)
        .returnResult().responseBody

      assertNotNull(hasPassword)
      assertFalse(hasPassword)
    }
  }

  @Nested
  inner class UpdateEmailAndUsername {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.put().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/email")
        .body(BodyInserters.fromValue(mapOf("username" to "jo_bloggs", "email" to "jo@bloggs.com")))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Not accessible without correct role`() {
      webTestClient.put().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/email")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ADD_SENSITIVE_CASE_NOTES")))
        .body(BodyInserters.fromValue(mapOf("username" to "jo_bloggs", "email" to "jo@bloggs.com")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Not accessible to group manager when no groups in common with user`() {
      webTestClient.put().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/email")
        .headers(setAuthorisation("CA_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(mapOf("username" to "jo_bloggs", "email" to "jo@bloggs.com")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `User not found`() {
      webTestClient.put().uri("/users/id/C0999EE9-99BF-999F-999C-AA99C9DF99F9/email")
        .headers(setAuthorisation("CA_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(mapOf("username" to "jo_bloggs", "email" to "jo@bloggs.com")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `Bad request - missing email and username`() {
      webTestClient.put().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8/email")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Email and username updated`() {
      webTestClient.get().uri("/users/id/1F650F15-0993-4DB7-9A32-5B930FF86037")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.username").isEqualTo("AUTH_DEVELOPER")
        .jsonPath("$.email").isEqualTo("auth_developer@digital.justice.gov.uk")
        .jsonPath("$.verified").isEqualTo(true)

      webTestClient.put().uri("/users/id/1F650F15-0993-4DB7-9A32-5B930FF86037/email")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("username" to "jo_bloggs", "email" to "jo@bloggs.com")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/users/id/1F650F15-0993-4DB7-9A32-5B930FF86037")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.username").isEqualTo("JO_BLOGGS")
        .jsonPath("$.email").isEqualTo("jo@bloggs.com")
        .jsonPath("$.verified").isEqualTo(false)
    }
  }

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

    @Test
    fun `User search return should return result with special char`() {
      val user = NewUser("bob2.o'hagan@bobdigital.justice.gov.uk", "Bob", "O'HAGAN")

      webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/${user.email}")
        .headers(setAuthorisation("ITAG_USER_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it.filter { it.key != "userId" }).containsAllEntriesOf(
            mapOf("username" to user.email.uppercase(), "enabled" to true, "lastName" to user.lastName, "firstName" to user.firstName),
          )
        }

      webTestClient
        .get().uri("/users/search?name=bob2.o'hagan@bobdigital.justice.gov.uk&groups=&roles=")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.content[0].username").isEqualTo("BOB2.O'HAGAN@BOBDIGITAL.JUSTICE.GOV.UK")
        .jsonPath("$.content[0].email").isEqualTo("bob2.o'hagan@bobdigital.justice.gov.uk")
        .jsonPath("$.content[0].firstName").isEqualTo("Bob")
        .jsonPath("$.content[0].lastName").isEqualTo("O'HAGAN")
/*        .jsonPath("$.content[0]").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
             // "userId" to "006a9299-ef3d-4990-8604-13cefac706b5",
              "username" to "BOB2.O'HAGAN@BOBDIGITAL.JUSTICE.GOV.UK",
              "email" to "bob2.o'hagan@bobdigital.justice.gov.uk",
              "firstName" to "Bob",
              "lastName" to "O'HAGAN",
              "locked" to false,
              "enabled" to true,
              "verified" to false,
              "inactiveReason" to  null,
            ),
          )
        }*/
        .jsonPath("totalElements").isEqualTo("1")
       /* .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Role already exists: Role with code RC1 already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Role with code RC1 already exists")*/
      // .json("user_search_specialchar.json".readFile())
    }
  }

  @Nested
  inner class EnableUserByUserId {

    @Test
    fun `External User Enable endpoint enables user`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75255/enable")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/AUTH_STATUS")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "userId" to "fc494152-f9ad-48a0-a87c-9adc8bd75255",
              "username" to "AUTH_STATUS",
              "email" to null,
              "firstName" to "Auth",
              "lastName" to "Status",
              "locked" to false,
              "enabled" to true,
              "verified" to true,
            ),
          )
        }
    }

    @Test
    fun `Group manager Enable endpoint enables user`() {
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/enable")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/AUTH_STATUS2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "userId" to "fc494152-f9ad-48a0-a87c-9adc8bd75266",
              "username" to "AUTH_STATUS2",
              "email" to null,
              "firstName" to "Auth",
              "lastName" to "Status2",
              "locked" to false,
              "enabled" to true,
              "verified" to true,
            ),
          )
        }

      // reset user so that tests can be rerun
      webTestClient
        .delete().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/groups/site_1_group_2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      val reason = DeactivateReason("left department")
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/disable")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .bodyValue(reason)
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
              "errorCode" to null,
            ),
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
              "errorCode" to null,
            ),
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
              "moreInfo" to null,
            ),
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
              "moreInfo" to null,
            ),
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
  inner class DisableUserByUserId {
    @Test
    fun `Auth User Disable endpoint disables user`() {
      val reason = DeactivateReason("left department")
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75255/disable")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .bodyValue(reason)
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/AUTH_STATUS")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "userId" to "fc494152-f9ad-48a0-a87c-9adc8bd75255",
              "username" to "AUTH_STATUS",
              "email" to null,
              "firstName" to "Auth",
              "lastName" to "Status",
              "locked" to false,
              "enabled" to false,
              "verified" to true,
            ),
          )
        }

      // reset user to original state
      webTestClient
        .put().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75255/enable")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Group manager Disable by userId endpoint disables user`() {
      webTestClient
        .put().uri("/users/FC494152-F9AD-48A0-A87C-9ADC8BD75266/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient
        .put().uri("/users/FC494152-F9AD-48A0-A87C-9ADC8BD75266/disable")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .bodyValue(DeactivateReason("left department"))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/AUTH_STATUS2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "userId" to "fc494152-f9ad-48a0-a87c-9adc8bd75266",
              "username" to "AUTH_STATUS2",
              "email" to null,
              "firstName" to "Auth",
              "lastName" to "Status2",
              "locked" to false,
              "enabled" to false,
              "verified" to true,
            ),
          )
        }

      // remove group so that tests can be rerun
      webTestClient
        .delete().uri("/users/fc494152-f9ad-48a0-a87c-9adc8bd75266/groups/site_1_group_2")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `Disable External User denied access with no authorization header`() {
      webTestClient
        .put().uri("/users/FC494152-F9AD-48A0-A87C-9ADC8BD75255/disable")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class GetUserById {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Not accessible without correct role`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8")
        .headers(setAuthorisation("AUTH_ADM", listOf("ADD_SENSITIVE_CASE_NOTES")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Not accessible to group manager when no groups in common with user`() {
      webTestClient.get().uri("/users/id/C0279EE3-76BF-487F-833C-AA47C5DF22F8")
        .headers(setAuthorisation("CA_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `User not found`() {
      webTestClient.get().uri("/users/id/999955ae-52ed-44cc-884c-011597a77999")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `Returns user details with correct role`() {
      webTestClient.get().uri("/users/id/608955ae-52ed-44cc-884c-011597a77949")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userId").isEqualTo("608955ae-52ed-44cc-884c-011597a77949")
        .jsonPath("$.username").isEqualTo("AUTH_USER")
        .jsonPath("$.email").isEqualTo("auth_user@digital.justice.gov.uk")
        .jsonPath("$.firstName").isEqualTo("Auth")
        .jsonPath("$.lastName").isEqualTo("Only")
        .jsonPath("$.verified").isEqualTo(true)
        .jsonPath("$.enabled").isEqualTo(true)
        .jsonPath("$.locked").isEqualTo(false)
    }
  }

  @Nested
  inner class GetUsersByUserName {
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
        .jsonPath("$.[*].groupCode").value<List<String>> { assertThat(it.size > 2) }
    }
  }

  data class NewUser(val email: String, val firstName: String, val lastName: String, val groupCodes: Set<String>? = null)

  @Nested
  inner class CreateExternalUser {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.post().uri("/users/user/create")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Create User by email endpoint succeeds to create user data`() {
      val user = NewUser("bob2@bobdigital.justice.gov.uk", "Bob", "Smith")

      webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/users/${user.email}")
        .headers(setAuthorisation("ITAG_USER_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it.filter { it.key != "userId" }).containsAllEntriesOf(
            mapOf("username" to user.email.uppercase(), "enabled" to true, "lastName" to user.lastName, "firstName" to user.firstName),
          )
        }
    }

    @Test
    fun `Create User by email endpoint fails if user with email already exists and unique is enabled`() {
      val user = NewUser("auth_test@digital.justice.gov.uk", "Bob", "Smith")

      webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Create User by email endpoint succeeds to create user data with group and roles`() {
      val user = NewUser("bob1@bobdigital.justice.gov.uk", "Bob", "Smith", setOf("SITE_1_GROUP_2"))

      val result = webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER", "ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .returnResult(String::class.java)

      val userId = result.responseBody.blockFirst()?.filterNot { it == '"' }

      webTestClient
        .get().uri("/users/$userId/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].groupCode").value<List<String>> {
          assertThat(it).containsOnly("CHILD_1")
        }
        .jsonPath("$.[*].groupName").value<List<String>> {
          assertThat(it).containsOnly("Child - Site 1 - Group 2")
        }

      webTestClient
        .get().uri("/users/$userId/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).isEqualTo(listOf("GLOBAL_SEARCH", "LICENCE_RO"))
        }
    }

    @Test
    fun `Create User by email endpoint fails if no privilege`() {
      val user = NewUser("bob@bobdigital.justice.gov.uk", "Bob", "Smith")

      webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("ITAG_USER_ADM"))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf("developerMessage" to "Denied", "status" to 403, "userMessage" to "Denied", "errorCode" to null, "moreInfo" to null),
          )
        }
    }

    @Test
    fun `Fails when first and last name are invalid`() {
      val user = NewUser("auth_test@digital.justice.gov.uk", ">", "S")

      webTestClient
        .post().uri("/users/user/create").bodyValue(user)
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(

              "status" to 400,
            ),
          )
          assertThat(it["userMessage"] as String).contains("firstName: First name length should be between minimum 2 to maximum 50 characters")
          assertThat(it["userMessage"] as String).contains("lastName: Last name length should be between minimum 2 to maximum 50 characters")
          assertThat(it["userMessage"] as String).contains("firstName: firstName failed validation")
        }
    }
  }
}
