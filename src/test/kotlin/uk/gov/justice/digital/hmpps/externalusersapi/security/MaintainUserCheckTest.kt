package uk.gov.justice.digital.hmpps.externalusersapi.security

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import java.util.Optional.of

class MaintainUserCheckTest {
  private val userRepository: UserRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val maintainUserCheck = MaintainUserCheck(userRepository, authenticationFacade)
  private val authentication: Authentication = mock()

  @BeforeEach
  fun initSecurityContext() {

    whenever(authenticationFacade.currentUsername).thenReturn("username")
    SecurityContextHolder.getContext().authentication = authentication
  }

  @Test
  fun `Group manager able to maintain group`() {
    val groupManager =
      createSampleUser("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString()))
      .thenReturn(of(groupManager))
    assertThatCode {
      maintainUserCheck.ensureMaintainerGroupRelationship(
        "group1"
      )
    }.doesNotThrowAnyException()
    verify(userRepository).findByUsername(ArgumentMatchers.anyString())
  }

  @Test
  fun `Group manager does not have group so cannot maintain`() {
    val groupManager =
      createSampleUser("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userRepository.findByUsername(anyOrNull()))
      .thenReturn(of(groupManager))
    assertThatThrownBy {
      maintainUserCheck.ensureMaintainerGroupRelationship(
        "group3"
      )
    }.isInstanceOf(MaintainUserCheck.AuthGroupRelationshipException::class.java)
      .hasMessage("Unable to maintain group: group3 with reason: Group not with your groups")
    verify(userRepository).findByUsername(anyOrNull())
  }
}
