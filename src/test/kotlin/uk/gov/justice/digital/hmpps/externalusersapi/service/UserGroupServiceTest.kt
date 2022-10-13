package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import java.util.Optional

class UserGroupServiceTest {
  private val userRepository: UserRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val service = UserGroupService(userRepository, groupRepository, telemetryClient, authenticationFacade)

  @Test
  fun removeGroup_groupNotOnUser() {
    val user = createSampleUser(username = "user")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThatThrownBy {
      service.removeGroup(
        "user",
        "BOB",
        "admin",
        listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
      )
    }.isInstanceOf(UserGroupException::class.java)
      .hasMessage("Add group failed for field group with reason: missing")
  }

  @Test
  fun removeGroup_success() {
    val user = createSampleUser(username = "user")
    user.groups.addAll(setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    service.removeGroup("user", "  licence_vary   ", "admin", SUPER_USER)
    assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
  }

  @Test
  fun removeGroup_success_groupManager() {
    val user = createSampleUser(username = "user")
    user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsername("user")).thenReturn(Optional.of(user))
    val manager = createSampleUser(
      username = "user",
      groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
    )
    whenever(userRepository.findByUsername("MANAGER")).thenReturn(Optional.of(manager))
    service.removeGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
    assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
  }

  @Test
  fun removeGroup_failure_groupManager() {
    val user = createSampleUser(username = "user")
    user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsername("user")).thenReturn(Optional.of(user))
    val manager = createSampleUser(
      username = "user",
      groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
    )
    whenever(userRepository.findByUsername("MANAGER")).thenReturn(Optional.of(manager))
    service.removeGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
    assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
  }

  @Test
  fun authUserGroups_notfound() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    val groups = service.getAuthUserGroups(" BOB ")
    assertThat(groups).isNull()
  }

  @Test
  fun authUserAssignableGroups_notAdminAndNoUser() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    val groups = service.getAssignableGroups(" BOB ", setOf())
    assertThat(groups).isEmpty()
  }

  @Test
  fun authUserGroups_success() {
    val user = createSampleUser(username = "user", groups = setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val groups = service.getAuthUserGroups(" BOB ")
    assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }

  @Test
  fun authUserAssignableGroups_normalUser() {
    val user = createSampleUser(username = "user", groups = setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val groups = service.getAssignableGroups(" BOB ", setOf())
    assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }

  @Test
  fun authUserAssignableGroups_superUser() {
    whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
      listOf(
        Group("JOE", "desc"),
        Group("LICENCE_VARY", "desc2")
      )
    )
    val groups = service.getAssignableGroups(" BOB ", setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
    assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
