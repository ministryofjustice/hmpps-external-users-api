package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import java.util.UUID

class UserGroupControllerTest {
  private val userGroupService: UserGroupService = mock()
  private val authentication: Authentication = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val userGroupController = UserGroupController(userGroupService, authenticationFacade)

  @Nested
  inner class Groups {

    @BeforeEach
    fun setAuthentication() {
      whenever(authenticationFacade.currentUsername).thenReturn("username")
      whenever(authenticationFacade.authentication).thenReturn(authentication)
      whenever(authenticationFacade.authentication.authorities).thenReturn(setOf(Authority("ROLE_MAINTAIN_OAUTH_USERS", "Role Maintain users")))
    }

    @Test
    fun `groups userNotFound`() {
      whenever(userGroupService.getGroups(any(), ArgumentMatchers.anyString(), any())).thenReturn(null)
      Assertions.assertThatThrownBy {
        userGroupController.groupsByUserId(UUID.randomUUID())
      }
        .isInstanceOf(UsernameNotFoundException::class.java)
    }

    @Test
    fun `groups no children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      whenever(userGroupService.getGroups(any(), ArgumentMatchers.anyString(), any())).thenReturn(
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
      whenever(userGroupService.getGroups(any(), ArgumentMatchers.anyString(), any())).thenReturn(
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
      whenever(userGroupService.getGroups(any(), ArgumentMatchers.anyString(), any())).thenReturn(
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
