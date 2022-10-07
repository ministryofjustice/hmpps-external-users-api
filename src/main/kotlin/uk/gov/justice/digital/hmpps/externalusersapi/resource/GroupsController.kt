package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import uk.gov.justice.digital.hmpps.externalusersapi.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupsService
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Validated
@RestController
class GroupsController(
  private val groupsService: GroupsService
) {

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
  fun getGroupDetail(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String
  ): GroupDetails {
    val returnedGroup: Group = groupsService.getGroupDetail(group)
    return GroupDetails(returnedGroup)
  }

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
  fun amendGroupName(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the group to be updated.",
      required = true
    ) @Valid @RequestBody
    groupAmendment: GroupAmendment

  ) {
    groupsService.updateGroup(group, groupAmendment)
  }

  @PutMapping("/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Amend child group name.",
    description = "Amend a Child Group Name"
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
        description = "Child Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun amendChildGroupName(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the child group to be updated.",
      required = true
    ) @Valid @RequestBody
    groupAmendment: GroupAmendment

  ) {
    groupsService.updateChildGroup(group, groupAmendment)
  }

  @DeleteMapping("/api/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Delete child group.",
    description = "Delete a Child Group"
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
        description = "Child Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun deleteChildGroup(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
  ) {
    groupsService.deleteChildGroup(group)
  }

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
  fun createGroup(
    @Parameter(description = "Details of the group to be created.", required = true)
    @Valid @RequestBody
    createGroup: CreateGroup
  ) {
    groupsService.createGroup(createGroup)
  }
}

@Schema(description = "Group Name")
data class GroupAmendment(
  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  @field:NotBlank(message = "parent group code must be supplied")
  @field:Size(min = 4, max = 100)
  val groupName: String
)

@Schema(description = "User Role")
data class AuthUserAssignableRole(
  @Schema(required = true, description = "Role Code", example = "LICENCE_RO")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Licence Responsible Officer")
  val roleName: String,

  @Schema(required = true, description = "automatic", example = "TRUE")
  val automatic: Boolean
) {

  constructor(a: Authority, automatic: Boolean) : this(a.roleCode, a.roleName, automatic)
}
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

@Schema(description = "Group Details")
data class GroupDetails(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  @Schema(required = true, description = "Assignable Roles")
  val assignableRoles: List<AuthUserAssignableRole>,

  @Schema(required = true, description = "Child Groups")
  val children: List<AuthUserGroup>
) {
  constructor(g: Group) : this(
    g.groupCode,
    g.groupName,
    g.assignableRoles.map { AuthUserAssignableRole(it.role, it.automatic) }.sortedBy { it.roleName },
    g.children.map { AuthUserGroup(it) }.sortedBy { it.groupName }
  )
}
