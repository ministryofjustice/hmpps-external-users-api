package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserGroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupService
import java.util.UUID

@Validated
@RestController
@Tag(name = "/users/{userId}/groups", description = "User Groups Controller")
class UserGroupController(
  private val userGroupService: UserGroupService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/users/{userId}/groups")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_VIEW_USER_GROUPS')")
  @Operation(
    summary = "Get groups for userId.",
    description = "Get groups for userId. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER or ROLE_VIEW_USER_GROUPS",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun groupsByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "Whether groups are expanded into their children.", required = false)
    @RequestParam(defaultValue = "true")
    children: Boolean = true,
  ): List<UserGroupDto> {
    if (children) {
      return userGroupService.getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId).map { UserGroupDto(it) }
    }
    return userGroupService.getParentGroups(userId).map { UserGroupDto(it) }
  }

  @DeleteMapping("/users/{userId}/groups/{group}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Remove group from user.",
    description = "Remove group from user. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Validation failed.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun removeGroupByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "The group to be delete from the user.", required = true)
    @PathVariable
    group: String,
  ) {
    userGroupService.removeGroupByUserId(userId, group)
    log.info("Remove group succeeded for userId {} and group {}", userId, group)
  }

  @PutMapping("/users/{userId}/groups/{group}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Add group to user.",
    description = "Add group to user. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Added",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Group for user already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun addGroupByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID,
    @Parameter(description = "The group code of the group to be added to the user.", required = true) @PathVariable
    group: String,
  ) {
    userGroupService.addGroupByUserId(userId, group)
    log.info("Add group succeeded for userId {} and group {}", userId, group)
  }
}
