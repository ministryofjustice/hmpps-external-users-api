package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
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
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleNotFoundException
import java.util.UUID

class RoleControllerTest {
  private val roleService: RoleService = mock()
  private val roleController = RoleController(roleService)

  @Nested
  inner class CreateRole {
    @Test
    fun create(): Unit = runBlocking {
      val newRole = CreateRole("CG", "Role", "Desc", mutableSetOf(AdminType.EXT_ADM))

      roleController.createRole(newRole)

      verify(roleService).createRole(newRole)
    }

    @Test
    fun `create - role can be created when description not present `(): Unit = runBlocking {
      val newRole = CreateRole(roleCode = "CG", roleName = "Role", adminType = mutableSetOf(AdminType.EXT_ADM, AdminType.EXT_ADM))

      roleController.createRole(newRole)

      verify(roleService).createRole(newRole)
    }

    @Test
    fun `create - role already exist exception`() {
      runBlocking {
        doThrow(RoleService.RoleExistsException("_code", "role code already exists")).whenever(roleService).createRole(any())
      }

      @Suppress("ClassName")
      val role = CreateRole("_code", " Role", "Description", mutableSetOf(DPS_ADM))
      assertThatThrownBy { runBlocking { roleController.createRole(role) } }
        .isInstanceOf(RoleService.RoleExistsException::class.java)
        .withFailMessage("Unable to maintain role: code with reason: role code already exists")
    }
  }

  @Nested
  inner class GetRoles {
    @Test
    fun `get roles`(): Unit = runBlocking {
      val role1 = Authority(id = UUID.randomUUID(), roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", "DPS_ADM")
      val role2 = Authority(id = UUID.randomUUID(), roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", "DPS_ADM")
      whenever(roleService.getRoles(any())).thenReturn(listOf(role1, role2))

      val roles = roleController.getRoles(listOf())
      verify(roleService).getRoles(listOf())
      assertThat(roles.size).isEqualTo(2)
    }

    @Test
    fun `No Roles Found`(): Unit = runBlocking {
      whenever(roleService.getRoles(any())).thenReturn(listOf())

      val noRoles = roleController.getRoles(listOf())
      verify(roleService).getRoles(listOf())
      assertThat(noRoles.size).isEqualTo(0)
    }
  }

  @Nested
  inner class GetPagedRoles {
    @Test
    fun `get roles`(): Unit = runBlocking {
      val role1 = Authority(id = UUID.randomUUID(), roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", "DPS_ADM")
      val role2 = Authority(id = UUID.randomUUID(), roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", "DPS_ADM")
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
    fun `No Roles Found`(): Unit = runBlocking {
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
    fun `Get role details`(): Unit = runBlocking {
      val role = Authority(
        roleCode = "RO1",
        roleName = "Role1",
        roleDescription = "First Role",
        adminType = "DPS_ADM"
      )

      whenever(roleService.getRoleDetails(any())).thenReturn(RoleDetails(role))

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
      runBlocking {
        whenever(roleService.getRoleDetails(any())).thenThrow(
          RoleNotFoundException(
            "find",
            "NoRole",
            "not found"
          )
        )
      }

      assertThatThrownBy { runBlocking { roleController.getRoleDetails("ROLE_DOES_NOT_EXIST") } }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `amend role name`(): Unit = runBlocking {
      val roleAmendment = RoleNameAmendment("role")
      roleController.amendRoleName("role1", roleAmendment)
      verify(roleService).updateRoleName("role1", roleAmendment)
    }

    @Test
    fun `amend role name with no match throws exception`() {
      runBlocking {
        whenever(roleService.updateRoleName(anyString(), any())).thenThrow(
          RoleNotFoundException(
            "find",
            "NoRole",
            "not found"
          )
        )
      }
      val roleAmendment = RoleNameAmendment("role")

      assertThatThrownBy { runBlocking { roleController.amendRoleName("NoRole", roleAmendment) } }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }

  @Nested
  inner class AmendRoleDescription {
    @Test
    fun `amend role description`(): Unit = runBlocking {
      val roleAmendment = RoleDescriptionAmendment("roleDesc")
      roleController.amendRoleDescription("role1", roleAmendment)
      verify(roleService).updateRoleDescription("role1", roleAmendment)
    }

    @Test
    fun `amend role description if no description set`(): Unit = runBlocking {
      val roleAmendment = RoleDescriptionAmendment(null)
      roleController.amendRoleDescription("role1", roleAmendment)
      verify(roleService).updateRoleDescription("role1", roleAmendment)
    }

    @Test
    fun `amend role description with no match throws exception`() {
      runBlocking {
        whenever(roleService.updateRoleDescription(anyString(), any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))
      }
      val roleAmendment = RoleDescriptionAmendment("role description")

      assertThatThrownBy { runBlocking { roleController.amendRoleDescription("NoRole", roleAmendment) } }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }

  @Nested
  inner class AmendRoleAdminType {
    @Test
    fun `amend role admin type`(): Unit = runBlocking {
      val roleAmendment = RoleAdminTypeAmendment(mutableSetOf(DPS_ADM))
      roleController.amendRoleAdminType("role1", roleAmendment)
      verify(roleService).updateRoleAdminType("role1", roleAmendment)
    }

    @Test
    fun `amend role admin type with no match throws exception`() {
      runBlocking {
        whenever(roleService.updateRoleAdminType(anyString(), any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))
      }
      val roleAmendment = RoleAdminTypeAmendment(mutableSetOf(DPS_ADM))

      assertThatThrownBy { runBlocking { roleController.amendRoleAdminType("NoRole", roleAmendment) } }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }
}
