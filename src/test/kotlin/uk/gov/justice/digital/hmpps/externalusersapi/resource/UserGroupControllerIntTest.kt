package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class UserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class RemoveGroupByUserId {

    @Test
    fun `remove group from user success`() {
      webTestClient
        .delete().uri("/users/id/7CA04ED7-8275-45B2-AFB4-4FF51432D1EC/groups/site_1_group_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `remove group as group manager`() {
      webTestClient
        .delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA02F/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `does not remove group if group Manager not member of group`() {
      webTestClient
        .delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA02C/groups/GC_DEL_4")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `does not remove group if group Manager and users last group`() {
      webTestClient
        .delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without admin role`() {
      webTestClient.delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("bob", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden without valid token`() {
      webTestClient.delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden with incorrect role`() {
      webTestClient.delete().uri("/users/id/90F930E1-2195-4AFD-92CE-0EB5672DA44B/groups/SITE_1_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isForbidden
    }
  }
}
