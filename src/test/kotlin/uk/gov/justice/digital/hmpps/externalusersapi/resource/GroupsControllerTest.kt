@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

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
import uk.gov.justice.digital.hmpps.externalusersapi.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.GroupAssignableRole
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService.GroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService.GroupNotFoundException

class GroupsControllerTest {
  private val groupsService: GroupsService = mock()
  private val groupsController = GroupsController(groupsService)

  @Test
  fun `get group details`() {
    val authority = Authority(roleCode = "RO1", roleName = "Role1")
    val group1 = Group(groupCode = "FRED", groupName = "desc")
    group1.assignableRoles.add(GroupAssignableRole(role = authority, group = group1, automatic = true))
    group1.children.add(ChildGroup(groupCode = "BOB", groupName = "desc"))

    whenever(
      groupsService.getGroupDetail(
        groupCode = anyString()
      )
    ).thenReturn(group1)

    val groupDetails = groupsController.getGroupDetail("group1")
    assertThat(groupDetails).isEqualTo(
      GroupDetails(
        groupCode = "FRED",
        groupName = "desc",
        assignableRoles = listOf(
          UserAssignableRole(
            roleCode = "RO1",
            roleName = "Role1",
            automatic = true
          )
        ),
        children = listOf(AuthUserGroup(groupCode = "BOB", groupName = "desc"))
      )
    )
  }

  @Test
  fun `Group Not Found`() {

    doThrow(GroupNotFoundException("find", "NotGroup", "not found")).whenever(groupsService)
      .getGroupDetail(
        anyString()
      )

    assertThatThrownBy { groupsController.getGroupDetail("NotGroup") }
      .isInstanceOf(GroupNotFoundException::class.java)
      .withFailMessage("Unable to find group: NotGroup with reason: not found")
  }

  @Test
  fun `amend child group name`() {
    val groupAmendment = GroupAmendment("groupie")
    groupsController.amendChildGroupName("group1", groupAmendment)
    verify(groupsService).updateChildGroup("group1", groupAmendment)
  }

  @Test
  fun `amend group name`() {
    val groupAmendment = GroupAmendment("groupie")
    groupsController.amendGroupName("group1", groupAmendment)
    verify(groupsService).updateGroup("group1", groupAmendment)
  }

  @Nested
  inner class `create group` {
    @Test
    fun create() {
      val childGroup = CreateGroup("CG", "Group")
      groupsController.createGroup(childGroup)
      verify(groupsService).createGroup(childGroup)
    }

    @Test
    fun `create - group already exist exception`() {
      doThrow(GroupExistsException("_code", "group code already exists")).whenever(groupsService)
        .createGroup(
          any()
        )

      @Suppress("ClassName") val Group = CreateGroup("_code", " group")
      assertThatThrownBy { groupsController.createGroup(Group) }
        .isInstanceOf(GroupExistsException::class.java)
        .withFailMessage("Unable to maintain group: code with reason: group code already exists")
    }
  }

  @Nested
  inner class `delete group` {
    @Test
    fun `delete group`() {
      groupsController.deleteGroup("GroupCode")
      verify(groupsService).deleteGroup("GroupCode")
    }
  }
}
