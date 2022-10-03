@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.GroupAssignableRole
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService

class GroupsControllerTest {
  private val groupsService: GroupsService = mock()
  private val authentication: Authentication = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val groupsController = GroupsController(groupsService, authenticationFacade)

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
          AuthUserAssignableRole(
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

    doThrow(GroupsService.GroupNotFoundException("find", "NotGroup", "not found")).whenever(groupsService)
      .getGroupDetail(
        anyString()
      )

    assertThatThrownBy { groupsController.getGroupDetail("NotGroup") }
      .isInstanceOf(GroupsService.GroupNotFoundException::class.java)
      .withFailMessage("Unable to find group: NotGroup with reason: not found")
  }

  @Test
  fun `amend child group name`() {
    val groupAmendment = GroupAmendment("groupie")
    whenever(authenticationFacade.currentUsername).thenReturn("user")
    SecurityContextHolder.getContext().authentication = authentication
    groupsController.amendChildGroupName("group1", groupAmendment)
    verify(groupsService).updateChildGroup("user", "group1", groupAmendment)
  }
}
