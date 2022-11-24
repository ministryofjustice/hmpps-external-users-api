package uk.gov.justice.digital.hmpps.externalusersapi.security

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group

class MaintainUserCheckTest {
  private val authenticationFacade: AuthenticationFacade = mock()
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck = MaintainUserCheck(authenticationFacade, groupRepository)

  @BeforeEach
  fun initSecurityContext(): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn("username")
    whenever(authenticationFacade.hasRoles(anyOrNull())).thenReturn(false)
  }

  @Test
  fun `Group manager able to maintain group`(): Unit = runBlocking {
    whenever(groupRepository.findGroupsByUsername("groupManager")).thenReturn(flowOf(Group("group1", "desc"), Group("group2", "desc")))

    assertThatCode {
      runBlocking {
        maintainUserCheck.ensureMaintainerGroupRelationship(
          "groupManager",
          "group1"
        )
      }
    }.doesNotThrowAnyException()

    verify(groupRepository).findGroupsByUsername("groupManager")
  }

  @Test
  fun `Group manager does not have group so cannot maintain`(): Unit = runBlocking {
    whenever(groupRepository.findGroupsByUsername("groupManager")).thenReturn(flowOf(Group("group1", "desc"), Group("group2", "desc")))
    assertThatThrownBy {
      runBlocking {
        maintainUserCheck.ensureMaintainerGroupRelationship(
          "groupManager",
          "group3"
        )
      }
    }.isInstanceOf(GroupRelationshipException::class.java)
      .hasMessage("Unable to maintain group: group3 with reason: Group not with your groups")

    verify(groupRepository).findGroupsByUsername("groupManager")
  }
}
