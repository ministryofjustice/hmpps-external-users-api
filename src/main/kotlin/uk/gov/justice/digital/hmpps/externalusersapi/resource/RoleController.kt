package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.ErrorDetail
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService

@Validated
@RestController
@Tag(name = "/roles", description = "Role Controller")
class RoleController(
  private val roleService: RoleService,
) {
  @GetMapping("/roles")
  @PreAuthorize("hasAnyRole('ROLE_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES_ADMIN','ROLE_MAINTAIN_ACCESS_ROLES')")
  @Operation(
    summary = "Get all Roles",
    description = "Get all Roles"
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
            schema = Schema(implementation = ErrorDetail::class)
          )
        ]
      )
    ]
  )
  fun getRoles(
    @Parameter(description = "Role admin type to find EXT_ADM, DPS_ADM, DPS_LSA.")
    @RequestParam(
      required = false
    )
    adminTypes: List<AdminType>?
  ): List<RoleDetails> = roleService.getRoles(adminTypes)
    .map {
      RoleDetails(it)
    }
}

@Schema(description = "Role Details")
data class RoleDetails(
  @Schema(required = true, description = "Role Code", example = "AUTH_GROUP_MANAGER")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Auth Group Manager")
  val roleName: String,

  @Schema(
    required = true,
    description = "Role Description",
    example = "Allow Group Manager to administer the account within their groups"
  )
  val roleDescription: String?,

  @Schema(
    required = true,
    description = "Administration Type",
    example = "{\"adminTypeCode\": \"EXT_ADM\",\"adminTypeName\": \"External Administrator\"}"
  )
  val adminType: List<AdminType>
) {
  constructor(r: Authority) : this(
    r.roleCode,
    r.roleName,
    r.roleDescription,
    r.adminType
  )
}
