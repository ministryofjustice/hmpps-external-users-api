package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import java.util.UUID

class UserGroupControllerTest {
  private val userGroupService: UserGroupService = mock()

  private val userGroupController = UserGroupController(userGroupService)

  @Test
  fun `should remove group by user id`() {
    val id = UUID.randomUUID()
    userGroupController.removeGroupByUserId(id, "test group")

    verify(userGroupService).removeGroupByUserId(id, "test group")
  }
}
