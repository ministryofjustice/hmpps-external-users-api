package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleFilter
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleDescriptionAmendmentDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleDetailsDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleNameAmendmentDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType.EXT_ADM
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleNotFoundException
import java.util.UUID

class RoleServiceTest {
  private val roleRepository: RoleRepository = mock()
  private val roleSearchRepository: RoleSearchRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val roleService = RoleService(roleSearchRepository, roleRepository, telemetryClient, authenticationFacade)

  @Nested
  inner class CreateRoles {
    @Test
    fun `create role`(): Unit = runBlocking {
      val createRole = CreateRoleDto(
        roleCode = "ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(EXT_ADM),
      )
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)
      whenever(authenticationFacade.getUsername()).thenReturn("user")

      roleService.createRole(createRole)
      val authority = Authority(null, "ROLE", " Role Name", "Role description", "EXT_ADM")
      verify(roleRepository).findByRoleCode("ROLE")
      verify(roleRepository).save(authority)
      verify(telemetryClient).trackEvent(
        "RoleCreateSuccess",
        mapOf(
          "username" to "user",
          "roleCode" to "ROLE",
          "roleName" to "Role Name",
          "roleDescription" to "Role description",
          "adminType" to "EXT_ADM",
        ),
        null,
      )
    }

    @Test
    fun `create role - having adminType DPS_LSA will auto add DPS_ADM`(): Unit = runBlocking {
      val createRole = CreateRoleDto(
        roleCode = "ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(DPS_LSA),
      )
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)
      whenever(authenticationFacade.getUsername()).thenReturn("user")

