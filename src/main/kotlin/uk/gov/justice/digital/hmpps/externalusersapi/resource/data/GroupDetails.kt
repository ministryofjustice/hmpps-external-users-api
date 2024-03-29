package uk.gov.justice.digital.hmpps.externalusersapi.resource.data

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserAssignableRoleDto

@Schema(description = "Group Details")
data class GroupDetails(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  @Schema(required = true, description = "Assignable Roles")
  val assignableRoles: List<UserAssignableRoleDto>,

  @Schema(required = true, description = "Child Groups")
  val children: List<UserGroupDto>,
) {
  constructor(g: Group, children: List<ChildGroup>, assignableRoles: List<UserAssignableRoleDto>) : this(
    g.groupCode,
    g.groupName,
    assignableRoles,
    children.map { UserGroupDto(it) }.sortedBy { it.groupName },
  )
}
