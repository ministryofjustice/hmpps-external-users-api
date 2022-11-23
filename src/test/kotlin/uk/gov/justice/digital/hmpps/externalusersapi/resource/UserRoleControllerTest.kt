package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserRole
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
      val role2 = Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", adminType = "DPS_ADM")
      whenever(userRoleService.getUserRoles(any())).thenReturn(
        mutableListOf(
          role1,
          role2
        )
      )
      val responseEntity = userRoleController.rolesByUserId(UUID.randomUUID())
      assertThat(responseEntity).containsOnly(
        UserRole(role1),
        UserRole(role2)

      )
    }
  }
}
