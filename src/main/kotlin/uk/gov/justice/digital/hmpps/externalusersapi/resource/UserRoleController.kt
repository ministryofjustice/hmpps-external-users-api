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
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserRoleDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserRoleService
import java.util.UUID
import javax.validation.constraints.NotEmpty

@RestController
@Tag(name = "/users/{userId}/roles", description = "User Roles Controller")
class UserRoleController(
  private val userRoleService: UserRoleService
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/users/{userId}/roles")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Get roles for user.",
    description = "Get roles for user."
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
        responseCode = "403",
        description = "Unable to maintain user, the user is not within one of your groups.",
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
  suspend fun rolesByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID
  ): Set<UserRoleDto> =
    userRoleService.getUserRoles(userId)
      ?.map { role -> UserRoleDto(role) }?.toSet()
      ?: throw UsernameNotFoundException("User $userId not found")

  @DeleteMapping("/users/{userId}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Remove role from user.",
    description = "Remove role from user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Removed."
      ),
      ApiResponse(
        responseCode = "400",
        description = "Validation failed.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
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
        description = "Unable to maintain user, the user is not within one of your groups.",
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
  suspend fun removeRoleByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "The role code of the role to be delete from the user.", required = true)
    @PathVariable
    role: String,
  ) {
    userRoleService.removeRoleByUserId(userId, role)
    log.info("Remove role succeeded for userId {} and role {}", userId, role)
  }

  @PostMapping("/users/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Add roles to user.",
    description = "Add role to user, post version taking multiple roles"
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Added."
      ),
      ApiResponse(
        responseCode = "400",
        description = "Validation failed.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
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
        description = "Unable to maintain user, the user is not within one of your groups.",
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
      ),
      ApiResponse(
        responseCode = "409",
        description = "Role(s) for user already exists..",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun addRolesByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "List of roles to be assigned.", required = true)
    @RequestBody
    @NotEmpty
    roles: List<String>
  ) {
    userRoleService.addRolesByUserId(userId, roles)
    log.info("Add role succeeded for userId {} and roles {}", userId, roles.toString())
  }

  @GetMapping("/users/{userId}/assignable-roles")
  @Operation(
    summary = "Get list of assignable roles.",
    description = "Get list of roles that can be assigned by the current user.  This is dependent on the group membership, although super users can assign any role"
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
  suspend fun assignableRoles(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
  ): List<UserRoleDto> {
    val roles = userRoleService.getAssignableRolesByUserId(userId)
    return roles.map { UserRoleDto(it) }
  }

  @GetMapping("/users/me/searchable-roles")
  @Operation(
    summary = "Get list of searchable roles.",
    description = "Get list of roles that can be search for by the current user."
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
  suspend fun searchableRoles(): List<UserRoleDto> {
    val roles = userRoleService.getAllAssignableRoles()
    return roles.map { UserRoleDto(it) }
  }
  @GetMapping("/users/username/{username}/roles")
  @Operation(
    summary = "List of roles for user.",
    description = "List of roles for user. Currently restricted to service specific roles: ROLE_INTEL_ADMIN or ROLE_PCMS_USER_ADMIN."
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
  @PreAuthorize("hasAnyRole('ROLE_INTEL_ADMIN', 'ROLE_PCMS_USER_ADMIN','ROLE_PF_USER_ADMIN')")
  suspend fun userRoles(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String
  ): Set<UserRole> {

    val userRoles = userRoleService.getRolesByUsername(username)

    if (userRoles.count() == 0) throw UsernameNotFoundException("User $username not found")

    return userRoles.map { role -> UserRole(role.roleCode) }.toSet()
  }
}
@Schema(description = "User Role")
data class UserRole(
  @Schema(required = true, description = "Role Code", example = "GLOBAL_SEARCH")
  val roleCode: String,
)
