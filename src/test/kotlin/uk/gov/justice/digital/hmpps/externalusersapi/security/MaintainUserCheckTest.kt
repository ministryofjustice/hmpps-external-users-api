package uk.gov.justice.digital.hmpps.externalusersapi.security

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import java.util.Optional.of

class MaintainUserCheckTest {
  private val userRepository: UserRepository = mock()
  private val maintainUserCheck = MaintainUserCheck(userRepository)

  @Test
  fun `Group manager able to maintain group`() {
    val groupManager =
      createSampleUser("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString()))
      .thenReturn(of(groupManager))
    assertThatCode {
      maintainUserCheck.ensureMaintainerGroupRelationship(
        "groupManager",
        "group1"
      )
    }.doesNotThrowAnyException()
    verify(userRepository).findByUsername(ArgumentMatchers.anyString())
  }

  @Test
  fun `Group manager does not have group so cannot maintain`() {
    val groupManager =
      createSampleUser("groupManager", groups = setOf(Group("group1", "desc"), Group("group2", "desc")))
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString()))
      .thenReturn(of(groupManager))
    assertThatThrownBy {
      maintainUserCheck.ensureMaintainerGroupRelationship(
        "groupManager",
        "group3"
      )
    }.isInstanceOf(MaintainUserCheck.AuthGroupRelationshipException::class.java)
      .hasMessage("Unable to maintain group: group3 with reason: Group not with your groups")
    verify(userRepository).findByUsername(anyOrNull())
  }
}
