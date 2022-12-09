package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserDto
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

class UserSearchServiceTest {

  private val userGroupService: UserGroupService = mock()
  private val userSearchRepository: UserSearchRepository = mock()
  private val userRepository: UserRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val user: UserDto = givenAUser()

  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(userGroupService, userSearchRepository, userRepository, authenticationFacade)
  }

  @Nested
  inner class FindUsersByEmail {

    @Test
    fun shouldNotInvokeRepositoryWhenEmailNull(): Unit = runBlocking {
      userSearchService.findUsersByEmail(null)

      verify(userRepository, never()).findByEmailAndSourceOrderByUsername(null)
    }

    @Test
    fun shouldNotInvokeRepositoryWhenEmailEmpty(): Unit = runBlocking {
      userSearchService.findUsersByEmail("")

      verify(userRepository, never()).findByEmailAndSourceOrderByUsername("")
    }

    @Test
    fun shouldCleanEmailAndInvokeRepositoryWhenEmailPresent(): Unit = runBlocking {
      userSearchService.findUsersByEmail("  FRED@testy.co.uk  ")

      verify(userRepository).findByEmailAndSourceOrderByUsername("fred@testy.co.uk")
    }

    @Test
    fun shouldReturnFlowDataFromRepository(): Unit = runBlocking() {
      val email = "fred@testy.co.uk"
      val user = User(username = email, source = AuthSource.auth)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(email)).thenReturn(flowOf(user))

      val actualUsers = userSearchService.findUsersByEmail(email)

      assertThat(actualUsers.toList()).containsOnly(user)
    }
  }

  @Nested
  inner class FindUsersByUserName {
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

  @Nested
  inner class FindUsers {

    @Test
    fun `should generate sql filters using all parameters`(): Unit = runBlocking {
      givenLoggedInUserIsSuperUser()
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)

      val pageDetails = PageRequest.of(1, 10)

      val users = userSearchService.findUsers(
        "someName",
        listOf("someRole"),
        listOf("someGroup"),
        pageDetails,
        UserFilter.Status.ACTIVE,
      )

      val expectedFilter = UserFilter(
        name = "someName",
        roleCodes = listOf("someRole"),
        groupCodes = listOf("someGroup"),
        status = UserFilter.Status.ACTIVE,
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }

    @Test
    fun `should generate sql filter using just user name`(): Unit = runBlocking {
      givenLoggedInUserIsSuperUser()
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)
      val pageDetails = PageRequest.of(1, 10)

      val users = userSearchService.findUsers(name = "someName", roleCodes = null, groupCodes = null, pageDetails, UserFilter.Status.ALL)

      val expectedFilter = UserFilter(
        name = "someName",
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }

    @Test
    fun `should generate sql filter using just roleCodes`(): Unit = runBlocking {
      givenLoggedInUserIsSuperUser()
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)
      val pageDetails = PageRequest.of(1, 10)

      val users = userSearchService.findUsers(name = null, roleCodes = listOf("AUDIT_VIEWER"), groupCodes = null, pageDetails, UserFilter.Status.ALL)

      val expectedFilter = UserFilter(
        roleCodes = listOf("AUDIT_VIEWER"),
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }

    @Test
    fun `should generate sql using just group codes`(): Unit = runBlocking {
      givenLoggedInUserIsSuperUser()
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)
      val pageDetails = PageRequest.of(1, 10)

      val users = userSearchService.findUsers(name = null, roleCodes = null, groupCodes = listOf("INT_SP_HARMONY_LIVING"), pageDetails, UserFilter.Status.ALL)

      val expectedFilter = UserFilter(
        groupCodes = listOf("INT_SP_HARMONY_LIVING"),
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }

    @Test
    fun `adds all group manager groups if no group specified`(): Unit = runBlocking {
      givenLoggedInUserIsGroupManagerWithName("BOB")
      whenever(userGroupService.getAssignableGroups("BOB", GROUP_MANAGER)).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc"),
          Group("SITE_1_GROUP_2", "desc"),
        )
      )
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)

      val pageDetails = PageRequest.of(1, 10)
      val users = userSearchService.findUsers(name = null, roleCodes = null, groupCodes = null, pageDetails, UserFilter.Status.ALL)

      val expectedFilter = UserFilter(
        groupCodes = listOf("SITE_1_GROUP_1", "SITE_1_GROUP_2"),
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }

    @Test
    fun `filters out groups that group manager isn't a member of`(): Unit = runBlocking {
      givenLoggedInUserIsGroupManagerWithName("BILL")
      whenever(userGroupService.getAssignableGroups("BILL", GROUP_MANAGER)).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc"),
          Group("SITE_1_GROUP_2", "desc"),
        )
      )
      whenever(userSearchRepository.searchForUsers(any())).thenReturn(flowOf(user))
      whenever(userSearchRepository.countAllBy(any())).thenReturn(20)

      val pageDetails = PageRequest.of(1, 10)
      val users = userSearchService.findUsers(name = null, roleCodes = null, groupCodes = listOf("SITE_1_GROUP_1", "SITE_1_GROUP_3"), pageDetails, UserFilter.Status.ALL)

      val expectedFilter = UserFilter(
        groupCodes = listOf("SITE_1_GROUP_1"),
        pageable = pageDetails
      )

      verify(userSearchRepository).searchForUsers(
        org.mockito.kotlin.check {
          assertThat(it).extracting("sql").isEqualTo(expectedFilter.sql)
        }
      )

      assertThat(users).isEqualTo(PageImpl(listOf(user), pageDetails, 20))
    }
  }

  private fun givenLoggedInUserIsSuperUser(): Unit = runBlocking {
    whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
    whenever(authentication.authorities).thenReturn(SUPER_USER)
  }

  private fun givenLoggedInUserIsGroupManagerWithName(userName: String): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn(userName)
    whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
    whenever(authentication.authorities).thenReturn(GROUP_MANAGER)
  }

  private fun givenAUser() = UserDto(userId = "1234", username = "testy", email = "testy@testing.com", firstName = "testy", lastName = "McTesterson", locked = false, enabled = true, verified = true)

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
