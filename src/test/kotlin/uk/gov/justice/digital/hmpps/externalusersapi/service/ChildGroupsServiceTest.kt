package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.resource.GroupAmendmentDto
import java.util.UUID

class ChildGroupsServiceTest {

  private val childGroupRepository: ChildGroupRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()

  private val childGroupsService = ChildGroupsService(childGroupRepository, groupRepository, telemetryClient, authenticationFacade)

  @BeforeEach
  fun initSecurityContext(): Unit = runBlocking {
    whenever(authenticationFacade.getUsername()).thenReturn("username")
  }

  @Nested
  inner class DeleteChildGroup {
    @Test
    fun `Delete child group`(): Unit = runBlocking {
      val childGroup = ChildGroup("CG", "disc", UUID.randomUUID())
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(childGroup)

      childGroupsService.deleteChildGroup("CG")
      verify(childGroupRepository).deleteByGroupCode("CG")
      verify(telemetryClient).trackEvent(
        "GroupChildDeleteSuccess",
        mapOf("username" to "username", "childGroupCode" to "CG"),
        null,
      )
    }

    @Test
    fun `Child Group not found`(): Unit = runBlocking {
      whenever(childGroupRepository.findByGroupCode("CG")).thenReturn(null)

      Assertions.assertThatThrownBy {
        runBlocking { childGroupsService.deleteChildGroup("CG") }
      }.isInstanceOf(ChildGroupNotFoundException::class.java)
        .hasMessage("Unable to get child group: CG with reason: notfound")
    }
  }

  @Nested
  inner class UpdateChildGroup {
    @Test
    fun `Update child group details`(): Unit = runBlocking {
      val dbGroup = ChildGroup("bob", "disc", UUID.randomUUID())
      val groupAmendment = GroupAmendmentDto("Joe")
      whenever(childGroupRepository.findByGroupCode(ArgumentMatchers.anyString())).thenReturn(dbGroup)

      childGroupsService.updateChildGroup("bob", groupAmendment)

      verify(childGroupRepository).findByGroupCode("bob")
      verify(childGroupRepository).save(dbGroup)
      verify(telemetryClient).trackEvent(
        "GroupChildUpdateSuccess",
        mapOf("username" to "username", "childGroupCode" to "bob", "newChildGroupName" to "Joe"),
        null,
      )
    }
  }

  @Nested
  inner class RetrieveChildGroupDetails {
    @Test
    fun `Retrieve child group details`(): Unit = runBlocking {
      val childGroup = ChildGroup("CHILD_1", "test", UUID.randomUUID())
      whenever(childGroupRepository.findByGroupCode(childGroup.groupCode)).thenReturn(childGroup)

      val actualChildGroupDetail = childGroupsService.getChildGroupDetail(childGroup.groupCode)

      org.junit.jupiter.api.Assertions.assertEquals(childGroup, actualChildGroupDetail)
    }

    @Test
    fun `Retrieve child group details not found`(): Unit = runBlocking {
      whenever(childGroupRepository.findByGroupCode(ArgumentMatchers.anyString())).thenReturn(null)

      Assertions.assertThatThrownBy {
        runBlocking {
          childGroupsService.getChildGroupDetail("CG")
        }
      }.isInstanceOf(ChildGroupNotFoundException::class.java)
        .hasMessage("Unable to get child group: CG with reason: notfound")
    }
  }
}
