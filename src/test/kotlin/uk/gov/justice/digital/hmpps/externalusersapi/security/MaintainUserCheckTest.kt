package uk.gov.justice.digital.hmpps.externalusersapi.security

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group

class MaintainUserCheckTest {
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val groupRepository: GroupRepository = mock()
  private val grantedAuthority: GrantedAuthority = mock()

  private val maintainUserCheck = MaintainUserCheck(authenticationFacade, groupRepository)

  @BeforeEach
  fun initSecurityContext(): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn("username")
    whenever(authenticationFacade.hasRoles(anyOrNull())).thenReturn(false)
  }

  @Nested
  inner class EnsureMaintainerGroupRelationship {
    @Test
    fun `Group manager able to maintain group`(): Unit = runBlocking {
      whenever(groupRepository.findGroupsByUsername("groupManager")).thenReturn(flowOf(Group("group1", "desc"), Group("group2", "desc")))

      assertThatCode {
        runBlocking {
          maintainUserCheck.ensureMaintainerGroupRelationship(
            "groupManager",
            "group1",
          )
        }
      }.doesNotThrowAnyException()

      verify(groupRepository).findGroupsByUsername("groupManager")
    }

    @Test
    fun `Group manager does not have group so cannot maintain`(): Unit = runBlocking {
      whenever(groupRepository.findGroupsByUsername("groupManager")).thenReturn(flowOf(Group("group1", "desc"), Group("group2", "desc")))
      assertThatThrownBy {
        runBlocking {
          maintainUserCheck.ensureMaintainerGroupRelationship(
            "groupManager",
            "group3",
          )
        }
      }.isInstanceOf(GroupRelationshipException::class.java)
        .hasMessage("Unable to maintain group: group3 with reason: Group not with your groups")

      verify(groupRepository).findGroupsByUsername("groupManager")
    }
  }

  @Nested
  inner class EnsureLoggedInUserRelationship {
    @Test
    fun `Logged in user can maintain users`(): Unit = runBlocking {
      givenLoggedInUserHasRole("ROLE_MAINTAIN_OAUTH_USERS")
      assertThatCode {
        runBlocking {
          maintainUserCheck.ensureUserLoggedInUserRelationship("testy")
        }
      }.doesNotThrowAnyException()
    }

    @Test
    fun `Logged in user has a group in common with user they are attempting to maintain for`(): Unit = runBlocking {
      givenLoggedInUserHasRole("ROLE_AUTH_GROUP_MANAGER")
      whenever(authenticationFacade.getUsername()).thenReturn("user")

      whenever(groupRepository.findGroupsByUsername("user")).thenReturn(flowOf(Group("GROUP_1", "first group"), Group("GROUP_2", "second group")))
      whenever(groupRepository.findGroupsByUsername("testy")).thenReturn(flowOf(Group("GROUP_3", "third group"), Group("GROUP_2", "second group")))

      assertThatCode {
        runBlocking {
          maintainUserCheck.ensureUserLoggedInUserRelationship("testy")
        }
      }.doesNotThrowAnyException()
    }

    @Test
    fun `Logged in user does not have a group in common with user they are attempting to maintain for`(): Unit = runBlocking {
      givenLoggedInUserHasRole("ROLE_AUTH_GROUP_MANAGER")
      whenever(authenticationFacade.getUsername()).thenReturn("user")

      whenever(groupRepository.findGroupsByUsername("user")).thenReturn(flowOf(Group("GROUP_1", "first group"), Group("GROUP_2", "second group")))
      whenever(groupRepository.findGroupsByUsername("testy")).thenReturn(flowOf(Group("GROUP_3", "third group"), Group("GROUP_4", "fourth group")))

      assertThatThrownBy {
        runBlocking {
          maintainUserCheck.ensureUserLoggedInUserRelationship("testy")
        }
      }.isInstanceOf(UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: testy with reason: User not with your groups")
    }

    private fun givenLoggedInUserHasRole(userRole: String): Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(listOf(grantedAuthority))
      whenever(grantedAuthority.authority).thenReturn(userRole)
    }
  }
}
