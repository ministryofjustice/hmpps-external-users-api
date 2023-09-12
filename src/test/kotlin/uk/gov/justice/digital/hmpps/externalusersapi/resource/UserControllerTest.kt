package uk.gov.justice.digital.hmpps.externalusersapi.resource

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.service.CreateUserService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import java.util.UUID

class UserControllerTest {
  private val userService: UserService = mock()
  private val userSearchService: UserSearchService = mock()
  private val userGroupService: UserGroupService = mock()
  private val request: HttpServletRequest = mock()
  private val createUserService: CreateUserService = mock()
  private val userController = UserController(userService, userSearchService, userGroupService, createUserService)

  @Test
  fun enableUserByUserId(): Unit = runBlocking {
    whenever(request.requestURL).thenReturn(StringBuffer("some/auth/url"))
    userController.enableUserByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
    verify(userService).enableUserByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"))
  }

  @Test
  fun disableUserByUserId(): Unit = runBlocking {
    userController.disableUserByUserId(
      UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"),
      DeactivateReason("A Reason"),
    )
    verify(userService).disableUserByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason")
  }
}
