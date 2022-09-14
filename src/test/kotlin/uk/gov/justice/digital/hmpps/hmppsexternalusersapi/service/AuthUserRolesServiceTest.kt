package uk.gov.justice.digital.hmpps.hmppsexternalusersapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.Authority

internal class AuthUserRolesServiceTest {
  private val roleRepository: RoleRepository = mock()

  private val authUserRoleService = AuthUserRoleService(roleRepository)

  @Nested
  inner class AllRoles {
    @Test
    fun allRoles() {
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("ROLE_MAINTAIN_OAUTH_USERS", "Maintain external users")
      val authorities = listOf(role, role2)

      whenever(roleRepository.findAllByOrderByRoleNameLike(anyString())).thenReturn(authorities)
      authUserRoleService.allRoles
      verify(roleRepository).findAllByOrderByRoleNameLike(
        AdminType.EXT_ADM.adminTypeCode
      )
    }
  }
}
