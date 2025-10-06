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
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
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
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
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
  private val userRoleService: UserRoleService = mock()

  private val userId = UUID.randomUUID()
  private val user = User("testy", AuthSource.auth)
  private val service = UserGroupService(
    userRepository, groupRepository, maintainUserCheck, telemetryClient,
    authenticationFacade, roleRepository, childGroupRepository, userGroupRepository, userRoleService,
  )

  @Nested
  inner class GetParentGroups {

    @BeforeEach
    fun setRoles(): Unit = runBlocking {
      givenRolesForUser(user.getUserName(), SUPER_USER)
    }

    @Test
    fun shouldFailWhenUserNotFound(): Unit = runBlocking {
      whenever(userRepository.findById(userId)).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          service.getParentGroups(userId)
        }
      }.isInstanceOf(UsernameNotFoundException::class.java)
        .hasMessage("User $userId not found")
    }

    @Test
    fun shouldFailWhenUserFailsSecurityCheck(): Unit = runBlocking {
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)).doThrow(UserGroupRelationshipException(user.name, "User not with your groups"))

      assertThatThrownBy {
        runBlocking {
          service.getParentGroups(userId)
        }
      }.isInstanceOf(UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: ${user.name} with reason: User not with your groups")
    }

    @Test
    fun shouldReturnParentGroups(): Unit = runBlocking {
      val group1 = Group("GROUP_1", "First Group")
      val group2 = Group("GROUP_2", "Second Group")

      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(group1, group2))

      val actualGroups = service.getParentGroups(userId)

      assertThat(actualGroups).containsOnly(group1, group2)
      verify(maintainUserCheck).ensureUserLoggedInUserRelationship(user.name)
    }

    @Test
    fun shouldNotCheckUserGroupRelationUserWithViewGroupRoleWhenParentGroups(): Unit = runBlocking {
      givenViewGroupsRoleForUser(user.name)
      val group1 = Group("GROUP_1", "First Group")
      val group2 = Group("GROUP_2", "Second Group")

      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(group1, group2))

      val actualGroups = service.getParentGroups(userId)

      assertThat(actualGroups).containsOnly(group1, group2)
      verify(maintainUserCheck, never()).ensureUserLoggedInUserRelationship(user.name)
    }
  }

  @Nested
  inner class GetAllGroupsUsingChildGroupsInLieuOfParentGroup {

    @BeforeEach
    fun setRoles(): Unit = runBlocking {
      givenRolesForUser(user.getUserName(), SUPER_USER)
    }

    @Test
    fun shouldFailWhenUserNotFound(): Unit = runBlocking {
      whenever(userRepository.findById(userId)).thenReturn(null)

      assertThatThrownBy {
        runBlocking {
          service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)
        }
      }.isInstanceOf(UsernameNotFoundException::class.java)
        .hasMessage("User $userId not found")
    }

    @Test
    fun shouldFailWhenUserFailsSecurityCheck(): Unit = runBlocking {
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)).doThrow(
        UserGroupRelationshipException(
          user.name,
          "User not with your groups",
        ),
      )

      assertThatThrownBy {
        runBlocking {
          service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)
        }
      }.isInstanceOf(UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: ${user.name} with reason: User not with your groups")
    }

    @Test
    fun shouldReturnEmptyCollectionWhenNoParentGroups(): Unit = runBlocking {
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf())

      val actualGroups = service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)

      assertThat(actualGroups).isEmpty()
      verify(maintainUserCheck).ensureUserLoggedInUserRelationship(user.name)
    }

    @Test
    fun shouldReturnParentGroupWhenNoChildGroups(): Unit = runBlocking {
      val group1 = Group("GROUP_1", "First Group")
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(group1))
      whenever(childGroupRepository.findAllByGroup(group1.groupId)).thenReturn(flowOf())

      val actualGroups = service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)
      assertThat(actualGroups).containsOnly(group1)
    }

    @Test
    fun shouldReturnChildGroupsWhenPresentInLieuOfParentGroup(): Unit = runBlocking {
      val group1 = Group("GROUP_1", "First Group")
      val childGroup1 =
        ChildGroup(groupCode = "CHILD_GROUP_1", groupName = "First Child Group", group = UUID.randomUUID())
      val childGroup2 =
        ChildGroup(groupCode = "CHILD_GROUP_2", groupName = "Second Child Group", group = UUID.randomUUID())
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf(group1))
      whenever(childGroupRepository.findAllByGroup(group1.groupId)).thenReturn(flowOf(childGroup1, childGroup2))

      val actualGroups = service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)

      assertThat(actualGroups).containsOnly(childGroup1, childGroup2)
    }

    @Test
    fun shouldNotCheckUserGroupRelationUserWithViewGroupsRoleWhenGroupsFetched(): Unit = runBlocking {
      givenViewGroupsRoleForUser(user.name)
      whenever(userRepository.findById(userId)).thenReturn(user)
      whenever(groupRepository.findGroupsByUserId(userId)).thenReturn(flowOf())

      val actualGroups = service.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)

      assertThat(actualGroups).isEmpty()
      verify(maintainUserCheck, never()).ensureUserLoggedInUserRelationship(user.name)
    }
  }

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
            listOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")),
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
        null,
      )
    }

    @Test
    fun removeGroup_success_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("GROUP_LICENCE_VARY", "desc2", UUID.randomUUID()),
        ),
      )

      whenever(userRepository.findByUsernameAndSource("user", AuthSource.auth)).thenReturn(user)
      val manager = createSampleUser(username = "managerUser")
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)
      whenever(userGroupRepository.deleteUserGroup(any(), any())).thenReturn(1)

      service.removeUserGroup("user", "  group_licence_vary   ", "MANAGER", GROUP_MANAGER_ROLE)
      verify(telemetryClient).trackEvent(
        "ExternalUserGroupRemoveSuccess",
        mapOf("username" to "user", "group" to "group_licence_vary", "admin" to "MANAGER"),
        null,
      )
    }

    @Test
    fun removeGroup_failure_lastgroup_groupManager(): Unit = runBlocking {
      val user = createSampleUser(username = "user", id = UUID.randomUUID())
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(Group("GROUP_LICENCE_VARY", "desc2")),
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
  }

  @Nested
  inner class GetAssignableGroups {
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
          Group("LICENCE_VARY", "desc2"),
        ),
      )

      val groups = service.getAssignableGroups(" BOB ", setOf())
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun userAssignableGroups_superUser(): Unit = runBlocking {
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2"),
        ),
      )
      val groups = service.getAssignableGroups(" BOB ", setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun myAssignableGroups_notAdminAndNoUser(): Unit = runBlocking {
      givenRolesForUser("BOB", setOf())
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(null)

      val groups = service.getMyAssignableGroups()
      assertThat(groups).isEmpty()
    }

    @Test
    fun myAssignableGroups_normalUser(): Unit = runBlocking {
      givenRolesForUser("BOB", setOf())

      val user = createSampleUser(username = "BOB")
      whenever(userRepository.findByUsernameAndSource(anyString(), anyOrNull())).thenReturn(user)
      whenever(groupRepository.findGroupsByUsername(any())).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2"),
        ),
      )

      val groups = service.getMyAssignableGroups()
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun myAssignableGroups_superUser(): Unit = runBlocking {
      givenSuperUserRoleForUser("BOB")
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("JOE", "desc"),
          Group("LICENCE_VARY", "desc2"),
        ),
      )

      val groups = service.getMyAssignableGroups()
      assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
    }

    @Test
    fun myAssignableGroups_contractManager(): Unit = runBlocking {
      givenRolesForUser("BOB", setOf(SimpleGrantedAuthority("ROLE_CONTRACT_MANAGER_VIEW_GROUP")))
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
        flowOf(
          Group("NOT_CRS_1", "desc"),
          Group("INT_CR_PRJ_CRS_1", "desc2"),
          Group("INT_CR_PRJ_CRS_2", "desc3"),
          Group("NOT_CRS_2", "desc4"),
        ),
      )

      val groups = service.getMyAssignableGroups()
      assertThat(groups).extracting<String> { it.groupCode }.contains("INT_CR_PRJ_CRS_1", "INT_CR_PRJ_CRS_2")
    }
  }

  @Nested
  inner class RemoveGroupByUserId {

    @Test
    fun groupNotOnUser(): Unit = runBlocking {
      givenSuperUserRoleForUser("admin")

      val groupId1 = UUID.randomUUID()
      val groupId2 = UUID.randomUUID()
      val group = flowOf(
        Group("JOE", "desc", groupId1),
        Group("LICENCE_VARY", "desc2", groupId2),
      )
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(
        group,
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
      givenSuperUserRoleForUser("admin")

      whenever(userGroupRepository.deleteUserGroup(anyOrNull(), anyOrNull())).thenReturn(0)
      val groupId1 = UUID.randomUUID()
      val groupId2 = UUID.randomUUID()
      val group = flowOf(
        Group("JOE", "desc", groupId1),
        Group("LICENCE_VARY", "desc2", groupId2),
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
      givenGroupManagerRoleForUser()

      val groupId2 = UUID.randomUUID()
      val groups = flowOf(
        Group("JOE", "desc", UUID.randomUUID()),
        Group("GROUP_LICENCE_VARY", "desc2", groupId2),
      )
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(groups)
      whenever(groupRepository.findGroupsByUsername("MANAGER")).thenReturn(groups)

      val userId = UUID.randomUUID()
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(userId)).thenReturn(user)
      val manager = createSampleUser(
        username = "user",
      )
      whenever(userRepository.findByUsernameAndSource("MANAGER", AuthSource.auth)).thenReturn(manager)
      whenever(userGroupRepository.deleteUserGroup(anyOrNull(), anyOrNull())).thenReturn(1)

      service.removeGroupByUserId(userId, "  group_licence_vary   ")
      verify(userGroupRepository).deleteUserGroup(userId, groupId2)
    }

    @Test
    fun failureAsGroupManagerLastGroup(): Unit = runBlocking {
      givenGroupManagerRoleForUser()

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
            "        ",
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: notfound")
    }

    @Test
    fun groupAlreadyOnUser(): Unit = runBlocking {
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(group))
      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(
            UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
            "LICENCE_VARY",
          )
        }
      }.isInstanceOf(UserGroupException::class.java)
        .hasMessage("Add group failed for field group with reason: exists")
    }

    @Test
    fun `Add Group By User Id success`(): Unit = runBlocking {
      givenSuperUserRoleForUser("MANAGER")

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val group1 = Group("GROUP_LICENCE_VARY_1", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(group1))
      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      val approveCategory = Authority(UUID.randomUUID(), "APPROVE_CATEGORISATION", "Approve Category assessments", "", "")

      whenever(roleRepository.findAutomaticGroupRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence, approveCategory))
      whenever(userRoleService.getUserRoles(anyOrNull())).thenReturn(listOf(approveCategory))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      service.addGroupByUserId(userId, "GROUP_LICENCE_VARY")
      verify(userRoleService).addRolesByUserId(userId, listOf("ROLE_LICENCE_VARY"))
      verify(groupRepository).findGroupsByUserId(userId)
      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "username" to "user", "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(
        eq("ExternalUserGroupAddSuccess"),
        eq(expectedTelemetryDetails),
        isNull(),
      )
    }

    @Test
    fun `No roles to add for a Group By User Id , if roles already exists`(): Unit = runBlocking {
      givenSuperUserRoleForUser("MANAGER")

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(anyOrNull())).thenReturn(user)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val group1 = Group("GROUP_LICENCE_VARY_1", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(group1))
      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      val approveCategory = Authority(UUID.randomUUID(), "APPROVE_CATEGORISATION", "Approve Category assessments", "", "")

      // Total roles for a group code
      whenever(roleRepository.findAutomaticGroupRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence, approveCategory))

      // existing roles
      whenever(userRoleService.getUserRoles(anyOrNull())).thenReturn(listOf(roleLicence, approveCategory))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      service.addGroupByUserId(userId, "GROUP_LICENCE_VARY")
      verify(userRoleService).addRolesByUserId(userId, emptyList())
      verify(groupRepository).findGroupsByUserId(userId)
      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "username" to "user", "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(
        eq("ExternalUserGroupAddSuccess"),
        eq(expectedTelemetryDetails),
        isNull(),
      )
    }

    @Test
    fun successAsGroupManager(): Unit = runBlocking {
      givenGroupManagerRoleForUser()

      val userId = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(userId)).thenReturn(user)

      val manager = createSampleUser(
        username = "user",
      )
      whenever(userRepository.findByUsernameAndSource("MANAGER")).thenReturn(manager)

      val group = Group("GROUP_LICENCE_VARY", "desc")
      val groupJoe = Group("GROUP_JOE", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(groupJoe))
      whenever(groupRepository.findGroupsByUsername(anyOrNull())).thenReturn(flowOf(group, groupJoe))
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)

      val roleLicence = Authority(UUID.randomUUID(), "ROLE_LICENCE_VARY", "Role Licence Vary", "", "")
      val approveCategory = Authority(UUID.randomUUID(), "APPROVE_CATEGORISATION", "Approve Category assessments", "", "")

      whenever(roleRepository.findAutomaticGroupRolesByGroupCode(anyString())).thenReturn(flowOf(roleLicence, approveCategory))
      whenever(userRoleService.getUserRoles(anyOrNull())).thenReturn(listOf(approveCategory))

      service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")

      verify(groupRepository).findGroupsByUserId(userId)
      verify(userRoleService).addRolesByUserId(userId, listOf("ROLE_LICENCE_VARY"))

      val expectedTelemetryDetails = mapOf("userId" to userId.toString(), "username" to "user", "group" to "GROUP_LICENCE_VARY", "admin" to "MANAGER")
      verify(telemetryClient).trackEvent(eq("ExternalUserGroupAddSuccess"), eq(expectedTelemetryDetails), isNull())
    }

    @Test
    fun failureWhenGroupManagerNotMemberOfGroup(): Unit = runBlocking {
      givenGroupManagerRoleForUser()

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(user)
      val manager = createSampleUser(username = "user")
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
      givenGroupManagerRoleForUser()

      val user = createSampleUser(username = "user")
      whenever(userRepository.findById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))).thenReturn(user)
      val manager = createSampleUser(username = "user")
      whenever(userRepository.findByUsernameAndSource("MANAGER")).thenReturn(manager)
      val group = Group("GROUP_LICENCE_VARY", "desc")
      val groupJoe = Group("GROUP_JOE", "desc 2")
      whenever(groupRepository.findGroupsByUserId(anyOrNull())).thenReturn(flowOf(groupJoe))
      whenever(groupRepository.findGroupsByUsername(anyOrNull())).thenReturn(flowOf(group, groupJoe))

      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(group)
      doThrow(UserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(anyString())

      assertThatThrownBy {
        runBlocking {
          service.addGroupByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "GROUP_LICENCE_VARY")
        }
      }.isInstanceOf(UserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }
  }

  private fun givenSuperUserRoleForUser(username: String): Unit = runBlocking {
    givenRolesForUser(username, SUPER_USER)
  }

  private fun givenGroupManagerRoleForUser(): Unit = runBlocking {
    givenRolesForUser("MANAGER", GROUP_MANAGER_ROLE)
  }

  private fun givenViewGroupsRoleForUser(username: String): Unit = runBlocking {
    givenRolesForUser(username, VIEW_USER_GROUPS)
  }

  private fun givenRolesForUser(username: String, authorities: Set<GrantedAuthority>): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn(username)
    whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
    whenever(authentication.authorities).thenReturn(authorities)
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
    private val VIEW_USER_GROUPS: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_VIEW_USER_GROUPS"))
  }
}
