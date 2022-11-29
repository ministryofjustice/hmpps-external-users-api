package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserRoleDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserRoleService
import java.util.UUID

@Validated
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
}
