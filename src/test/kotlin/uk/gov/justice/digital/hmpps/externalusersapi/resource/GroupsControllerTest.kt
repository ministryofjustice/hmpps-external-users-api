@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.GroupDetails
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserGroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService

class GroupsControllerTest {
  private val groupsService: GroupsService = mock()
  private val groupsController = GroupsController(groupsService)

  @Nested
  inner class Groups {
    @Test
    fun allGroups(): Unit = runBlocking {
      val group1 = Group("GROUP_1", "first group")
      val group2 = Group("GLOBAL_SEARCH", "global search")
      val userGroup1 = UserGroupDto(group1)
      val userGroup2 = UserGroupDto(group2)
      val allGroups = flowOf(group1, group2)
      val allUserGroups = flowOf(userGroup1, userGroup2)

      whenever(groupsService.getAllGroups()).thenReturn(allGroups)
      val response = groupsController.allGroups()
      verify(groupsService).getAllGroups()
      assertThat(response.toList()).isEqualTo(allUserGroups.toList())
    }
  }

  @Nested
  inner class `parent group` {
    @Test
    fun create(): Unit = runBlocking {
      val childGroup = CreateGroupDto("CG", "Group")
      groupsController.createGroup(childGroup)
      verify(groupsService).createGroup(childGroup)
    }

    @Test
    fun `create - group already exist exception`(): Unit = runBlocking {
      doThrow(GroupExistsException("_code", "group code already exists")).whenever(groupsService)
        .createGroup(
          any(),
        )

      assertThatThrownBy { runBlocking { groupsController.createGroup(CreateGroupDto(groupCode = "g", groupName = "name")) } }
        .isInstanceOf(GroupExistsException::class.java)
        .withFailMessage("Unable to maintain group: code with reason: group code already exists")
    }

    @Test
    fun `get group details`(): Unit = runBlocking {
      val assignableRole = listOf(UserAssignableRoleDto(roleName = "Role1", roleCode = "RO1", automatic = true))
      val childGroup = listOf(UserGroupDto(groupCode = "BOB", groupName = "desc"))
      val groupDetail = GroupDetails(groupCode = "FRED", groupName = "desc", assignableRoles = assignableRole, children = childGroup)
      whenever(
        groupsService.getGroupDetail(
          groupCode = anyString(),
        ),
      ).thenReturn(groupDetail)

      val groupDetails = groupsController.getGroupDetails("group1")
      assertThat(groupDetails).isEqualTo(
        GroupDetails(
          groupCode = "FRED",
          groupName = "desc",
          assignableRoles = listOf(
            UserAssignableRoleDto(
              roleCode = "RO1",
              roleName = "Role1",
              automatic = true,
            ),
          ),
          children = listOf(UserGroupDto(groupCode = "BOB", groupName = "desc")),
        ),
      )
    }

    @Test
    fun `Group Not Found`(): Unit = runBlocking {
      doThrow(GroupNotFoundException("find", "NotGroup", "not found")).whenever(groupsService)
        .getGroupDetail(
          anyString(),
        )

      assertThatThrownBy { runBlocking { groupsController.getGroupDetails("NotGroup") } }
        .isInstanceOf(GroupNotFoundException::class.java)
        .withFailMessage("Unable to find group: NotGroup with reason: not found")
    }

    @Test
    fun `amend group name`(): Unit = runBlocking {
      val groupAmendment = GroupAmendmentDto("groupie")
      groupsController.amendGroupName("group1", groupAmendment)
      verify(groupsService).updateGroup("group1", groupAmendment)
    }
  }

  @Nested
  inner class `delete group` {
    @Test
    fun `delete group`(): Unit = runBlocking {
      groupsController.deleteGroup("GroupCode")
      verify(groupsService).deleteGroup("GroupCode")
    }
  }
}
