package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService

@Validated
@RestController
@Tag(name = "/roles", description = "Role Controller")
class RoleController(
  private val roleService: RoleService,
) {
  @PostMapping("/roles")
  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Create role.",
    description = "Create a Role. Requires role ROLE_ROLES_ADMIN",
  )
  @ApiResponses(
    value = [
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
        responseCode = "409",
        description = "Role already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @Throws(RoleService.RoleExistsException::class)
  suspend fun createRole(
    @Parameter(description = "Details of the role to be created.", required = true)
    @Valid
    @RequestBody
    createRole: CreateRoleDto,
  ) = roleService.createRole(createRole)

  @GetMapping("/roles")
  @PreAuthorize("hasAnyRole('ROLE_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES_ADMIN','ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_VIEW_ADMINISTRABLE_USER_ROLES')")
  @Operation(
    summary = "Get all Roles",
    description = "Get all Roles. Requires role ROLE_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES or ROLE_VIEW_ADMINISTRABLE_USER_ROLES",
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
    ],
  )
  suspend fun getRoles(
    @Parameter(description = "Role admin type to find EXT_ADM, DPS_ADM, DPS_LSA.")
    @RequestParam(required = false)
    adminTypes: List<AdminType>?,
  ) = roleService.getRoles(adminTypes).map { RoleDetailsDto(it) }

  @GetMapping("/roles/paged")
  @PreAuthorize("hasAnyRole('ROLE_ROLES_ADMIN', 'ROLE_VIEW_ADMINISTRABLE_USER_ROLES')")
  @Operation(
    summary = "Get all paged Roles.",
    description = "Get all paged Roles. Requires role ROLE_ROLES_ADMIN or ROLE_VIEW_ADMINISTRABLE_USER_ROLES",
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
        description = "Roles not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun getRoles(
    @Parameter(description = "Role name or partial of a role name")
    @RequestParam(required = false)
    roleName: String?,
    @Parameter(description = "Role code or partial of a role code")
    @RequestParam(required = false)
    roleCode: String?,
    @Parameter(description = "Role admin type to find EXT_ADM, DPS_ADM, DPS_LSA.")
    @RequestParam(required = false)
    adminTypes: List<AdminType>?,
    @PageableDefault(sort = ["roleName"], direction = Sort.Direction.ASC) pageable: Pageable,
  ): Page<RoleDetailsDto> = roleService.getRoles(
    roleName,
    roleCode,
    adminTypes,
    pageable,
  )
    .map { RoleDetailsDto(it) }

  @GetMapping("/roles/{role}")
  @PreAuthorize("hasAnyRole('ROLE_ROLES_ADMIN', 'ROLE_VIEW_ADMINISTRABLE_USER_ROLES')")
  @Operation(
    summary = "Role detail.",
    description = "Get Role Details. Requires role ROLE_ROLES_ADMIN or ROLE_VIEW_ADMINISTRABLE_USER_ROLES",
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
        description = "Role not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun getRoleDetails(
    @Parameter(description = "The Role code of the role.", required = true)
    @PathVariable
    role: String,
  ): RoleDetailsDto = roleService.getRoleDetails(role)

  @PutMapping("/roles/{role}")
  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Amend role name.",
    description = "Amend role name. Requires role ROLE_ROLES_ADMIN",
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
        description = "Role not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun amendRoleName(
    @Parameter(description = "The role code of the role.", required = true)
    @PathVariable
    role: String,
    @Parameter(
      description = "Details of the role to be updated.",
      required = true,
    ) @Valid @RequestBody
    roleAmendment: RoleNameAmendmentDto,
  ) = roleService.updateRoleName(role, roleAmendment)

  @PutMapping("/roles/{role}/description")
  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Amend role description.",
    description = "Amend role description. Requires role ROLE_ROLES_ADMIN",
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
        description = "Role not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun amendRoleDescription(
    @Parameter(description = "The role code of the role.", required = true)
    @PathVariable
    role: String,
    @Parameter(
      description = "Details of the role to be updated.",
      required = true,
    ) @Valid @RequestBody
    roleAmendment: RoleDescriptionAmendmentDto,
  ) = roleService.updateRoleDescription(role, roleAmendment)

  @PutMapping("/roles/{roleCode}/admintype")
  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Amend role admin type.",
    description = "Amend role admin type. Requires role ROLE_ROLES_ADMIN",
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
        description = "Role not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun amendRoleAdminType(
    @Parameter(description = "The role code of the role.", required = true)
    @PathVariable
    roleCode: String,
    @Parameter(
      description = "Details of the role to be updated.",
      required = true,
    )
    @Valid
    @RequestBody
    roleAmendment: RoleAdminTypeAmendmentDto,
  ) = roleService.updateRoleAdminType(roleCode, roleAmendment)
}

@Schema(description = "Role Details")
data class RoleDetailsDto(
  @Schema(required = true, description = "Role Code", example = "AUTH_GROUP_MANAGER")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Auth Group Manager")
  val roleName: String,

  @Schema(
    required = true,
    description = "Role Description",
    example = "Allow Group Manager to administer the account within their groups",
  )
  val roleDescription: String?,

  @Schema(
    required = true,
    description = "Administration Type",
    example = "{\"adminTypeCode\": \"EXT_ADM\",\"adminTypeName\": \"External Administrator\"}",
  )
  val adminType: List<AdminType>,
) {
  constructor(r: Authority) : this(
    r.roleCode,
    r.roleName,
    r.roleDescription,
    r.adminType.split(",").map {
      it.trim()
      AdminType.valueOf(it)
    },
  )
}

data class CreateRoleDto(
  @Schema(required = true, description = "Role Code", example = "AUTH_GROUP_MANAGER")
  @field:NotBlank(message = "role code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val roleCode: String,

  @Schema(required = true, description = "roleName", example = "Auth Group Manager")
  @field:NotBlank(message = "role name must be supplied")
  @field:Size(min = 4, max = 128)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$")
  val roleName: String,

  @Schema(
    required = false,
    description = "roleDescription",
    example = "Allow Group Manager to administer the account within their groups",
  )
  @field:Size(max = 1024)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&\r\n]*\$")
  val roleDescription: String? = null,

  @Schema(
    required = true,
    description = "adminType, can be used if multiple admin types required",
    example = "[\"EXT_ADM\", \"DPS_ADM\"]",
  )
  @field:NotEmpty(message = "Admin type cannot be empty")
  val adminType: Set<AdminType>,
)

@Schema(description = "Role Name")
data class RoleNameAmendmentDto(
  @Schema(required = true, description = "Role Name", example = "Central admin")
  @field:NotBlank(message = "Role name must be supplied")
  @field:Size(min = 4, max = 100)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$")
  val roleName: String,
)

@Schema(description = "Role Description")
data class RoleDescriptionAmendmentDto(
  @Schema(required = true, description = "Role Description", example = "Maintaining admin users")
  @field:Size(max = 1024)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&\r\n]*\$")
  val roleDescription: String?,
)

@Schema(description = "Role Administration Types")
data class RoleAdminTypeAmendmentDto(
  @Schema(required = true, description = "Role Administration Types", example = "[\"DPS_ADM\"]")
  @field:NotEmpty(message = "Admin type cannot be empty")
  val adminType: Set<AdminType>,
)
