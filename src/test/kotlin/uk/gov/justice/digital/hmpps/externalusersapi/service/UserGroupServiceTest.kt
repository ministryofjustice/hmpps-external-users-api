package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.util.UUID

class UserGroupServiceTest {
  private val userRepository: UserRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val roleRepository: RoleRepository = mock()
  private val childGroupRepository: ChildGroupRepository = mock()
  private val userGroupRepository: UserGroupRepository = mock()
  private val service = UserGroupService(userRepository, groupRepository, maintainUserCheck, telemetryClient, authenticationFacade, roleRepository, childGroupRepository, userGroupRepository)

  @Nested
  inner class RemoveGroup {
    @Test
    fun removeGroup_groupNotOnUser(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(flowOf())

      assertThatThrownBy {
        runBlocking {
          service.removeUserGroup(
            "user",
            "BOB",
            "admin",
            listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Remove group failed for field group with reason: missing")
    }

    @Test
    fun removeGroup_success(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      val dbGroup1 = Group("JOE", "desc")
      val dbGroup2 = Group("LICENCE_VARY", "desc2", UUID.randomUUID())

      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(flowOf(dbGroup1, dbGroup2))
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      whenever(userGroupRepository.deleteUserGroup(any(), any())).thenReturn(1)

      service.removeUserGroup("user", "  licence_vary   ", "admin", SUPER_USER)
      verify(telemetryClient).trackEvent(
        "ExternalUserGroupRemoveSuccess",
        mapOf("username" to "user", "group" to "licence_vary", "admin" to "admin"),
        null
      )
    }

    @Test
    fun removeGroup_success_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("GROUP_LICENCE_VARY", "desc2", UUID.randomUUID())
        )
      )

      whenever(userRepository.findByUsernameAndSource("user", AuthSource.auth)).thenReturn(user)
      val manager = createSampleUser(username = "managerUser")
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)
      whenever(userGroupRepository.deleteUserGroup(any(), any())).thenReturn(1)

      service.removeUserGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
      verify(telemetryClient).trackEvent(
        "ExternalUserGroupRemoveSuccess",
        mapOf("username" to "user", "group" to "group_licence_vary", "admin" to "MANAGER"),
        null
      )
    }

