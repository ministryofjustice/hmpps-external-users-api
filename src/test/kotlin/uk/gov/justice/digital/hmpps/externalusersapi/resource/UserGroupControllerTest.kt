package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserGroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import java.util.UUID

class UserGroupControllerTest {
  private val userGroupService: UserGroupService = mock()
  private val userGroupController = UserGroupController(userGroupService)

  @Nested
  inner class Groups {

    @Test
    fun `groups userNotFound`(): Unit = runBlocking {
      whenever(userGroupService.getAllGroupsUsingChildGroupsInLieuOfParentGroup(any())).doThrow(UsernameNotFoundException("test"))
      assertThatThrownBy {
        runBlocking {
          userGroupController.groupsByUserId(UUID.randomUUID())
        }
      }
        .isInstanceOf(UsernameNotFoundException::class.java)
        .withFailMessage("test")
    }

    @Test
    fun `groups no children`(): Unit = runBlocking {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")

      val userId = UUID.randomUUID()
      whenever(userGroupService.getParentGroups(userId)).thenReturn(
        mutableListOf(
          group1,
          group2,
        ),
      )

      val responseEntity =
        userGroupController.groupsByUserId(userId, false)

      assertThat(responseEntity).containsOnly(UserGroupDto(group1), UserGroupDto(group2))
    }

    @Test
    fun `groups default children`(): Unit = runBlocking {
      val userId = UUID.randomUUID()
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1", UUID.randomUUID())

      whenever(userGroupService.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)).thenReturn(
        mutableListOf(group1, group2, childGroup),
      )

      val responseEntity = userGroupController.groupsByUserId(userId)

      assertThat(responseEntity).containsOnly(
        UserGroupDto(group1),
        UserGroupDto(group2),
        UserGroupDto(childGroup),
      )
    }

    @Test
    fun `groups with children requested`(): Unit = runBlocking {
      val userId = UUID.randomUUID()
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1", UUID.randomUUID())

      whenever(userGroupService.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId)).thenReturn(
        mutableListOf(group1, group2, childGroup),
      )

      val responseEntity = userGroupController.groupsByUserId(userId, true)

      assertThat(responseEntity).containsOnly(
        UserGroupDto(group1),
        UserGroupDto(group2),
        UserGroupDto(childGroup),
      )
    }
  }

  @Test
  fun `should remove group by user id`(): Unit = runBlocking {
    val id = UUID.randomUUID()
    userGroupController.removeGroupByUserId(id, "test group")

    verify(userGroupService).removeGroupByUserId(id, "test group")
  }

  @Test
  fun `should add group to user`(): Unit = runBlocking {
    val id = UUID.randomUUID()
    userGroupController.addGroupByUserId(id, "test group")

    verify(userGroupService).addGroupByUserId(id, "test group")
  }
}
