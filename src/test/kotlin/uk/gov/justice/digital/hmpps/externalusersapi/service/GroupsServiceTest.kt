package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateGroup
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

  @Test
  fun `update group details`() {
    val dbGroup = Group("bob", "disc")
    val groupAmendment = GroupAmendment("Joe")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    groupsService.updateGroup("bob", groupAmendment)

    verify(groupRepository).findByGroupCode("bob")
    verify(groupRepository).save(dbGroup)
    verify(telemetryClient).trackEvent(
      "GroupUpdateSuccess",
      mapOf("username" to "username", "groupCode" to "bob", "newGroupName" to "Joe"),
      null
    )
  }

  @Nested
  inner class createGroup {
    @BeforeEach
    fun initSecurityContext() {

      whenever(authenticationFacade.currentUsername).thenReturn("username")
      SecurityContextHolder.getContext().authentication = authentication
    }
    @Test
    fun `Create group`() {
      val createGroup = CreateGroup(groupCode = "CG", groupName = "Group")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(null)

      groupsService.createGroup(createGroup)
      val cg = Group("CG", " Group")
      verify(groupRepository).findByGroupCode("CG")
      verify(groupRepository).save(cg)
      verify(telemetryClient).trackEvent(
        "GroupCreateSuccess",
        mapOf("username" to "username", "groupCode" to "CG", "groupName" to "Group"),
        null
      )
    }

    @Test
    fun `Create group exists`() {
      val createGroup = CreateGroup(groupCode = "CG", groupName = "Group")
      whenever(groupRepository.findByGroupCode(anyString())).thenReturn(Group("code", "name"))

      Assertions.assertThatThrownBy {
        groupsService.createGroup(createGroup)
      }.isInstanceOf(GroupExistsException::class.java)
        .hasMessage("Unable to create group: CG with reason: group code already exists")
    }
  }
}
