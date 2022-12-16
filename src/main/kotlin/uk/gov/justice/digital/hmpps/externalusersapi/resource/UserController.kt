package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter.Status
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserGroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import java.time.LocalDateTime
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/users")
@Tag(name = "/users", description = "External User Controller")
class UserController(
  private val userService: UserService,
  private val userSearchService: UserSearchService,
  private val userGroupService: UserGroupService,
) {

  @GetMapping
  @Operation(
    summary = "Search for a user.",
    description = "Search for a user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = UserDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "204",
        description = "No users found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun searchForUser(
    @Parameter(description = "The email address of the user.", required = true) @RequestParam
    email: String?
  ): ResponseEntity<Any> {
    val users = userSearchService.findUsersByEmail(email).map { UserDto.fromUser(it) }
    return if (users.count() == 0) ResponseEntity.noContent().build() else ResponseEntity.ok(users)
  }

  @GetMapping("/{username}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "User detail.",
    description = "User detail."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun user(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String
  ) = UserDto.fromUser(userSearchService.getUserByUsername(username))

  @GetMapping("/search")
  @Operation(
    summary = "Search for an external user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  @PreAuthorize(
    "hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')"
  )
  suspend fun searchForUser(
    @Parameter(
      description = "The username, email or name of the user.",
      example = "j smith"
    ) @RequestParam(required = false)
    name: String?,
    @Parameter(description = "The role codes of the user.") @RequestParam(required = false)
    roles: List<String>?,
    @Parameter(description = "The group codes of the user.") @RequestParam(required = false)
    groups: List<String>?,
    @Parameter(description = "Limit to active / inactive / show all users.") @RequestParam(
      required = false,
      defaultValue = "ALL"
    )
    status: Status,
    @RequestParam(value = "page", defaultValue = "0", required = false) page: Int,
    @RequestParam(value = "size", defaultValue = "10", required = false) size: Int
  ): Page<UserDto> =
    userSearchService.findUsers(
      name,
      roles,
      groups,
      PageRequest.of(page, size),
      status
    )

  @PutMapping("/{userId}/enable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Enable a user.",
    description = "Enable a user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EmailNotificationDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unable to enable user, the user is not within one of your groups.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun enableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID
  ) = userService.enableUserByUserId(
    userId
  )

  @PutMapping("/{userId}/disable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Disable a user.",
    description = "Disable a user."
  )
  @ResponseStatus(HttpStatus.OK)
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK."
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unable to disable user, the user is not within one of your groups.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun disableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID,
    @Parameter(
      description = "The reason user made inactive.",
      required = true
    ) @RequestBody
    deactivateReason: DeactivateReason
  ) = userService.disableUserByUserId(
    userId,
    deactivateReason.reason
  )

  @GetMapping("/me/assignable-groups")
  @Operation(
    summary = "Get list of assignable groups.",
    description = "Get list of groups that can be assigned by the current user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun assignableGroups(): List<UserGroupDto> {
    val groups = userGroupService.getMyAssignableGroups()
    return groups.map { UserGroupDto(it) }
  }
}

data class UserDto(
  @Schema(
    required = true,
    description = "User ID",
    example = "91229A16-B5F4-4784-942E-A484A97AC865"
  )
  val userId: String? = null,

  @Schema(required = true, description = "Username", example = "externaluser")
  val username: String? = null,

  @Schema(
    required = true,
    description = "Email address",
    example = "external.user@someagency.justice.gov.uk"
  )
  val email: String? = null,

  @Schema(required = true, description = "First name", example = "External")
  val firstName: String? = null,

  @Schema(required = true, description = "Last name", example = "User")
  val lastName: String? = null,

  @Schema(
    required = true,
    description = "Account is locked due to incorrect password attempts",
    example = "true"
  )
  val locked: Boolean = false,

  @Schema(required = true, description = "Account is enabled", example = "false")
  val enabled: Boolean = false,

  @Schema(required = true, description = "Email address has been verified", example = "false")
  val verified: Boolean = false,

  @Schema(required = true, description = "Last time user logged in", example = "01/01/2001")
  val lastLoggedIn: LocalDateTime? = null,

  @Schema(required = true, description = "Inactive reason", example = "Left department")
  val inactiveReason: String? = null
) {
  companion object {
    fun fromUser(user: User): UserDto {
      return UserDto(
        userId = user.id.toString(),
        username = user.name,
        email = user.email,
        firstName = user.getFirstName(),
        lastName = user.lastName,
        locked = user.locked,
        enabled = user.isEnabled(),
        verified = user.verified,
        lastLoggedIn = user.lastLoggedIn,
        inactiveReason = user.inactiveReason
      )
    }
  }
}

data class EmailNotificationDto(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "email of the user", example = "Smith@gov.uk")
  val email: String?,

  @Schema(description = "admin id who enabled user", example = "ADMIN_USR")
  val admin: String
)

@Schema(description = "Deactivate Reason")
data class DeactivateReason(
  @Schema(required = true, description = "Deactivate Reason", example = "User has left")
  @field:Size(max = 100, min = 4, message = "Reason must be between 4 and 100 characters") @NotBlank
  val reason: String
)
