package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import java.util.UUID

class UserGroupServiceTest {
  private val userRepository: UserRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val userService: UserService = mock()
  private val service = UserGroupService(userRepository, groupRepository, maintainUserCheck, telemetryClient, authenticationFacade, userService)

  @Nested
  inner class RemoveGroup {
    @Test
    fun removeGroup_groupNotOnUser(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.just(user))
      assertThatThrownBy {
        runBlocking {
          service.removeGroup(
            "user",
            "BOB",
            "admin",
            listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: missing")
    }

    @Test
    fun removeGroup_success(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.just(user))
      service.removeGroup("user", "  licence_vary   ", "admin", SUPER_USER)
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
      verify(telemetryClient).trackEvent(
        "ExternalUserGroupRemoveSuccess",
        mapOf("username" to "user", "group" to "licence_vary", "admin" to "admin"),
        null
      )
    }

    @Test
    fun removeGroup_success_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
      whenever(userRepository.findByUsernameAndSource("user", AuthSource.auth)).thenReturn(Mono.just(user))
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("GROUP_LICENCE_VARY", "desc2")
        )
      )
      whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(Mono.just(manager))
      service.removeGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
    }

    @Test
    fun removeGroup_failure_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
      whenever(userRepository.findByUsernameAndSource("user", AuthSource.auth)).thenReturn(Mono.just(user))
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("GROUP_LICENCE_VARY", "desc2")
        )
      )
      whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(Mono.just(manager))
      service.removeGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
    }

    @Test
    fun userAssignableGroups_notAdminAndNoUser(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.empty())
      val groups = service.getAssignableGroups(" BOB ", setOf())
      assertThat(groups).isEmpty()
    }

    @Test
    fun userAssignableGroups_normalUser(): Unit = runBlocking {
      val user = createSampleUser(username = "user", groups = setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
      whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.just(user))
      val groups = service.getAssignableGroups(" BOB ", setOf())
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun userAssignableGroups_superUser(): Unit = runBlocking {
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2")
        )
      )
      val groups = service.getAssignableGroups(" BOB ", setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Nested
    inner class GetGroups {
      @Test
      fun groups_success(): Unit = runBlocking {
        whenever(authenticationFacade.getUsername()).thenReturn("admin")
        whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
        whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))

        val id = UUID.randomUUID()
        val user =
          createSampleUser(username = "user", groups = setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
        whenever(
          maintainUserCheck
            .ensureUserLoggedInUserRelationship(
              anyString(),
              org.mockito.kotlin.any(),
              org.mockito.kotlin.any()
            )
        ).thenReturn(user)
        whenever(userRepository.findById(id)).thenReturn(user)
        val groups = service.getGroups(id)
        assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
      }

      @Test
      fun groups_user_notfound(): Unit = runBlocking {
        val id = UUID.randomUUID()
        whenever(userRepository.findById(id)).thenReturn(null)
        val groups = service.getGroups(id)
        assertThat(groups).isNull()
      }

      @Test
      fun groups_by_username_success(): Unit = runBlocking {
        val user =
          createSampleUser(username = "user", groups = setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
        whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
        whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.just(user))
        val groups = service.getGroupsByUserName(" BOB ")
        assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
      }

      @Test
      fun groups_by_username_notfound(): Unit = runBlocking {
        whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(Mono.empty())
        val groups = service.getGroupsByUserName(" BOB ")
        assertThat(groups).isNull()
      }
    }
  }

  @Nested
  inner class RemoveGroupByUserId {

    @Test
    fun groupNotOnUser(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      assertThatThrownBy {
        runBlocking { service.removeGroupByUserId(UUID.randomUUID(), "BOB") }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: missing")
    }

    @Test
    fun success(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER)

      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      runBlocking { service.removeGroupByUserId(UUID.randomUUID(), "  licence_vary   ") }
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
    }

    @Test
    fun successAsGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
      whenever(userRepository.findById(userId)).thenReturn(user)
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(Mono.just(manager))
      service.removeGroupByUserId(userId, "  group_licence_vary   ")
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
    }

    @Test
    fun failureAsGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      user.groups.addAll(setOf(Group("JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc2")))
      whenever(userRepository.findById(userId)).thenReturn(user)
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(userService.getUserAndGroupByUserName(anyString())).thenReturn(user)
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(Mono.just(manager))
      service.removeGroupByUserId(userId, "  group_licence_vary   ")
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
    }
  }

 /* TODO
 @Nested
  inner class AddGroupByUserId {

    @Test
    fun blankGroupCode() : Unit = runBlocking {
      whenever(userRepository.findById(any())).thenReturn(createSampleUser(username = "user"))
      assertThatThrownBy {runBlocking {
        service.addGroupByUserId(
          UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
          "        "
        )}
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: notfound")
    }

    @Test
    fun groupAlreadyOnUser() : Unit = runBlocking {
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val user = createSampleUser(username = "user", groups = setOf(group))
      whenever(userRepository.findById(any())).thenReturn(Mono.just(user))
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      assertThatThrownBy {
        : Unit = runBlocking {
        service.addGroupByUserId(
          UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
          "LICENCE_VARY"
        )
      }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: exists")
    }

    @Test
    fun success() : Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER)

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(any())).thenReturn(Mono.just(user))
      val group = Group("GROUP_LICENCE_VARY", "desc")

      val roleLicence = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val roleJoe = Authority("JOE", "Role Joe")
      group.assignableRoles.addAll(
        setOf(
          GroupAssignableRole(roleLicence, group, true),
          GroupAssignableRole(roleJoe, group, false)
        )
      )
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      service.addGroupByUserId(userId, "GROUP_LICENCE_VARY")
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("GROUP_JOE", "GROUP_LICENCE_VARY")
      assertThat(user.authorities).extracting<String> { it.roleCode }.containsOnly("LICENCE_VARY")

      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(eq("AuthUserGroupAddSuccess"), eq(expectedTelemetryDetails), isNull())
    }

    @Test
    fun successAsGroupManager() : Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(userId)).thenReturn(Mono.just(user))
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(userRepository.findByUsername("MANAGER")).thenReturn(Mono.just(manager))
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val roleLicence = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val roleJoe = Authority("JOE", "Role Joe")
      group.assignableRoles.addAll(
        setOf(
          GroupAssignableRole(roleLicence, group, true),
          GroupAssignableRole(roleJoe, group, false)
        )
      )
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
      assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("GROUP_JOE", "GROUP_LICENCE_VARY")
      assertThat(user.authorities).extracting<String> { it.roleCode }.containsOnly("LICENCE_VARY")

      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(eq("AuthUserGroupAddSuccess"), eq(expectedTelemetryDetails), isNull())
    }

    @Test
    fun failureWhenGroupManagerNotMemberOfGroup() : Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(Mono.just(user))
      val manager = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findByUsername("MANAGER")).thenReturn(Mono.just(manager))
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val roleLicence = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val roleJoe = Authority("JOE", "Role Joe")
      group.assignableRoles.addAll(
        setOf(
          GroupAssignableRole(roleLicence, group, true),
          GroupAssignableRole(roleJoe, group, false)
        )
      )
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      assertThatThrownBy {
        service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
      }.isInstanceOf(UserGroupManagerException::class.java)
        .hasMessage("Add group failed for field group with reason: managerNotMember")
    }

    @Test
    fun failureWhenGroupManagerNotAllowedToMaintainUser() : Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(Mono.just(user))
      val manager = createSampleUser(username = "user", groups = setOf(Group("GROUP_LICENCE_VARY", "desc")))
      whenever(userRepository.findByUsername("MANAGER")).thenReturn(Mono.just(manager))
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val roleLicence = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val roleJoe = Authority("JOE", "Role Joe")
      group.assignableRoles.addAll(
        setOf(
          GroupAssignableRole(roleLicence, group, true),
          GroupAssignableRole(roleJoe, group, false)
        )
      )
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      doThrow(MaintainUserCheck.UserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(
          anyString(),
          org.mockito.kotlin.any(),
          org.mockito.kotlin.any()
        )

      assertThatThrownBy {
        service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
      }.isInstanceOf(MaintainUserCheck.UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }
  }*/

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
