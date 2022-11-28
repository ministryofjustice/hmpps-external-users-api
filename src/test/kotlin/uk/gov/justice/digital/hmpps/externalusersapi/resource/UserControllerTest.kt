@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService

class UserControllerTest {
  private val userService: UserService = mock()
  private val userSearchService: UserSearchService = mock()
  private val userController = UserController(userSearchService, userService)

  @Test
  fun enableUserByUserId(): Unit = runBlocking {
    // whenever(request.requestURL).thenReturn(StringBuffer("some/auth/url"))
    userController.enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
    verify(userService).enableUserByUserId("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
  }
}
