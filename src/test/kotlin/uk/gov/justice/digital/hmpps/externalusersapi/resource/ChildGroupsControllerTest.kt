package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupsService
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import java.util.UUID

class ChildGroupsControllerTest {
  private val childGroupsService: ChildGroupsService = mock()

  private val childGroupsController = ChildGroupsController(childGroupsService)

  @Nested
  inner class ChildGroup {
    @Test
    fun `child group details`(): Unit = runBlocking {
      val childGroupCode = "CHILD_1"
      val childGroupName = "Child - Site 1 - Group 2"
      val uuid = UUID.randomUUID()

      whenever(childGroupsService.getChildGroupDetail(childGroupCode)).thenReturn(
        ChildGroup(
          childGroupCode,
          childGroupName,
          uuid
        )
      )
      val actualChildGroupDetail = childGroupsController.getChildGroupDetail(childGroupCode)

      Assertions.assertEquals(childGroupCode, actualChildGroupDetail.groupCode)
      Assertions.assertEquals(childGroupName, actualChildGroupDetail.groupName)
      verify(childGroupsService).getChildGroupDetail(childGroupCode)
    }

    @Test
    fun `amend child group name`(): Unit = runBlocking {
      val groupAmendment = GroupAmendment("groupie")
      childGroupsController.amendChildGroupName("group1", groupAmendment)
      verify(childGroupsService).updateChildGroup("group1", groupAmendment)
    }

    @Test
    fun `delete child group`(): Unit = runBlocking {
      childGroupsController.deleteChildGroup("childGroup")
      verify(childGroupsService).deleteChildGroup("childGroup")
    }
  }

  @Nested
  inner class CreateChildGroup {
    @Test
    fun create(): Unit = runBlocking {
      val childGroup = CreateChildGroup("PG", "CG", "Group")
      childGroupsController.createChildGroup(childGroup)
      verify(childGroupsService).createChildGroup(childGroup)
    }

    @Test
    fun `create - group already exist exception`(): Unit = runBlocking {
      doThrow(ChildGroupExistsException("child_code", "group code already exists")).whenever(childGroupsService)
        .createChildGroup(
          any()
        )

      val childGroup = CreateChildGroup("parent_code", "child_code", "Child group")
      org.assertj.core.api.Assertions.assertThatThrownBy { runBlocking { childGroupsController.createChildGroup(childGroup) } }
        .isInstanceOf(ChildGroupExistsException::class.java)
        .withFailMessage("Unable to maintain group: code with reason: group code already exists")
    }

    @Test
    fun `create - parent group not found exception`(): Unit = runBlocking {
      doThrow(GroupNotFoundException("create", "NotGroup", "ParentGroupNotFound")).whenever(childGroupsService)
        .createChildGroup(
          any()
        )

      val childGroup = CreateChildGroup("parent_code", "child_code", "Child group")

      org.assertj.core.api.Assertions.assertThatThrownBy { runBlocking { childGroupsController.createChildGroup(childGroup) } }
        .isInstanceOf(GroupNotFoundException::class.java)
        .withFailMessage("Unable to maintain group: NotGroup with reason: not found")
    }
  }
}
