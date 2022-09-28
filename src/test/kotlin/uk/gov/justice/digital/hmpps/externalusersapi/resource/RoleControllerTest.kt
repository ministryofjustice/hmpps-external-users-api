package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleNotFoundException

class RoleControllerTest {
  private val roleService: RoleService = mock()
  private val roleController = RoleController(roleService)

  @Nested
  inner class CreateRole {
    @Test
    fun create() {
      val newRole = CreateRole("CG", "Role", "Desc", mutableSetOf(AdminType.EXT_ADM))

      roleController.createRole(newRole)

      verify(roleService).createRole(newRole)
    }

    @Test
    fun `create - role can be created when description not present `() {
      val newRole = CreateRole(roleCode = "CG", roleName = "Role", adminType = mutableSetOf(AdminType.EXT_ADM, AdminType.EXT_ADM))

      roleController.createRole(newRole)

      verify(roleService).createRole(newRole)
    }

    @Test
    fun `create - role already exist exception`() {
      doThrow(RoleService.RoleExistsException("_code", "role code already exists")).whenever(roleService).createRole(any())

      @Suppress("ClassName")
      val role = CreateRole("_code", " Role", "Description", mutableSetOf(DPS_ADM))
      Assertions.assertThatThrownBy { roleController.createRole(role) }
        .isInstanceOf(RoleService.RoleExistsException::class.java)
        .withFailMessage("Unable to maintain role: code with reason: role code already exists")
    }
  }

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

  @Nested
  inner class GetPagedRoles {
    @Test
    fun `get roles`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      val roles = listOf(role1, role2)
      whenever(roleService.getRoles(isNull(), isNull(), any(), any())).thenReturn(PageImpl(roles))

      val allRoles = roleController.getRoles(null, null, listOf(), Pageable.unpaged())
      verify(roleService).getRoles(
        null,
        null,
        listOf(),
        Pageable.unpaged(),
      )
      assertThat(allRoles.size).isEqualTo(2)
    }

    @Test
    fun `No Roles Found`() {
      whenever(roleService.getRoles(isNull(), isNull(), any(), any())).thenReturn(PageImpl(listOf()))

      val noRoles = roleController.getRoles(null, null, listOf(), Pageable.unpaged())
      verify(roleService).getRoles(
        null,
        null,
        listOf(),
        Pageable.unpaged(),
      )
      assertThat(noRoles.size).isEqualTo(0)
    }
  }

  @Nested
  inner class RoleDetail {
    @Test
    fun `Get role details`() {
      val role = Authority(
        roleCode = "RO1",
        roleName = "Role1",
        roleDescription = "First Role",
        adminType = listOf(DPS_ADM)
      )

      whenever(roleService.getRoleDetails(any())).thenReturn(role)

      val roleDetails = roleController.getRoleDetails("RO1")
      assertThat(roleDetails).isEqualTo(
        RoleDetails(
          roleCode = "RO1",
          roleName = "Role1",
          roleDescription = "First Role",
          adminType = listOf(DPS_ADM)
        )
      )
    }

    @Test
    fun `Get role details with no match throws exception`() {
      whenever(roleService.getRoleDetails(any())).thenThrow(
        RoleNotFoundException(
          "find",
          "NoRole",
          "not found"
        )
      )

      Assertions.assertThatThrownBy { roleController.getRoleDetails("ROLE_DOES_NOT_EXIST") }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }
}
