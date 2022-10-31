package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupAssignableRoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck

class GroupsServiceTest {
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val childGroupRepository: ChildGroupRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val userRepository: UserRepository = mock()
  private val userGroupService: UserGroupService = mock()
  private val authentication: Authentication = mock()
  private val groupAssignableRoleRepository: GroupAssignableRoleRepository = mock()
  private val groupsService = GroupsService(
    groupRepository,
    childGroupRepository,
    telemetryClient,
    authenticationFacade,
    userRepository,
    userGroupService,
    groupAssignableRoleRepository,
    maintainUserCheck
  )

  @BeforeEach
  fun initSecurityContext(): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn("username")
  }

  @Nested
  inner class Groups {
    @Test
    fun `get all groups`(): Unit = runBlocking {
      val dbGroup1 = Group("GROUP_1", "first group")
      val dbGroup2 = Group("GROUP_2", "second group")
      val allGroups = flowOf(dbGroup1, dbGroup2)
      whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(allGroups)

      val actualGroups = groupsService.allGroups
      assertThat(actualGroups).isEqualTo(allGroups.toList())
      verify(groupRepository).findAllByOrderByGroupName()
    }
  }

 /* @Nested
  inner class DeleteChildGroup {

    @Test
    fun `Delete child group`() : Unit = runBlocking {
      val childGroup = ChildGroup("CG", "disc")
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(childGroup)

      groupsService.deleteChildGroup("CG")
      verify(childGroupRepository).delete(childGroup)
      verify(telemetryClient).trackEvent(
        "GroupChildDeleteSuccess",
        mapOf("username" to "username", "childGroupCode" to "CG"),
        null
      )
    }

    @Test
    fun `Child Group not found`() : Unit = runBlocking {
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(null)

      Assertions.assertThatThrownBy {
        groupsService.deleteChildGroup("CG")
      }.isInstanceOf(ChildGroupNotFoundException::class.java)
        .hasMessage("Unable to get child group: CG with reason: notfound")
    }
  }

  @Nested
  inner class ParentGroup {

    @Test
    fun `Create group`() : Unit = runBlocking {
      val createGroup = CreateGroup(groupCode = "CG", groupName = "Group")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(null)

      groupsService.createGroup(createGroup)
      val cg = Group("CG", " Group")
      verify(groupRepository).findByGroupCode("CG")
      verify(groupRepository).save(cg)
      verify(telemetryClient).trackEvent(
        "GroupCreateSuccess",
        mapOf("username" to "username", "groupCode" to "CG", "groupName" to "Group"),
        null
      )
    }

    @Test
    fun `Create group exists`() : Unit = runBlocking {
      val createGroup = CreateGroup(groupCode = "CG", groupName = "Group")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(Group("code", "name"))

      Assertions.assertThatThrownBy {
        groupsService.createGroup(createGroup)
      }.isInstanceOf(GroupExistsException::class.java)
        .hasMessage("Unable to create group: CG with reason: group code already exists")
    }

    @Test
    fun `update group details`() : Unit = runBlocking {
      val dbGroup = Group("bob", "disc")
      val groupAmendment = GroupAmendment("Joe")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

      groupsService.updateGroup("bob", groupAmendment)

      verify(groupRepository).findByGroupCode("bob")
      verify(groupRepository).save(dbGroup)
      verify(telemetryClient).trackEvent(
        "GroupUpdateSuccess",
        mapOf("username" to "username", "groupCode" to "bob", "newGroupName" to "Joe"),
        null
      )
    }

    @Test
    fun `get group details`() : Unit = runBlocking {
      val dbGroup = Group("bob", "disc")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

      val group = groupsService.getGroupDetail("bob")

      assertThat(group).isEqualTo(dbGroup)
      verify(groupRepository).findByGroupCode("bob")
      verify(maintainUserCheck).ensureMaintainerGroupRelationship("username", "bob")
    }
  }

  @Nested
  inner class DeleteGroup {

    @BeforeEach
    fun initSecurityContext() : Unit = runBlocking {
      whenever(authenticationFacade.getAuthentication()).thenReturn(authentication)
      whenever(authenticationFacade.getAuthentication().authorities).thenReturn(setOf(Authority("ROLE_COMMUNITY", "Role Community")))
    }
    @Test
    fun `delete group, no members`() : Unit = runBlocking {
      val dbGroup = Group("groupCode", "disc")
      whenever(groupRepository.findByGroupCode("groupCode")).thenReturn(dbGroup)
      whenever(userRepository.findAll(any())).thenReturn(listOf())

      groupsService.deleteGroup("groupCode",)
      verify(groupRepository).findByGroupCode("groupCode")
      verify(userRepository).findAll(any())
      verify(groupRepository).delete(dbGroup)
    }

    @Test
    fun `delete group, with members`() : Unit = runBlocking {
      val user1 = UserHelper.createSampleUser(username = "user1")
      val user2 = UserHelper.createSampleUser(username = "user2")
      val dbGroup = Group("groupCode", "disc")
      whenever(groupRepository.findByGroupCode("groupCode")).thenReturn(dbGroup)
      whenever(userRepository.findAll(any())).thenReturn(listOf(user1, user2))

      groupsService.deleteGroup("groupCode")
      verify(groupRepository).findByGroupCode("groupCode")
      verify(userRepository).findAll(any())
      verify(userGroupService, times(2)).removeGroup(anyString(), anyString(), anyString(), any())
      verify(groupRepository).delete(dbGroup)
    }
  }

  @Nested
  inner class ChildGroup {
    @Test
    fun `Delete child group`() : Unit = runBlocking {
      val childGroup = ChildGroup("CG", "disc")
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(childGroup)

      groupsService.deleteChildGroup("CG")
      verify(childGroupRepository).delete(childGroup)
      verify(telemetryClient).trackEvent(
        "GroupChildDeleteSuccess",
        mapOf("username" to "username", "childGroupCode" to "CG"),
        null
      )
    }

    @Test
    fun `Delete child Group not found`() : Unit = runBlocking {
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(null)

      Assertions.assertThatThrownBy {
        groupsService.deleteChildGroup("CG")
      }.isInstanceOf(ChildGroupNotFoundException::class.java)
        .hasMessage("Unable to get child group: CG with reason: notfound")
    }

    @Test
    fun `Update child group details`() : Unit = runBlocking {
      val dbGroup = ChildGroup("bob", "disc")
      val groupAmendment = GroupAmendment("Joe")
      whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

      groupsService.updateChildGroup("bob", groupAmendment)

      verify(childGroupRepository).findByGroupCode("bob")
      verify(childGroupRepository).save(dbGroup)
      verify(telemetryClient).trackEvent(
        "GroupChildUpdateSuccess",
        mapOf("username" to "username", "childGroupCode" to "bob", "newChildGroupName" to "Joe"),
        null
      )
    }

    @Test
    fun `Retrieve child group details`() : Unit = runBlocking {
      val childGroup = ChildGroup("CHILD_1", "test")
      whenever(childGroupRepository.findByGroupCode(childGroup.groupCode)).thenReturn(childGroup)

      val actualChildGroupDetail = groupsService.getChildGroupDetail(childGroup.groupCode)

      assertEquals(childGroup, actualChildGroupDetail)
    }

    @Test
    fun `Retrieve child group details not found`() : Unit = runBlocking {
      whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(null)

      Assertions.assertThatThrownBy { runBlocking {
        groupsService.getChildGroupDetail("CG")}
      }.isInstanceOf(ChildGroupNotFoundException::class.java)
        .hasMessage("Unable to get child group: CG with reason: notfound")
    }
  }*/
}
