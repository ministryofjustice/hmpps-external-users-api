package uk.gov.justice.digital.hmpps.externalusersapi.resource.data

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority

@Schema(description = "User Role")
data class UserRole(
  @Schema(required = true, description = "Role Code", example = "LICENCE_RO")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Licence Responsible Officer")
  val roleName: String,

  @Schema(required = true, description = "Role Description", example = "Responsible for licences")
  val roleDescription: String?
) {
  constructor(a: Authority) : this(a.roleCode, a.roleName, a.roleDescription)
}
