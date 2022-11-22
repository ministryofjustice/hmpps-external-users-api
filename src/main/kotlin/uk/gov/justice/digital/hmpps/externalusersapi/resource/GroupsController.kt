package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.GroupDetails
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.UserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupHasChildGroupException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Validated
@RestController
class GroupsController(
  private val groupsService: GroupsService,
) {

  @GetMapping("/groups")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Get all possible groups.",
    description = "Get all groups."
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
  suspend fun allGroups() = groupsService.getAllGroups().map { UserGroup(it) }

  @GetMapping("/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Group detail.",
    description = "return Group Details"
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
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun getGroupDetails(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String
  ): GroupDetails = groupsService.getGroupDetail(group)

  @PutMapping("/groups/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Amend group name.",
    description = "AmendGroupName"
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
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun amendGroupName(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the group to be updated.",
      required = true
    ) @Valid @RequestBody
    groupAmendment: GroupAmendment

  ) = groupsService.updateGroup(group, groupAmendment)

  @PostMapping("/groups")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Create group.",
    description = "Create a Group"
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
        responseCode = "409",
        description = "Group already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  @Throws(GroupExistsException::class, GroupNotFoundException::class)
  suspend fun createGroup(
    @Parameter(description = "Details of the group to be created.", required = true)
    @Valid @RequestBody
    createGroup: CreateGroup
  ) = groupsService.createGroup(createGroup)

  @DeleteMapping("/groups/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Delete group.",
    description = "Delete a Group"
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
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  @Throws(GroupNotFoundException::class, GroupHasChildGroupException::class)
  suspend fun deleteGroup(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String
  ) = groupsService.deleteGroup(group)
}

@Schema(description = "Group Name")
data class GroupAmendment(
  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  @field:NotBlank(message = "parent group code must be supplied")
  @field:Size(min = 4, max = 100)
  val groupName: String
)

@Schema(description = "User Role")
data class UserAssignableRole(
  @Schema(required = true, description = "Role Code", example = "LICENCE_RO")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Licence Responsible Officer")
  val roleName: String,

  @Schema(required = true, description = "automatic", example = "TRUE")
  val automatic: Boolean
)

data class CreateGroup(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  @field:NotBlank(message = "group code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val groupCode: String,

  @Schema(required = true, description = "groupName", example = "HDC NPS North East")
  @field:NotBlank(message = "group name must be supplied")
  @field:Size(min = 4, max = 100)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$")
  val groupName: String
)
