package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository

class RoleControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var roleRepository: RoleRepository

  @Nested
  inner class CreateRole {

    @Test
    fun `Create role`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC",
              "roleName" to " New role",
              "roleDescription" to "New role description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      runBlocking {
        val role = roleRepository.findByRoleCode("RC")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create IMS role`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "IMS_USER_ROLE",
              "roleName" to " New IMS role",
              "roleDescription" to "New IMS role description",
              "adminType" to listOf("IMS_HIDDEN"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      runBlocking {
        val role = roleRepository.findByRoleCode("IMS_USER_ROLE")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create role passes regex validation`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC",
              "roleName" to "good's & Role(),.-",
              "roleDescription" to "good's & Role(),.-lineone\r\nlinetwo",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      runBlocking {
        val role = roleRepository.findByRoleCode("RC")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create role with empty role description`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC",
              "roleName" to " New role",
              "roleDescription" to "",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      runBlocking {
        val role = roleRepository.findByRoleCode("RC")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create role with no role description`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC",
              "roleName" to " New role",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      runBlocking {
        val role = roleRepository.findByRoleCode("RC")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create role error`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "",
              "roleName" to "",
              "adminType" to listOf<String>(),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("roleCode: role code must be supplied")
          assertThat(it["userMessage"] as String).contains("roleCode: size must be between 2 and 30")
          assertThat(it["userMessage"] as String).contains("roleName: role name must be supplied")
          assertThat(it["userMessage"] as String).contains("roleName: size must be between 4 and 128")
          assertThat(it["userMessage"] as String).contains("adminType: Admin type cannot be empty")
        }
    }

    @Test
    fun `Create role length too long`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "x".repeat(30) + "x",
              "roleName" to "x".repeat(128) + "y",
              "roleDescription" to "x".repeat(1024) + "y",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["userMessage"] as String).contains("roleCode: size must be between 2 and 30")
          assertThat(it["userMessage"] as String).contains("roleName: size must be between 4 and 128")
          assertThat(it["userMessage"] as String).contains("roleDescription: size must be between 0 and 1024")
        }
    }

    @Test
    fun `Create role admin type empty`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLE_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "xxxx",
              "roleName" to "123456",
              "adminType" to listOf<String>(),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String)
            .startsWith("Validation failure:")
          assertThat(it["userMessage"] as String)
            .contains("Admin type cannot be empty")
        }
    }

    @Test
    fun `Create role failed regex`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "a-b",
              "roleName" to "a\$here",
              "roleDescription" to "a\$description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String)
            .contains("roleCode: must match \"^[0-9A-Za-z_]*")
          assertThat(it["userMessage"] as String)
            .contains("roleName: must match \"^[0-9A-Za-z- ,.()'&]*\$")
          assertThat(it["userMessage"] as String)
            .contains("roleDescription: must match \"^[0-9A-Za-z- ,.()'&\r\n]*\$")
        }
    }

    @Test
    fun `Create role endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("bob"))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "ROLE3",
              "roleName" to " role 3",
              "adminType" to listOf("EXT_ADM"),
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
    fun `Create role - role already exists`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "ROLE3",
              "roleName" to " role 3",
              "roleDescription" to "New role description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "ROLE3",
              "roleName" to " role 3",
              "roleDescription" to "New role description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String)
            .isEqualTo("Unable to add role: Unable to create role: ROLE3 with reason: role code already exists")
        }

      runBlocking {
        val role = roleRepository.findByRoleCode("ROLE3")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }

    @Test
    fun `Create role endpoint not accessible without valid token`() {
      webTestClient.post().uri("/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

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
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
            ),
          )
        }
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
    fun `Get Roles endpoint returns all excluding hidden roles if no adminType set`() {
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
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).doesNotContain("HIDDEN_ROLE")
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
        .jsonPath("$.[*].roleCode").value<List<String>> { assertThat(it).doesNotContain("HIDDEN_ROLE") }
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
          assertThat(it).hasSize(4)
        }
        .jsonPath("$[0].roleName").isEqualTo("Licence Responsible Officer")
        .jsonPath("$[0].roleCode").isEqualTo("LICENCE_RO")
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
        .jsonPath("$[0].adminType[2].adminTypeCode").isEqualTo("EXT_ADM")
    }

    @Test
    fun `Get Roles endpoint returns roles filter requested IMS_HIDDEN adminType`() {
      webTestClient
        .get().uri("/roles?adminTypes=IMS_HIDDEN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleName").value<List<String>> { assertThat(it).hasSize(1) }
        .jsonPath("$[0].roleName").isEqualTo("IMS user")
        .jsonPath("$[0].roleCode").isEqualTo("IMS_USER")
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
        .jsonPath("$[0].adminType[2].adminTypeCode").isEqualTo("IMS_HIDDEN")
    }
  }

  @Nested
  inner class GetRolesDefaultPaging {

    @Test
    fun `Get Paged Roles endpoint not accessible without valid token`() {
      webTestClient.get().uri("/roles/paged")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Get Paged Roles endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .get().uri("/roles/paged")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Get Paged Roles endpoint returns (default size=10) roles when user has role ROLE_ROLES_ADMIN`() {
      webTestClient
        .get().uri("/roles/paged")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("manage_roles.json".readFile())
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filtered by requested roleCode`() {
      webTestClient
        .get().uri("/roles/paged?roleCode=GLOBAL")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("manage_roles2.json".readFile())
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filter requested roleName when user has role ROLE_ROLES_ADMIN`() {
      webTestClient
        .get().uri("/roles/paged?roleName=admin")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("manage_roles3.json".readFile())
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filter requested adminType when user has role ROLE_ROLES_ADMIN`() {
      webTestClient
        .get().uri("/roles/paged?adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("manage_roles1.json".readFile())
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filter requested multiple adminTypes when user has role ROLE_ROLES_ADMIN`() {
      webTestClient
        .get().uri("/roles/paged?adminTypes=DPS_ADM&adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("Add Secure Case Notes")
        .jsonPath("$.content[0].roleCode").isEqualTo("ADD_SENSITIVE_CASE_NOTES")
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$.content[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filter requested when user has role ROLE_ROLES_ADMIN`() {
      webTestClient
        .get().uri("/roles/paged?roleName=add&roleCode=ADD_SENSITIVE_CASE_NOTES&adminTypes=DPS_ADM&adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("Add Secure Case Notes")
        .jsonPath("$.content[0].roleCode").isEqualTo("ADD_SENSITIVE_CASE_NOTES")
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("DPS_ADM")
        .jsonPath("$.content[0].adminType[1].adminTypeCode").isEqualTo("DPS_LSA")
        .jsonPath("$.content[0].adminType[2].adminTypeCode").doesNotExist()
        .jsonPath("$.totalElements").isEqualTo(1)
    }

    @Test
    fun `Get Paged Roles endpoint returns roles filter requested excluding hidden roles`() {
      webTestClient
        .get().uri("/roles/paged?roleName=add&roleCode=HIDDEN_ROLES&adminTypes=DPS_ADM&adminTypes=DPS_LSA")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> { assertThat(it).doesNotContain("HIDDEN_ROLE") }
    }
  }

  @Nested
  inner class GetRolesPaged {
    @Test
    fun `find page of roles with default sort`() {
      webTestClient.get().uri("/roles/paged?page=0&size=3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .assertPageOfMany()
        .jsonPath("$.content[2].roleName").isEqualTo("Approve Category assessments")
        .jsonPath("$.content[2].roleCode").isEqualTo("APPROVE_CATEGORISATION")
    }

    @Test
    fun `find page of roles sorting by role code`() {
      webTestClient.get().uri("/roles/paged?page=5&size=3&sort=roleCode")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .assertPageOfMany()
        .jsonPath("$.content[1].roleName").isEqualTo("HWPV Band 2")
        .jsonPath("$.content[1].roleCode").isEqualTo("HWPV_CLAIM_ENTRY_BAND_2")
    }

    @Test
    fun `find page of roles sorting by role code descending`() {
      webTestClient.get().uri("/roles/paged?page=7&size=3&sort=roleCode,desc")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .assertPageOfMany()
        .jsonPath("$.content[1].roleName").isEqualTo("PECS Person Escort Record Author")
        .jsonPath("$.content[1].roleCode").isEqualTo("PECS_PER_AUTHOR")
        .jsonPath("$.content[1].roleDescription").isEmpty
        .jsonPath("$.content[1].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
        .jsonPath("$.content[1].adminType[0].adminTypeName").isEqualTo("External Administrator")
    }

    private fun WebTestClient.BodyContentSpec.assertPageOfMany() = this.jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(72)
      .jsonPath("$.totalPages").isEqualTo(24)
      .jsonPath("$.last").isEqualTo(false)
  }

  @Nested
  inner class HideRole {

    @Test
    fun `Role details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE/hide")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Role details endpoint returns forbidden when does not have admin role`() {
      webTestClient
        .put().uri("/roles/ANY_ROLE/hide")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Role details endpoint returns error when role does not exist`() {
      webTestClient
        .put().uri("/roles/ROLE_DOES_NOT_EXIST/hide")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to find role: ROLE_DOES_NOT_EXIST with reason: not found",
              "userMessage" to "Unable to find role: Unable to find role: ROLE_DOES_NOT_EXIST with reason: not found",
              "moreInfo" to null,
            ),
          )
        }
    }

    @Test
    fun `Hide role endpoint returns success`() {
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC_HIDE",
              "roleName" to "New role to hide",
              "roleDescription" to "New role description to hide",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient
        .get().uri("/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).contains("RC_HIDE")
        }

      webTestClient
        .put().uri("/roles/RC_HIDE/hide")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      webTestClient
        .get().uri("/roles")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[*].roleCode").value<List<String>> {
          assertThat(it).doesNotContain("RC_HIDE")
        }

      runBlocking {
        val role = roleRepository.findByRoleCode("RC_HIDE")
        if (role != null) {
          roleRepository.delete(role)
        }
      }
    }
  }

  @Nested
  inner class RoleDetails {

    @Test
    fun `Role details endpoint not accessible without valid token`() {
      webTestClient.get().uri("/roles/ANY_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Role details endpoint returns forbidden when does not have admin role`() {
      webTestClient
        .get().uri("/roles/ANY_ROLE")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Role details endpoint returns error when role does not exist`() {
      webTestClient
        .get().uri("/roles/ROLE_DOES_NOT_EXIST")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to get role: ROLE_DOES_NOT_EXIST with reason: notfound",
              "userMessage" to "Unable to find role: Unable to get role: ROLE_DOES_NOT_EXIST with reason: notfound",
            ),
          )
        }
    }

    @Test
    fun `Role details endpoint returns success`() {
      webTestClient
        .get().uri("/roles/GLOBAL_SEARCH")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("role_details.json".readFile())
    }
  }

  @Nested
  inner class AmendRoleName {

    @Test
    fun `Change role name endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role name endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE")
        .headers(setAuthorisation("bob"))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
            ),
          )
        }
    }

    @Test
    fun `Change role name returns error when role not found`() {
      webTestClient
        .put().uri("/roles/Not_A_Role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to maintain role: Not_A_Role with reason: notfound",
              "userMessage" to "Unable to find role: Unable to maintain role: Not_A_Role with reason: notfound",
            ),
          )
        }
    }

    @Test
    fun `Change role name returns error when length too short`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "tim")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [roleName],100,4]; default message [size must be between 4 and 100]]")
        }
    }

    @Test
    fun `Change role name returns error when length too long`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleName" to "12345".repeat(20) + "y",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [roleName],100,4]; default message [size must be between 4 and 100]]")
        }
    }

    @Test
    fun `Change role name failed regex`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleName" to "a\$here",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [roleName],[Ljakarta.validation.constraints.Pattern")
        }
    }

    @Test
    fun `Change role name success`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk

      // reset role
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "Auth Client Management (admin)")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role name passes regex validation`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleName" to "good's & Role(),.-",
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // reset role
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "Auth Client Management (admin)")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class AmendRoleDescription {

    @Test
    fun `Change role description endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE/description")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role description endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE/description")
        .headers(setAuthorisation("bob"))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
            ),
          )
        }
    }

    @Test
    fun `Change role description returns error when role not found`() {
      webTestClient
        .put().uri("/roles/Not_A_Role/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to maintain role: Not_A_Role with reason: notfound",
              "userMessage" to "Unable to find role: Unable to maintain role: Not_A_Role with reason: notfound",
            ),
          )
        }
    }

    @Test
    fun `Change role description returns error when length too long`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleDescription" to "12345".repeat(205) + "y",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [roleDescription],1024,0]; default message [size must be between 0 and 1024]]")
        }
    }

    @Test
    fun `Change role description failed regex`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleDescription" to "a\$here",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [roleDescription],[Ljakarta.validation.constraints.Pattern$")
        }
    }

    @Test
    fun `Change role description success`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isOk

      // reset role
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to null)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for empty roleDescription`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for no role description`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to null)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description passes regex validation`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleDescription" to "good's & Role(),.-lineone\r\nlinetwo",
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class AmendRoleAdminType {

    @Test
    fun `Change role adminType endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE/admintype")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role adminType endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE/admintype")
        .headers(setAuthorisation("bob"))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.FORBIDDEN.value(),
              "developerMessage" to "Denied",
              "userMessage" to "Denied",
            ),
          )
        }
    }

    @Test
    fun `Change role admin type returns error when role not found`() {
      webTestClient
        .put().uri("/roles/Not_A_Role/admintype")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to HttpStatus.NOT_FOUND.value(),
              "developerMessage" to "Unable to maintain role: Not_A_Role with reason: notfound",
              "userMessage" to "Unable to find role: Unable to maintain role: Not_A_Role with reason: notfound",
            ),
          )
        }
    }

    @Test
    fun `Change role adminType returns bad request for no admin type`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf<String>())))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Validation failure:")
          assertThat(it["developerMessage"] as String).contains("default message [Admin type cannot be empty]")
        }
    }

    @Test
    fun `Change role admin type returns bad request when adminType does not exist`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DOES_NOT_EXIST"))))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("Parameter conversion failure:")
          assertThat(it["developerMessage"] as String).contains("400 BAD_REQUEST \"Failed to read HTTP message\"")
        }
    }
  }
}
