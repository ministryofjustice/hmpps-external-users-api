package uk.gov.justice.digital.hmpps.externalusersapi.security

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUserWithGroupAndAuthority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService

class MaintainUserCheckTest {
  private val userService: UserService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck = MaintainUserCheck(authenticationFacade, userService, groupRepository)

  @BeforeEach
  fun initSecurityContext(): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn("username")
    whenever(authenticationFacade.hasRoles(anyOrNull())).thenReturn(false)
  }

  @Test
  fun `Group manager able to maintain group`(): Unit = runBlocking {
    val groupManager =
      createSampleUserWithGroupAndAuthority("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userService.getUserAndGroupByUserName(ArgumentMatchers.anyString()))
      .thenReturn(groupManager)
    assertThatCode {
      runBlocking {
        maintainUserCheck.ensureMaintainerGroupRelationship(
          "groupManager",
          "group1"
        )
      }
    }.doesNotThrowAnyException()
    verify(userService).getUserAndGroupByUserName(ArgumentMatchers.anyString())
  }

  @Test
  fun `Group manager does not have group so cannot maintain`(): Unit = runBlocking {
    val groupManager =
      createSampleUserWithGroupAndAuthority("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userService.getUserAndGroupByUserName(ArgumentMatchers.anyString()))
      .thenReturn(groupManager)
    assertThatThrownBy {
      runBlocking {
        maintainUserCheck.ensureMaintainerGroupRelationship(
          "groupManager",
          "group3"
        )
      }
    }.isInstanceOf(GroupRelationshipException::class.java)
      .hasMessage("Unable to maintain group: group3 with reason: Group not with your groups")
    whenever(userService.getUserAndGroupByUserName(ArgumentMatchers.anyString()))
      .thenReturn(groupManager)
  }
}
