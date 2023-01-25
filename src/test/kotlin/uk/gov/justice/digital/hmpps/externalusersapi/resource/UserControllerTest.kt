package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import wiremock.javax.servlet.http.HttpServletRequest
import java.util.UUID

class UserControllerTest {
  private val userService: UserService = mock()
  private val userSearchService: UserSearchService = mock()
  private val userGroupService: UserGroupService = mock()
  private val request: HttpServletRequest = mock()
  private val userController = UserController(userService, userSearchService, userGroupService)

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
      DeactivateReason("A Reason")
    )
    verify(userService).disableUserByUserId(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), "A Reason")
  }

  @Test
  fun myRoles(): Unit = runBlocking {
    whenever(userService.myRoles()).thenReturn(listOf(UserRole("BOB"), UserRole("JOE_FRED")))
    assertThat(userController.myRoles()).containsOnly(UserRole("BOB"), UserRole("JOE_FRED"))
  }
}
