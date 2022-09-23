package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class RolesControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GetRoles {
    @Test
    fun `Get Roles endpoint not accessible without valid token`() {
      webTestClient.get().uri("/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Get Roles endpoint returns forbidden when does not have correct role `() {
      webTestClient
        .get().uri("/roles")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Get Roles endpoint returns all roles if no adminType set`() {
      webTestClient
        .get().uri("/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleName").value<List<String>> {
          assertThat(it).hasSizeGreaterThanOrEqualTo(68)
        }
    }

    @Test
    fun `Get Roles endpoint returns roles filter requested adminType`() {
      webTestClient
        .get().uri("/roles?adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleName").value<List<String>> { assertThat(it).hasSize(23) }
        .jsonPath("$[0].roleName").isEqualTo("Add Secure Case Notes")
        .jsonPath("$[0].roleCode").isEqualTo("ADD_SENSITIVE_CASE_NOTES")
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
        .jsonPath("$[0].adminType[2].adminTypeCode").doesNotExist()
    }

    @Test
    fun `Get Roles endpoint returns roles filter requested multiple adminTypes`() {
      webTestClient
        .get().uri("/roles?adminTypes=EXT_ADM&adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleName").value<List<String>> {
          assertThat(it).hasSize(5)
        }
        .jsonPath("$[0].roleName").isEqualTo("Artemis user")
        .jsonPath("$[0].roleCode").isEqualTo("ARTEMIS_USER")
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
        .jsonPath("$[0].adminType[2].adminTypeCode").isEqualTo("EXT_ADM")
    }
  }
}
