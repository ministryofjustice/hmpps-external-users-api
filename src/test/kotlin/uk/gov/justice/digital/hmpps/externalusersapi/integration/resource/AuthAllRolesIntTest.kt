package uk.gov.justice.digital.hmpps.externalusersapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class AuthAllRolesIntTest : IntegrationTestBase() {
  @Test
  fun `All Roles endpoint returns all possible auth roles`() {
    hmppsAuthMockServer.stubGetAllRolesFilterAdminType()
    webTestClient
      .get().uri("/authroles")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("[?(@.roleCode == 'GLOBAL_SEARCH')]")
      .isEqualTo(mapOf("roleCode" to "GLOBAL_SEARCH", "roleName" to "Global Search", "roleDescription" to "Allow user to search globally for a user"))
      .jsonPath("[*].roleCode").value<List<String>> {
        assertThat(it).hasSizeGreaterThan(2)
      }
  }

  @Test
  fun `Auth Roles endpoint not accessible without valid token`() {
    webTestClient.get().uri("/authroles")
      .exchange()
      .expectStatus().isUnauthorized
  }
}