    @Test
    fun removeGroup_failure_lastgroup_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(Group("GROUP_LICENCE_VARY", "desc2"))
      )
      whenever(userRepository.findByUsernameAndSource("user", AuthSource.auth)).thenReturn(user)

      val manager = createSampleUser(username = "managerUser")
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)
      whenever(userGroupRepository.deleteUserGroup(any(), any())).thenReturn(1)

      assertThatThrownBy {
        runBlocking {
          service.removeUserGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
        }
      }.isInstanceOf(UserLastGroupException::class.java)
        .hasMessage("remove group failed for field group with reason: last")

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun userAssignableGroups_notAdminAndNoUser(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)

      val groups = service.getAssignableGroups(" BOB ", setOf())
      assertThat(groups).isEmpty()
    }

    @Test
    fun userAssignableGroups_normalUser(): Unit = runBlocking {
      val user = createSampleUser(username = "user")
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2")
        )
      )

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

      whenever(userRepository.findById(id)).thenReturn(user)

      val childGroup = flowOf(ChildGroup("CG", "disc", UUID.randomUUID()))
      whenever(childGroupRepository.findAllByGroup(anyOrNull())).thenReturn(childGroup)
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("GROUP_LICENCE_VARY", "desc2")
        )
      )

      val groups = service.getGroups(id)
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "GROUP_LICENCE_VARY")
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
      val user = createSampleUser(username = "user")
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      whenever(groupRepository.findGroupsByUsername(anyString())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2")
        )
      )

      val groups = service.getGroupsByUserName(" BOB ")
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun groups_by_username_notfound(): Unit = runBlocking {
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)

      val groups = service.getGroupsByUserName(" BOB ")
      assertThat(groups).isNull()
    }
  }

  @Nested
  inner class RemoveGroupByUserId {

    @Test
    fun groupNotOnUser(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
      val groupId1 = UUID.randomUUID()
      val groupId2 = UUID.randomUUID()
      val group = flowOf(
        Group("JOE", "desc", groupId1), Group("LICENCE_VARY", "desc2", groupId2)
      )
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(
        group
      )

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      assertThatThrownBy {
        runBlocking { service.removeGroupByUserId(UUID.randomUUID(), "BOB") }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Remove group failed for field group with reason: missing")
    }

    @Test
    fun success(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("admin")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER)
      whenever(userGroupRepository.deleteUserGroup(anyOrNull(), anyOrNull())).thenReturn(0)
      val groupId1 = UUID.randomUUID()
      val groupId2 = UUID.randomUUID()
      val group = flowOf(
        Group("JOE", "desc", groupId1), Group("LICENCE_VARY", "desc2", groupId2)
      )
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(group)
      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)

      runBlocking { service.removeGroupByUserId(userId, "  licence_vary   ") }
      verify(userGroupRepository).deleteUserGroup(userId, groupId2)
    }

    @Test
    fun successAsGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)
      val groupId2 = UUID.randomUUID()
      val groups = flowOf(
        Group("JOE", "desc", UUID.randomUUID()), Group("GROUP_LICENCE_VARY", "desc2", groupId2)
      )
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(groups)
      whenever(groupRepository.findGroupsByUsername("MANAGER")).thenReturn(groups)

      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(userId)).thenReturn(user)
      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc"), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)
      whenever(userGroupRepository.deleteUserGroup(anyOrNull(), anyOrNull())).thenReturn(1)

      service.removeGroupByUserId(userId, "  group_licence_vary   ")
      verify(userGroupRepository).deleteUserGroup(userId, groupId2)
    }

    @Test
    fun failureAsGroupManagerLastGroup(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val groups = flowOf(Group("GROUP_LICENCE_VARY", "desc2"))
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(groups)
      whenever(groupRepository.findGroupsByUsername("MANAGER")).thenReturn(groups)

      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(userId)).thenReturn(user)
      val manager = createSampleUser(username = "managerUser")
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)

      assertThatThrownBy {
        runBlocking {
          service.removeGroupByUserId(userId, "  group_licence_vary   ")
        }
      }.isInstanceOf(UserLastGroupException::class.java)
        .hasMessage("remove group failed for field group with reason: last")

      verifyNoInteractions(userGroupRepository)
    }
  }

  @Nested
  inner class AddGroupByUserId {

    @Test
    fun blankGroupCode(): Unit = runBlocking {
      whenever(userRepository.findById(anyOrNull())).thenReturn(createSampleUser(username = "user"))
      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(
            UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            "        "
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: notfound")
    }

    @Test
    fun groupAlreadyOnUser(): Unit = runBlocking {
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val user = createSampleUser(username = "user", groups = setOf(group))
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(group))
      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(
            UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            "LICENCE_VARY"
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: exists")
    }

    @Test
    fun success(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(SUPER_USER)

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val group1 = Group("GROUP_LICENCE_VARY_1", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(group1))
      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      whenever(roleRepository.findRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      service.addGroupByUserId(userId, "GROUP_LICENCE_VARY")

      verify(groupRepository).findGroupsByUserId(userId)
      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(
        eq("AuthUserGroupAddSuccess"),
        eq(expectedTelemetryDetails), isNull()
      )
    }

    @Test
    fun successAsGroupManager(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(userId)).thenReturn(user)

      val manager = createSampleUser(
        username = "user",
        groups = setOf(Group("GROUP_JOE", "desc", groupId = UUID.randomUUID()), Group("GROUP_LICENCE_VARY", "desc"))
      )
      whenever(userRepository.findByUsernameAndSource("MANAGER")).thenReturn(manager)

      val group = Group("GROUP_LICENCE_VARY", "desc")
      val groupJoe = Group("GROUP_JOE", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(groupJoe))
      whenever(groupRepository.findGroupsByUsername(anyOrNull())).thenReturn(flowOf(group, groupJoe))
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)

      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      whenever(roleRepository.findRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence))

      service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")

      verify(groupRepository).findGroupsByUserId(userId)

      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(eq("AuthUserGroupAddSuccess"), eq(expectedTelemetryDetails), isNull())
    }

    @Test
    fun failureWhenGroupManagerNotMemberOfGroup(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val user = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(user)
      val manager = createSampleUser(username = "user", groups = setOf(Group("GROUP_JOE", "desc")))
      whenever(userRepository.findByUsernameAndSource("MANAGER")).thenReturn(manager)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val groupJoe = Group("GROUP_JOE", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(groupJoe))
      whenever(groupRepository.findGroupsByUsername(anyOrNull())).thenReturn(flowOf(groupJoe))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)

      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
        }
      }.isInstanceOf(UserGroupManagerException::class.java)
        .hasMessage("Add group failed for field group with reason: managerNotMember")
    }

    @Test
    fun failureWhenGroupManagerNotAllowedToMaintainUser(): Unit = runBlocking {
      whenever(authenticationFacade.getUsername()).thenReturn("MANAGER")
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(GROUP_MANAGER_ROLE)

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(user)
      val manager = createSampleUser(username = "user", groups = setOf(Group("GROUP_LICENCE_VARY", "desc")))
      whenever(userRepository.findByUsernameAndSource("MANAGER")).thenReturn(manager)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val groupJoe = Group("GROUP_JOE", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(groupJoe))
      whenever(groupRepository.findGroupsByUsername(anyOrNull())).thenReturn(flowOf(group, groupJoe))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      doThrow(UserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(
          anyString(),
          any(),
          any()
        )

      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
        }
      }.isInstanceOf(UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
