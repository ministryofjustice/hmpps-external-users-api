package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserGroupCoroutineRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRoleCoroutineRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateUser
import java.util.*

class CreateUserServiceTest {

  private var userRepository: UserRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userGroupService: UserGroupService = mock()
  private var roleRepository: RoleRepository = mock()
  private var userGroupCoroutineRepository: UserGroupCoroutineRepository = mock()
  private val authentication: Authentication = mock()
  private var userRoleCoroutineRepository: UserRoleCoroutineRepository = mock()

  private val createUserService = CreateUserService(userRepository, authenticationFacade, telemetryClient, userGroupService, roleRepository, userGroupCoroutineRepository, userRoleCoroutineRepository)

  @Nested
  inner class CreateExternalUser {

    val user = UserHelper.createSampleUser(username = "email@email.com", id = UUID.randomUUID())
    private var createUser = CreateUser("email@email.com", "first_name", "last_name", setOf(""))
    private val groupManager: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"), SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))

    @BeforeEach
    fun initSecurityContext(): Unit = runBlocking {
      MockitoAnnotations.openMocks(this)
      whenever(authenticationFacade.getUsername()).thenReturn("adminuser")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authenticationFacade.getAuthentication()!!.authorities).thenReturn(groupManager)
    }

    @Test
    fun `create User fails By UserName_Exists`(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      assertThatThrownBy {
        runBlocking {
          createUserService.createUserByEmail(createUser)
        }
      }.isInstanceOf(UserExistsException::class.java)
        .hasMessage("User already exists, for field userId with reason: User with username email@email.com already exists")
    }

    @Test
    fun `create User fails By Email_Exists`(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), anyOrNull())).thenReturn(flowOf(user))
      assertThatThrownBy {
        runBlocking {
          createUserService.createUserByEmail(createUser)
        }
      }.isInstanceOf(UserExistsException::class.java)
        .hasMessage("User already exists, for field email with reason: Email with username email@email.com already exists")
    }

    @Test
    fun `create User fails when group code doesn't exists`(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), anyOrNull())).thenReturn(emptyFlow())

      whenever(userGroupService.getAssignableGroups(anyString(), anyOrNull())).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc"),
          Group("SITE_1_GROUP_2", "desc"),
        ),
      )
      assertThatThrownBy {
        runBlocking {
          createUserService.createUserByEmail(createUser)
        }
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field groupCode with reason: notfound")
    }

    @Test
    fun `createUserByEmail fails if group is missing`(): Unit = runBlocking {
      createUser = CreateUser("email@email.com", "first_name", "last_name", null)
      whenever(authenticationFacade.getAuthentication()!!.authorities).thenReturn(emptySet())
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), anyOrNull())).thenReturn(emptyFlow())
      assertThatThrownBy {
        runBlocking {
          createUserService.createUserByEmail(createUser)
        }
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field groupCode with reason: missing")
    }

    @Test
    fun `Create external user`(): Unit = runBlocking {
      createUser = CreateUser("email@email.com", "first_name", "last_name", setOf("SITE_1_GROUP_1"))

      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)
      whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), anyOrNull())).thenReturn(emptyFlow())
      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      whenever(roleRepository.findRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence))

      val site1Group1GRPID = UUID.randomUUID()
      val site1Group2GRPID = UUID.randomUUID()

      whenever(userGroupService.getAssignableGroups(anyString(), anyOrNull())).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc", site1Group1GRPID),
          Group("SITE_1_GROUP_2", "desc", site1Group2GRPID),
        ),
      )
      val captUser = argumentCaptor<User>()
      createUserService.createUserByEmail(createUser)
      verify(userRepository).save(captUser.capture())
      assertThat(captUser.firstValue.email).isEqualTo(createUser.email)
      assertThat(captUser.firstValue.getUserName()).isEqualTo(StringUtils.upperCase(createUser.email))
      assertThat(captUser.firstValue.getFirstName()).isEqualTo(createUser.firstName)
      assertThat(captUser.firstValue.lastName).isEqualTo(createUser.lastName)
      assertThat(captUser.firstValue.isEnabled()).isTrue
    }
  }
}
