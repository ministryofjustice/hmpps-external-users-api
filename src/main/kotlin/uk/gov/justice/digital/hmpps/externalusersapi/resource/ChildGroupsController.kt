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
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupsService
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Validated
@RestController
class ChildGroupsController(
  private val childGroupsService: ChildGroupsService
) {

  @GetMapping("/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Child Group detail.",
    description = "get Child Group Details"
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
  suspend fun getChildGroupDetail(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
  ): ChildGroupDetailsDto {
    val returnedGroup: ChildGroup =
      childGroupsService.getChildGroupDetail(group)
    return ChildGroupDetailsDto(returnedGroup)
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
  suspend fun amendChildGroupName(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the child group to be updated.",
      required = true
    ) @Valid @RequestBody
    groupAmendment: GroupAmendmentDto

  ) = childGroupsService.updateChildGroup(group, groupAmendment)

  @DeleteMapping("/groups/child/{group}")
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
  suspend fun deleteChildGroup(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
  ) {
    childGroupsService.deleteChildGroup(group)
  }

  @PostMapping("/groups/child")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Create child group.",
    description = "Create a Child Group"
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
        description = "Child Group already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
  suspend fun createChildGroup(
    @Schema(description = "Details of the child group to be created.", required = true)
    @Valid @RequestBody
    createChildGroup: CreateChildGroupDto
  ) {
    childGroupsService.createChildGroup(createChildGroup)
  }
}

@Schema(description = "Group Details")
data class ChildGroupDetailsDto(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String
) {
  constructor(g: ChildGroup) : this(g.groupCode, g.groupName)
}

data class CreateChildGroupDto(
  @Schema(required = true, description = "Parent Group Code", example = "HNC_NPS")
  @field:NotBlank(message = "parent group code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val parentGroupCode: String,

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
