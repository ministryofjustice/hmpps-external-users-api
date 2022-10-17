package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import java.util.UUID

class UserGroupControllerTest {
  private val userGroupService: UserGroupService = mock()
  private val userGroupController = UserGroupController(userGroupService)

  @Nested
  inner class Groups {

    @Test
    fun `groups userNotFound`() {
      whenever(userGroupService.getGroups(any())).thenReturn(null)
      Assertions.assertThatThrownBy {
        userGroupController.groupsByUserId(UUID.randomUUID())
      }
        .isInstanceOf(UsernameNotFoundException::class.java)
    }

    @Test
    fun `groups no children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      whenever(userGroupService.getGroups(any())).thenReturn(
        setOf(
          group1,
          group2
        )
      )
      val responseEntity =
        userGroupController.groupsByUserId(UUID.randomUUID(), false)
      Assertions.assertThat(responseEntity).containsOnly(UserGroup(group1), UserGroup(group2))
    }

    @Test
    fun `groups default children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(userGroupService.getGroups(any())).thenReturn(
        setOf(
          group1,
          group2
        )
      )
      val responseEntity = userGroupController.groupsByUserId(UUID.randomUUID())
      Assertions.assertThat(responseEntity).containsOnly(UserGroup("FRED", "desc"), UserGroup("CHILD_1", "child 1"))
    }

    @Test
    fun `groups with children requested`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(userGroupService.getGroups(any())).thenReturn(
        setOf(
          group1,
          group2
        )
      )
      val responseEntity = userGroupController.groupsByUserId(UUID.randomUUID())
      Assertions.assertThat(responseEntity).containsOnly(UserGroup("FRED", "desc"), UserGroup("CHILD_1", "child 1"))
    }
  }

  @Test
  fun `should remove group by user id`() {
    val id = UUID.randomUUID()
    userGroupController.removeGroupByUserId(id, "test group")

    verify(userGroupService).removeGroupByUserId(id, "test group")
  }
}
