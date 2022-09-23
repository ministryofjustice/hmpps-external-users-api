package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService

class RoleControllerTest {
  private val roleService: RoleService = mock()
  private val roleController = RoleController(roleService)

  @Nested
  inner class GetRoles {
    @Test
    fun `get roles`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      whenever(roleService.getRoles(any())).thenReturn(listOf(role1, role2))

      val roles = roleController.getRoles(listOf())
      verify(roleService).getRoles(listOf())
      assertThat(roles.size).isEqualTo(2)
    }

    @Test
    fun `No Roles Found`() {
      whenever(roleService.getRoles(any())).thenReturn(listOf())

      val noRoles = roleController.getRoles(listOf())
      verify(roleService).getRoles(listOf())
      assertThat(noRoles.size).isEqualTo(0)
    }
  }
}
