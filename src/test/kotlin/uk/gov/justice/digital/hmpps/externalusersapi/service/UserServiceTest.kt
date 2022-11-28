package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.time.LocalDateTime

class UserServiceTest {
  private val userRepository: UserRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val manageUsersApiService: ManageUsersApiService = mock()
  private val user = User("testy", AuthSource.auth)
  private val service = UserService(userRepository, maintainUserCheck, authenticationFacade, telemetryClient, manageUsersApiService, 90)

  @Nested
  inner class enableUserByUserId {
    @Test
    fun enableUserByUserId_superUser(): Unit = runBlocking {

      whenever(userRepository.findById(any())).thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(user).extracting { it.isEnabled() }.isEqualTo(true)
      verify(userRepository).save(user)
    }

    @Test
    fun `enable user by userId sends email`(): Unit = runBlocking {

      whenever(authenticationFacade.getUsername()).thenReturn("username")
      val user = User("testy", AuthSource.auth)
      user.email = "email"
      whenever(userRepository.findById(any())).thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      verify(manageUsersApiService).sendEnableEmail(user, "username")
    }

    @Test
    fun `enable User by userId invalidGroup_GroupManager`(): Unit = runBlocking {
      val user = User("testy", AuthSource.auth)
      whenever(userRepository.findById(any())).thenReturn(user)
      doThrow(UserGroupRelationshipException("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(anyString())
      assertThatThrownBy {
        runBlocking {
          service.enableUserByUserId(
            "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"
          )
        }
      }.isInstanceOf(
        UserGroupRelationshipException::class.java
      ).hasMessage("Unable to maintain user: 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a with reason: User not with your groups")
    }

    @Test
    fun `enable User by userId validGroup_groupManager`(): Unit = runBlocking {
      val user = User("testy", AuthSource.auth)
      whenever(userRepository.findById(any()))
        .thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(user).extracting { it.isEnabled() }.isEqualTo(true)
      verify(userRepository).save(user)
    }

    @Test
    fun `enable User By UserId_NotFound`(): Unit = runBlocking {
      assertThatThrownBy {
        runBlocking {
          service.enableUserByUserId(
            "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"
          )
        }
      }.isInstanceOf(UsernameNotFoundException::class.java)
        .withFailMessage("User 00000000-aaaa-0000-aaaa-0a0a0a0a0a0a not found")
    }

    @Test
    fun `enable User by userId track event`(): Unit = runBlocking {
      val user = User("testy", AuthSource.auth)
      whenever(authenticationFacade.getUsername()).thenReturn("username")
      whenever(userRepository.findById(any())).thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      verify(telemetryClient).trackEvent(
        "AuthUserEnabled",
        mapOf("username" to "testy", "admin" to "username"),
        null
      )
    }

    @Test
    fun `enable User by userId set LastLoggedIn`(): Unit = runBlocking {
      val userToCheck = user
      val tooLongAgo = LocalDateTime.now().minusDays(95)
      userToCheck.lastLoggedIn = tooLongAgo
      whenever(userRepository.findById(any())).thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(userToCheck.lastLoggedIn).isBetween(LocalDateTime.now().minusDays(84), LocalDateTime.now().minusDays(82))
    }

    @Test
    fun `enable User by userId leave LastLoggedIn alone`(): Unit = runBlocking {

      val userToCheck = user
      val fiveDaysAgo = LocalDateTime.now().minusDays(5)
      userToCheck.lastLoggedIn = fiveDaysAgo
      whenever(userRepository.findById(any())).thenReturn(user)
      service.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(userToCheck.lastLoggedIn).isEqualTo(fiveDaysAgo)
    }
  }
}
