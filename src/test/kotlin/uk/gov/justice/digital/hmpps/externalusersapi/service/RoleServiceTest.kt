package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority

class RoleServiceTest {
  private val roleRepository: RoleRepository = mock()
  private val rolesService = RoleService(roleRepository)

  @Nested
  inner class GetRoles {
    @Test
    fun `get roles`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf(role1, role2))

      val allRoles = rolesService.getRoles(null)
      assertThat(allRoles.size).isEqualTo(2)
    }

    @Test
    fun `get roles returns no roles`() {
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf())

      val allRoles = rolesService.getRoles(null)
      assertThat(allRoles.size).isEqualTo(0)
    }

    @Test
    fun `get roles check filter`() {
      val role1 = Authority(roleCode = "RO1", roleName = "Role1", roleDescription = "First Role")
      val role2 = Authority(roleCode = "RO2", roleName = "Role2", roleDescription = "Second Role")
      whenever(roleRepository.findAll(any(), any<Sort>())).thenReturn(listOf(role1, role2))

      rolesService.getRoles(listOf(AdminType.DPS_ADM, AdminType.DPS_LSA))
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

      val allRoles = rolesService.getRoles(null, null, null, Pageable.unpaged())
      assertThat(allRoles.size).isEqualTo(2)
    }

    @Test
    fun `get all roles returns no roles`() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())

      val allRoles = rolesService.getRoles(null, null, null, Pageable.unpaged())
      assertThat(allRoles.size).isEqualTo(0)
    }

    @Test
    fun `get All Roles check filter - multiple `() {
      whenever(roleRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      rolesService.getRoles(
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
      rolesService.getRoles(
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
      rolesService.getRoles(
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
      rolesService.getRoles(
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
}
