package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserRoleDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserRoleService
import java.util.UUID

class UserRoleControllerTest {
  private val userRoleService: UserRoleService = mock()
  private val userRoleController = UserRoleController(userRoleService)

  @Nested
  inner class RolesByUserId {
    @Test
    fun rolesByUserId_userNotFound(): Unit = runBlocking {
      assertThatThrownBy {
        runBlocking {
          userRoleController.rolesByUserId(UUID.randomUUID())
        }
      }
        .isInstanceOf(UsernameNotFoundException::class.java)
        .withFailMessage("Account for userId 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
    }

    @Test
    fun rolesByUserId_success(): Unit = runBlocking {
      val role1 = Authority(UUID.randomUUID(), "FRED", "desc", adminType = "DPS_ADM")
      val role2 = Authority(
        UUID.randomUUID(),
        "GLOBAL_SEARCH",
        "Global Search",
        "Allow user to search globally for a user",
        adminType = "DPS_ADM"
      )
      whenever(userRoleService.getUserRoles(any())).thenReturn(
        mutableListOf(
          role1,
          role2
        )
      )
      val responseEntity = userRoleController.rolesByUserId(UUID.randomUUID())
      assertThat(responseEntity).containsOnly(
        UserRoleDto(role1),
        UserRoleDto(role2)

      )
    }
  }

  @Test
  fun addRolesByUserId(): Unit = runBlocking {
    val userId = UUID.randomUUID()
    val roles = listOf("roleCode")

    userRoleController.addRolesByUserId(userId, roles)
    verify(userRoleService).addRolesByUserId(userId, roles)
  }

  @Test
  fun removeRoleByUserId_success(): Unit = runBlocking {
    val roleId = UUID.randomUUID()
    userRoleController.removeRoleByUserId(roleId, "roleCode")
    verify(userRoleService).removeRoleByUserId(roleId, "roleCode")
  }

  @Test
  fun assignableRoles(): Unit = runBlocking {
    val role1 = Authority(UUID.randomUUID(), "FRED", "FRED", adminType = "EXT_ADM")
    val role2 = Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", adminType = "EXT_ADM")
    whenever(userRoleService.getAssignableRolesByUserId(any())).thenReturn(listOf(role1, role2))

    val response = userRoleController.assignableRoles(UUID.randomUUID())
    assertThat(response).containsOnly(UserRoleDto(role1), UserRoleDto(role2))
  }

  @Nested
  inner class ListOfRolesForUser {
    @Test
    fun userRoles_externalUser(): Unit = runBlocking {
      val role1 = Authority(UUID.randomUUID(), "FRED", "FRED", adminType = "EXT_ADM")
      val role2 = Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", adminType = "EXT_ADM")
      whenever(userRoleService.getRolesByUsername(any())).thenReturn(setOf(role1, role2))
      assertThat(userRoleController.userRoles("JOE")).contains(UserRoleDto(role1), UserRoleDto(role2))
    }

    @Test
    fun userRoles_notFound(): Unit = runBlocking {
      whenever(userRoleService.getRolesByUsername(any())).thenThrow(UsernameNotFoundException::class.java)
      assertThatThrownBy { runBlocking { userRoleController.userRoles("JOE") } }
        .isInstanceOf(UsernameNotFoundException::class.java)
    }
  }
}
