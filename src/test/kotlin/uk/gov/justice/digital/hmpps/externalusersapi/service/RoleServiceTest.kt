package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleNotFoundException

class RoleServiceTest {
  private val roleRepository: RoleRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val roleService = RoleService(roleRepository, telemetryClient, authenticationFacade)

  @Nested
  inner class CreateRoles {
    @Test
    fun `create role`() {
      val createRole = CreateRole(
        roleCode = "ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(AdminType.EXT_ADM)
      )
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)
      whenever(authenticationFacade.currentUsername).thenReturn("user")

      roleService.createRole(createRole)
      val authority = Authority("ROLE", " Role Name", "Role description", mutableListOf(AdminType.EXT_ADM))
      verify(roleRepository).findByRoleCode("ROLE")
      verify(roleRepository).save(authority)
      verify(telemetryClient).trackEvent(
        "RoleCreateSuccess",
        mapOf(
          "username" to "user",
          "roleCode" to "ROLE",
          "roleName" to "Role Name",
          "roleDescription" to "Role description",
          "adminType" to "[EXT_ADM]"
        ),
        null
      )
    }

    @Test
    fun `create role - having adminType DPS_LSA will auto add DPS_ADM`() {
      val createRole = CreateRole(
        roleCode = "ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(AdminType.DPS_LSA)
      )
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)
      whenever(authenticationFacade.currentUsername).thenReturn("user")

      roleService.createRole(createRole)
      val authority =
        Authority("ROLE", " Role Name", "Role description", mutableListOf(AdminType.DPS_LSA, AdminType.DPS_ADM))
      verify(roleRepository).findByRoleCode("ROLE")
      verify(roleRepository).save(authority)
      verify(telemetryClient).trackEvent(
        "RoleCreateSuccess",
        mapOf(
          "username" to "user",
          "roleCode" to "ROLE",
          "roleName" to "Role Name",
          "roleDescription" to "Role description",
          "adminType" to "[DPS_LSA, DPS_ADM]"
        ),
        null
      )
    }

    @Test
    fun `Create role exists`() {
      val createRole = CreateRole(
        roleCode = "NEW_ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(AdminType.DPS_LSA)
      )
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(
        Authority(
          roleCode = "NEW_ROLE",
          roleName = "Role Name",
          roleDescription = "Role description",
          adminType = mutableListOf(AdminType.DPS_LSA)
        )
      )

      Assertions.assertThatThrownBy {
        roleService.createRole(createRole)
      }.isInstanceOf(RoleService.RoleExistsException::class.java)
        .hasMessage("Unable to create role: NEW_ROLE with reason: role code already exists")
    }
  }

  @Nested
  inner class GetRoles {
    @Test
    fun `get roles`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf(role1, role2))

      val allRoles = roleService.getRoles(null)
      assertThat(allRoles.size).isEqualTo(2)
    }

    @Test
    fun `get roles returns no roles`() {
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf())

      val allRoles = roleService.getRoles(null)
      assertThat(allRoles.size).isEqualTo(0)
    }

    @Test
    fun `get roles check filter`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf(role1, role2))

      roleService.getRoles(listOf(AdminType.DPS_ADM, AdminType.DPS_LSA))
      verify(roleRepository).findAll(
        check {
          assertThat(it).extracting("adminTypes").isEqualTo(listOf(AdminType.DPS_ADM, AdminType.DPS_LSA))
        },
        eq(Sort.by(Sort.Direction.ASC, "roleName"))
      )
    }
  }

  @Nested
  inner class GetPagedRoles {
    @Test
    fun `get all roles`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      val roles = listOf(role1, role2)
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(PageImpl(roles))

      val allRoles = roleService.getRoles(null, null, null, Pageable.unpaged())
      assertThat(allRoles.size).isEqualTo(2)
    }

    @Test
    fun `get all roles returns no roles`() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())

      val allRoles = roleService.getRoles(null, null, null, Pageable.unpaged())
      assertThat(allRoles.size).isEqualTo(0)
    }

    @Test
    fun `get All Roles check filter - multiple `() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      roleService.getRoles(
        "Admin",
        "HWPV",
        listOf(AdminType.EXT_ADM, AdminType.DPS_LSA),
        unpaged,
      )
      verify(roleRepository).findAll(
        check {
          assertThat(it).extracting("roleName").isEqualTo("Admin")
          assertThat(it).extracting("roleCode").isEqualTo("HWPV")
          assertThat(it).extracting("adminTypes").isEqualTo(listOf(AdminType.EXT_ADM, AdminType.DPS_LSA))
        },
        eq(unpaged)
      )
    }
    @Test
    fun `get All Roles check filter - roleName`() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      roleService.getRoles(
        "Admin",
        null,
        null,
        unpaged,
      )
      verify(roleRepository).findAll(
        check {
          assertThat(it).extracting("roleName").isEqualTo("Admin")
        },
        eq(unpaged)
      )
    }

    @Test
    fun `get All Roles check filter - roleCode`() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      roleService.getRoles(
        null,
        "HWPV",
        null,
        unpaged,
      )
      verify(roleRepository).findAll(
        check {
          assertThat(it).extracting("roleCode").isEqualTo("HWPV")
        },
        eq(unpaged)
      )
    }

    @Test
    fun `get All Roles check filter - adminType`() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      roleService.getRoles(
        null,
        null,
        listOf(AdminType.DPS_ADM, AdminType.DPS_LSA),
        unpaged,
      )
      verify(roleRepository).findAll(
        check {
          assertThat(it).extracting("adminTypes").isEqualTo(listOf(AdminType.DPS_ADM, AdminType.DPS_LSA))
        },
        eq(unpaged)
      )
    }
  }

  @Nested
  inner class RoleDetails {

    @Test
    fun `get role details`() {
      val dbRole = Authority(roleCode = "RO1", roleName = "Role Name", roleDescription = "A Role")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      val role = roleService.getRoleDetails("RO1")
      assertThat(role).isEqualTo(dbRole)
      verify(roleRepository).findByRoleCode("RO1")
    }

    @Test
    fun `get role details when no role matches`() {
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)

      assertThatThrownBy {
        roleService.getRoleDetails("RO1")
      }.isInstanceOf(RoleNotFoundException::class.java)
    }
  }
}
