package uk.gov.justice.digital.hmpps.externalusersapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.service.CreateUserService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserLastNameDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import java.util.UUID

class UserControllerTest {
  private val userService: UserService = mock()
  private val userSearchService: UserSearchService = mock()
  private val userGroupService: UserGroupService = mock()
  private val createUserService: CreateUserService = mock()
  private val userController = UserController(userService, userSearchService, userGroupService, createUserService)

  @Test
  fun enableUserByUserId(): Unit = runBlocking {
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

  @Test
  fun `getAllUsers should return list of UserLastNameDto`(): Unit = runBlocking {
    val users = listOf(
      UserLastNameDto("user1", "Doe"),
      UserLastNameDto("user2", "Smith"),
    )
    whenever(userService.getAllUsersLastName()).thenReturn(users)

    val response: List<UserLastNameDto> = userController.getAllUsersLastNames()

    assertThat(response).hasSize(2)
    assertThat(response[0].lastName).isEqualTo("Doe")
    assertThat(response[1].lastName).isEqualTo("Smith")
    verify(userService).getAllUsersLastName()
  }
}
