package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

class UserSearchServiceTest {

  private val userGroupService: UserGroupService = mock()
  private val userSearchRepository: UserSearchRepository = mock()
  private val userRepository: UserRepository = mock()

  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(userGroupService, userSearchRepository, userRepository)
  }

  @Nested
  inner class FindAuthUsersByEmail {

    @Test
    fun shouldNotInvokeRepositoryWhenEmailNull(): Unit = runBlocking {
      userSearchService.findAuthUsersByEmail(null)

      verify(userRepository, never()).findByEmailAndSourceOrderByUsername(null)
    }

    @Test
    fun shouldNotInvokeRepositoryWhenEmailEmpty(): Unit = runBlocking {
      userSearchService.findAuthUsersByEmail("")

      verify(userRepository, never()).findByEmailAndSourceOrderByUsername("")
    }

    @Test
    fun shouldCleanEmailAndInvokeRepositoryWhenEmailPresent(): Unit = runBlocking {
      userSearchService.findAuthUsersByEmail("  FRED@testy.co.uk  ")

      verify(userRepository).findByEmailAndSourceOrderByUsername("fred@testy.co.uk")
    }

    @Test
    fun shouldReturnFlowDataFromRepository(): Unit = runBlocking() {
      val email = "fred@testy.co.uk"
      val user = User(username = email, source = AuthSource.auth)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(email)).thenReturn(flowOf(user))

      val actualUsers = userSearchService.findAuthUsersByEmail(email)

      assertThat(actualUsers.toList()).containsOnly(user)
    }
  }

  @Nested
  inner class FindExternalUsersByUserName {
    @Test
    fun shouldFailWithUserException(): Unit = runBlocking {

      Assertions.assertThatThrownBy {
        runBlocking {
          userSearchService.getUserByUsername("   bob   ")
        }
      }.isInstanceOf(UsernameNotFoundException::class.java)
        .hasMessage("Account for username    bob    not found")
    }

    @Test
    fun externalUserByUsername(): Unit = runBlocking {
      val mockUser = User("BOB", AuthSource.auth)
      whenever(userRepository.findByUsernameAndSource(anyOrNull(), anyOrNull())).thenReturn(mockUser)
      val user = userSearchService.getUserByUsername("   bob   ")
      verify(userRepository).findByUsernameAndSource("BOB", AuthSource.auth)
      assertThat(user).isEqualTo(mockUser)
    }
  }
}