      roleService.createRole(createRole)
      val authority =
        Authority(null, "ROLE", " Role Name", "Role description", "DPS_LSA,DPS_ADM")
      verify(roleRepository).findByRoleCode("ROLE")
      verify(roleRepository).save(authority)
      verify(telemetryClient).trackEvent(
        "RoleCreateSuccess",
        mapOf(
          "username" to "user",
          "roleCode" to "ROLE",
          "roleName" to "Role Name",
          "roleDescription" to "Role description",
          "adminType" to "DPS_LSA,DPS_ADM",
        ),
        null,
      )
    }

    @Test
    fun `Create role exists`(): Unit = runBlocking {
      val createRole = CreateRoleDto(
        roleCode = "NEW_ROLE",
        roleName = "Role Name",
        roleDescription = "Role description",
        adminType = mutableSetOf(DPS_LSA),
      )
      runBlocking {
        whenever(roleRepository.findByRoleCode(anyString())).thenReturn(
          Authority(
            roleCode = "NEW_ROLE",
            roleName = "Role Name",
            roleDescription = "Role description",
            adminType = DPS_LSA.adminTypeCode,
          ),
        )
      }

      assertThatThrownBy {
        runBlocking { roleService.createRole(createRole) }
      }.isInstanceOf(RoleExistsException::class.java)
        .hasMessage("Unable to create role: NEW_ROLE with reason: role code already exists")
    }
  }

  @Nested
  inner class GetRoles {
    @Test
    fun `get roles`(): Unit = runBlocking {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", adminType = DPS_ADM.adminTypeCode)
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", adminType = DPS_LSA.adminTypeCode)

      runBlocking { whenever(roleSearchRepository.searchForRoles(any())).thenReturn(flowOf(role1, role2)) }

      val allRoles = roleService.getRoles(null)
      assertThat(allRoles.toList().size).isEqualTo(2)
    }

    @Test
    fun `get roles returns no roles`(): Unit = runBlocking {
      whenever(roleSearchRepository.searchForRoles(any())).thenReturn(flowOf())

      val allRoles = roleService.getRoles(null)
      assertThat(allRoles.toList().size).isEqualTo(0)
    }

    @Test
    fun `get roles check filter`(): Unit = runBlocking {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", adminType = DPS_ADM.adminTypeCode)
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", adminType = DPS_LSA.adminTypeCode)
      runBlocking { whenever(roleSearchRepository.searchForRoles(any())).thenReturn(flowOf(role1, role2)) }

      roleService.getRoles(listOf(DPS_ADM, DPS_LSA))

      runBlocking {
        verify(roleSearchRepository).searchForRoles(any())
      }
    }
  }

  @Nested
  inner class GetPagedRoles {

    @Test
    fun `should retrieve paged roles`(): Unit = runBlocking {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", adminType = DPS_ADM.adminTypeCode)
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", adminType = DPS_LSA.adminTypeCode)
      runBlocking {
        whenever(roleSearchRepository.searchForRoles(any())).thenReturn(flowOf(role1, role2))
        whenever(roleSearchRepository.countAllBy(any())).thenReturn(25)
      }

      val rolesPage = roleService.getRoles(null, null, null, Pageable.ofSize(2))
      assertThat(rolesPage.get().toList().size).isEqualTo(2)
      assertThat(rolesPage.size).isEqualTo(2)
      assertThat(rolesPage.totalElements).isEqualTo(25)
    }

    @Test
    fun `should populate filter correctly`(): Unit = runBlocking {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role", adminType = DPS_ADM.adminTypeCode)
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role", adminType = DPS_LSA.adminTypeCode)
      runBlocking {
        whenever(roleSearchRepository.searchForRoles(any())).thenReturn(flowOf(role1, role2))
        whenever(roleSearchRepository.countAllBy(any())).thenReturn(25)
      }

      roleService.getRoles(
        "Admin",
        "HWPV",
        listOf(EXT_ADM, DPS_LSA),
        Pageable.ofSize(10),
      )

      val roleFilterCaptor = argumentCaptor<RoleFilter>()
      val expectedRoleFilter = RoleFilter(roleName = "Admin", roleCode = "HWPV", adminTypes = listOf(EXT_ADM, DPS_LSA), Pageable.ofSize(10))

      runBlocking {
        verify(roleSearchRepository).searchForRoles(roleFilterCaptor.capture())
        val actualRoleFilter = roleFilterCaptor.firstValue
        assertTrue(actualRoleFilter.sql == expectedRoleFilter.sql)
        assertTrue(actualRoleFilter.countSQL == expectedRoleFilter.countSQL)
        assertTrue(actualRoleFilter.countSQL == expectedRoleFilter.countSQL)
      }
    }
  }

  @Nested
  inner class RoleDetails {

    @Test
    fun `get role details`(): Unit = runBlocking {
      val dbRole = Authority(UUID.randomUUID(), roleCode = "RO1", roleName = "Role Name", roleDescription = "A Role", "DPS_ADM")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)
      val dbRoleDetails = RoleDetailsDto("RO1", "Role Name", "A Role", listOf(DPS_ADM))

      val role = roleService.getRoleDetails("RO1")
      assertThat(role).isEqualTo(dbRoleDetails)
      verify(roleRepository).findByRoleCode("RO1")
    }

    @Test
    fun `get role details when no role matches`(): Unit = runBlocking {
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          roleService.getRoleDetails("RO1")
        }
        assertThatThrownBy {
          runBlocking { roleService.getRoleDetails("RO1") }
        }.isInstanceOf(RoleNotFoundException::class.java)
      }
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `update role name when no role matches`(): Unit = runBlocking {
      val roleAmendment = RoleNameAmendmentDto("UpdatedName")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)

      assertThatThrownBy {
        runBlocking { roleService.updateRoleName("RO1", roleAmendment) }
      }.isInstanceOf(RoleNotFoundException::class.java)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update role name successfully`(): Unit = runBlocking {
      val dbRole = Authority(id = UUID.randomUUID(), roleCode = "RO1", roleName = "Role Name", roleDescription = "A Role", adminType = "DPS_ADM")
      val roleAmendment = RoleNameAmendmentDto("UpdatedName")
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleName("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleNameUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleName" to "UpdatedName"),
        null,
      )
    }
  }

  @Nested
  inner class AmendRoleDescription {

    @Test
    fun `update role description when no role matches`(): Unit = runBlocking {
      val roleAmendment = RoleDescriptionAmendmentDto("UpdatedDescription")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          roleService.updateRoleDescription("RO1", roleAmendment)
        }
      }.isInstanceOf(RoleNotFoundException::class.java)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update role description successfully`(): Unit = runBlocking {
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "DPS_ADM",
      )
      val roleAmendment = RoleDescriptionAmendmentDto("UpdatedDescription")
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleDescription("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleDescriptionUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleDescription" to "UpdatedDescription"),
        null,
      )
    }
  }

  @Nested
  inner class AmendRoleAdminType {

    @Test
    fun `update role admin type when no role matches`(): Unit = runBlocking {
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          roleService.updateRoleAdminType("RO1", roleAmendment)
        }
      }.isInstanceOf(RoleNotFoundException::class.java)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update role admin type successfully`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM, DPS_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type with adminType DPS_LSA will auto add DPS_ADM`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM, DPS_LSA))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_LSA,DPS_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type with adminType DPS_LSA will not add DPS_ADM if it already exists`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")

      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM, DPS_LSA))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_LSA,DPS_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type without DPS_ADM will not remove immutable DPS_ADM if it already exists`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type without EXT_ADM will not remove immutable EXT_ADM if it already exists`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(DPS_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "DPS_ADM,EXT_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type will not duplicate existing immutable EXT_ADM`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM, DPS_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)
      roleService.updateRoleAdminType("RO1", roleAmendment)

      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_ADM"),
        null,
      )
    }

    @Test
    fun `update role admin type without DPS_LSA will remove DPS_LSA if it already exists`(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("user")
      val dbRole = Authority(
        id = UUID.randomUUID(),
        roleCode = "RO1",
        roleName = "Role Name",
        roleDescription = "Role Desc",
        adminType = "EXT_ADM,DPS_ADM,DPS_LSA",
      )
      val roleAmendment = RoleAdminTypeAmendmentDto(mutableSetOf(EXT_ADM))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(dbRole)

      roleService.updateRoleAdminType("RO1", roleAmendment)
      verify(roleRepository).findByRoleCode("RO1")
      verify(roleRepository).save(dbRole)
      verify(telemetryClient).trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to "user", "roleCode" to "RO1", "newRoleAdminType" to "EXT_ADM,DPS_ADM"),
        null,
      )
    }
  }
}
