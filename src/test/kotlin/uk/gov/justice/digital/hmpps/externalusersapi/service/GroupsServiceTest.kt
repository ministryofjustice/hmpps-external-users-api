package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck

class GroupsServiceTest {
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val childGroupRepository: ChildGroupRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val groupsService = GroupsService(
    groupRepository,
    maintainUserCheck,
    childGroupRepository,
    telemetryClient,
    authenticationFacade
  )

  @BeforeEach
  fun initSecurityContext() {

    whenever(authenticationFacade.currentUsername).thenReturn("username")
    SecurityContextHolder.getContext().authentication = authentication
  }
  @Test
  fun `get group details`() {
    val dbGroup = Group("bob", "disc")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    val group = groupsService.getGroupDetail("bob")

    assertThat(group).isEqualTo(dbGroup)
    verify(groupRepository).findByGroupCode("bob")
    verify(maintainUserCheck).ensureMaintainerGroupRelationship("username", "bob")
  }

  @Test
  fun `update child group details`() {
    val dbGroup = ChildGroup("bob", "disc")
    val groupAmendment = GroupAmendment("Joe")
    whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    groupsService.updateChildGroup("bob", groupAmendment)

    verify(childGroupRepository).findByGroupCode("bob")
    verify(childGroupRepository).save(dbGroup)
    verify(telemetryClient).trackEvent(
      "GroupChildUpdateSuccess",
      mapOf("username" to "username", "childGroupCode" to "bob", "newChildGroupName" to "Joe"),
      null
    )
  }
}
