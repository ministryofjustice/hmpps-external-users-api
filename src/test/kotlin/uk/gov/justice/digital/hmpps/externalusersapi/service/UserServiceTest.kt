package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.fromString

class UserServiceTest {
  private val userRepository: UserRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val user = User("someuser", AuthSource.auth)
  private val userService = UserService(userRepository, groupRepository, maintainUserCheck, authenticationFacade, telemetryClient, 90)

  @Nested
  inner class EnableUserByUserId {

    @BeforeEach
    fun initSecurityContext(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("adminuser")
    }

    @Nested
    inner class EnableUserByUserId {
      @Test
      fun enableUserByUserId_superUser(): Unit = runBlocking {
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.enableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
        assertThat(user).extracting { it.isEnabled() }.isEqualTo(true)
        verify(userRepository).save(user)
      }

      @Test
      fun `enable User by userId invalidGroup_GroupManager`(): Unit = runBlocking {
        val user = User("someuser", AuthSource.auth)
        whenever(userRepository.findById(any())).thenReturn(user)
        doThrow(
          UserGroupRelationshipException(
            "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a",
            "User not with your groups"
          )
        ).whenever(maintainUserCheck)
          .ensureUserLoggedInUserRelationship(anyString())
        assertThatThrownBy {
          runBlocking {
            userService.enableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
            )
          }
        }.isInstanceOf(
          UserGroupRelationshipException::class.java
        )
          .hasMessage("Unable to maintain user: 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a with reason: User not with your groups")
      }

      @Test
      fun `enable User by userId validGroup_groupManager`(): Unit = runBlocking {
        val user = User("someuser", AuthSource.auth)
        whenever(userRepository.findById(any()))
          .thenReturn(user)
        userService.enableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
        assertThat(user).extracting { it.isEnabled() }.isEqualTo(true)
        verify(userRepository).save(user)
      }

      @Test
      fun `enable User By UserId_NotFound`(): Unit = runBlocking {
        assertThatThrownBy {
          runBlocking {
            userService.enableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
            )
          }
        }.isInstanceOf(UsernameNotFoundException::class.java)
          .withFailMessage("User 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
      }

      @Test
      fun `enable User by userId set LastLoggedIn`(): Unit = runBlocking {
        val userToCheck = user
        val tooLongAgo = LocalDateTime.now().minusDays(95)
        userToCheck.lastLoggedIn = tooLongAgo
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.enableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
        assertThat(userToCheck.lastLoggedIn).isBetween(
          LocalDateTime.now().minusDays(84),
          LocalDateTime.now().minusDays(82)
        )
      }

      @Test
      fun `enable User by userId leave LastLoggedIn alone`(): Unit = runBlocking {
        val userToCheck = user
        val fiveDaysAgo = LocalDateTime.now().minusDays(5)
        userToCheck.lastLoggedIn = fiveDaysAgo
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.enableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
        assertThat(userToCheck.lastLoggedIn).isEqualTo(fiveDaysAgo)
      }

      @Test
      fun `enable user by userId track event`(): Unit = runBlocking {
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.enableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
        verify(telemetryClient).trackEvent(
          "ExternalUserEnabled",
          mapOf("username" to "someuser", "admin" to "adminuser"),
          null
        )
      }
    }

    @Nested
    inner class DisableUserByUserId {

      @Test
      fun disableUserByUserId_superUser(): Unit = runBlocking {
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.disableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason")
        assertThat(user).extracting { it.isEnabled() }.isEqualTo(false)
        verify(userRepository).save(user)
      }

      @Test
      fun `disable User by userId invalidGroup_GroupManager`(): Unit = runBlocking {
        whenever(userRepository.findById(any())).thenReturn(user)
        doThrow(
          UserGroupRelationshipException(
            "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a",
            "User not with your groups"
          )
        ).whenever(maintainUserCheck)
          .ensureUserLoggedInUserRelationship(anyString())
        assertThatThrownBy {
          runBlocking {
            userService.disableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason"
            )
          }
        }.isInstanceOf(
          UserGroupRelationshipException::class.java
        )
          .hasMessage("Unable to maintain user: 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a with reason: User not with your groups")
      }

      @Test
      fun `enable User by userId validGroup_groupManager`(): Unit = runBlocking {
        whenever(userRepository.findById(any()))
          .thenReturn(user)
        userService.disableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason")
        assertThat(user).extracting { it.isEnabled() }.isEqualTo(false)
        verify(userRepository).save(user)
      }

      @Test
      fun `disable user by userId track event`(): Unit = runBlocking {
        whenever(userRepository.findById(any())).thenReturn(user)
        userService.disableUserByUserId(fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason")
        verify(telemetryClient).trackEvent(
          "ExternalUserDisabled",
          mapOf("username" to "someuser", "admin" to "adminuser"),
          null
        )
      }

      @Test
      fun `disable User By UserId_NotFound`(): Unit = runBlocking {
        assertThatThrownBy {
          runBlocking {
            userService.disableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason"
            )
          }
        }.isInstanceOf(UsernameNotFoundException::class.java)
          .withFailMessage("User 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
      }
    }

    @Nested
    inner class FindUserByUserIdForMaintenance {

      private val userId = UUID.randomUUID()

      @Test
      fun `should throw exception when user not found`(): Unit = runBlocking {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy {
          runBlocking {
            userService.findUserForEmailUpdate(userId)
          }
        }.isInstanceOf(
          UsernameNotFoundException::class.java
        )
          .hasMessage("User $userId not found")

        verify(maintainUserCheck, never()).ensureUserLoggedInUserRelationship(anyString())
      }

      @Test
      fun `should throw exception when maintain user check fails`(): Unit = runBlocking {
        doThrow(UserGroupRelationshipException("Bob", "User not with your groups"))
          .whenever(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString())
        whenever(userRepository.findById(userId)).thenReturn(user)

        assertThatThrownBy {
          runBlocking {
            userService.findUserForEmailUpdate(userId)
          }
        }.isInstanceOf(
          UserGroupRelationshipException::class.java
        )
          .hasMessage("Unable to maintain user: Bob with reason: User not with your groups")
      }

      @Test
      fun `should respond with user when user found and in PECS group`(): Unit = runBlocking {
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(createGroup(UUID.randomUUID(), "PECS Group Test", "PECS Group Testing")))

        val userInfo = userService.findUserForEmailUpdate(userId)

        assertEquals(user, userInfo.first)
        assertTrue(userInfo.second)
        verify(maintainUserCheck).ensureUserLoggedInUserRelationship(user.name)
      }

      @Test
      fun `should respond with user when user found but not in PECS group`(): Unit = runBlocking {
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(createGroup(UUID.randomUUID(), "Group Test", "Group Testing")))

        val userInfo = userService.findUserForEmailUpdate(userId)

        assertEquals(user, userInfo.first)
        assertFalse(userInfo.second)
        verify(maintainUserCheck).ensureUserLoggedInUserRelationship(user.name)
      }

      @Test
      fun `should respond with user when user found but not in any groups`(): Unit = runBlocking {
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf())

        val userInfo = userService.findUserForEmailUpdate(userId)

        assertEquals(user, userInfo.first)
        assertFalse(userInfo.second)
        verify(maintainUserCheck).ensureUserLoggedInUserRelationship(user.name)
      }

      private fun createGroup(id: UUID, groupCode: String, groupName: String): Group =
        Group(groupCode, groupName, id)
    }
  }
}
