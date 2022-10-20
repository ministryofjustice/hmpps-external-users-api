package uk.gov.justice.digital.hmpps.externalusersapi.data

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserGroup

@Schema(description = "Group Details")
data class GroupDetails(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  // @Schema(required = true, description = "Assignable Roles")
  // val assignableRoles: List<UserAssignableRole>,

  @Schema(required = true, description = "Child Groups")
  val children: List<UserGroup>
) {
  constructor(g: Group, children: List<ChildGroup>) : this(
    g.groupCode,
    g.groupName,
    // g.assignableRoles.map { UserAssignableRole(it.role, it.automatic) }.sortedBy { it.roleName },
    children.map { UserGroup(it) }.sortedBy { it.groupName }
  )
}
