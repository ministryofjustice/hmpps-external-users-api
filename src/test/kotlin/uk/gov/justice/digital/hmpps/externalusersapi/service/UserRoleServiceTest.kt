package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import java.util.UUID

internal class UserRoleServiceTest {
  private val userRepository: UserRepository = mock()
  private val roleRepository: RoleRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val service = UserRoleService(userRepository, maintainUserCheck, authenticationFacade, roleRepository)

  @Nested
  inner class GetAuthUserByUserId {
    @Test
    fun getRolesSuccess(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
      val id = UUID.randomUUID()
      val user = UserHelper.createSampleUser(username = "user")

      whenever(userRepository.findById(id)).thenReturn(user)

      whenever(roleRepository.findRolesByUserId(any())).thenReturn(
        flowOf(
          Authority(UUID.randomUUID(), "GROUP_ONE", "Group One info", adminType = "EXT_ADM"),
          Authority(UUID.randomUUID(), "GLOBAL_SEARCH", "Global Search", "Allow user to search globally for a user", "EXT_ADM")
        )
      )

      val roles = service.getUserRoles(id)
      assertThat(roles).extracting<String> { it.roleCode }.containsOnly("GROUP_ONE", "GLOBAL_SEARCH")
    }

    @Test
    fun getRolesUserNotFound(): Unit = runBlocking {
      val id = UUID.randomUUID()
      whenever(userRepository.findById(id)).thenReturn(null)
      val roles = service.getUserRoles(id)
      assertThat(roles).isNull()
    }
  }
}
