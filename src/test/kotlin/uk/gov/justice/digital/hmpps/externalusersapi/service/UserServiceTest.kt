package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.time.LocalDateTime
import java.util.UUID.fromString

class UserServiceTest {
  private val userRepository: UserRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val user = User("someuser", AuthSource.auth)
  private val userService = UserService(userRepository, maintainUserCheck, authenticationFacade, telemetryClient, 90)

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
            "User not with your groups",
          ),
        ).whenever(maintainUserCheck)
          .ensureUserLoggedInUserRelationship(anyString())
        assertThatThrownBy {
          runBlocking {
            userService.enableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            )
          }
        }.isInstanceOf(
          UserGroupRelationshipException::class.java,
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
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            )
          }
        }.isInstanceOf(UserNotFoundException::class.java)
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
          LocalDateTime.now().minusDays(82),
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
          null,
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
            "User not with your groups",
          ),
        ).whenever(maintainUserCheck)
          .ensureUserLoggedInUserRelationship(anyString())
        assertThatThrownBy {
          runBlocking {
            userService.disableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
              "A Reason",
            )
          }
        }.isInstanceOf(
          UserGroupRelationshipException::class.java,
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
          null,
        )
      }

      @Test
      fun `disable User By UserId_NotFound`(): Unit = runBlocking {
        assertThatThrownBy {
          runBlocking {
            userService.disableUserByUserId(
              fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
              "A Reason",
            )
          }
        }.isInstanceOf(UserNotFoundException::class.java)
          .withFailMessage("User 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
      }
    }
  }

  @Nested
  inner class GetAllExternalUsers {

    @Test
    fun `getAllUsersLastName should return list of UserLastNameDto`(): Unit = runBlocking {
      val users = listOf(
        createSampleUser(username = "bob", source = AuthSource.auth, lastName = "Doe"),
        createSampleUser(username = "bob", source = AuthSource.auth, lastName = "Smith"),
      )
      whenever(userRepository.findAllBySource()).thenReturn(flowOf(*users.toTypedArray()))

      val result = userService.getAllUsersLastName()

      assertThat(result.size).isEqualTo(2)
      assertThat(result[0].lastName).isEqualTo("Doe")
      assertThat(result[1].lastName).isEqualTo("Smith")
      verify(userRepository, times(1)).findAllBySource()
      verifyNoMoreInteractions(userRepository)
    }
  }
}
