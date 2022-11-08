package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserFilterSQL.Status
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import java.time.LocalDateTime

@Validated
@RestController
@Tag(name = "/user", description = "External User Controller")
class ExternalUserController(private val userService: UserService) {

  @GetMapping("/search")
  @Operation(
    summary = "Search for an external user."
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
  @PreAuthorize(
    "hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')"
  )
  suspend fun searchForUser(
    @Parameter(
      description = "The username, email or name of the user.",
      example = "j smith"
    ) @RequestParam(required = false)
    name: String?,
    @Parameter(description = "The role codes of the user.") @RequestParam(required = false)
    roles: List<String>?,
    @Parameter(description = "The group codes of the user.") @RequestParam(required = false)
    groups: List<String>?,
    @Parameter(description = "Limit to active / inactive / show all users.") @RequestParam(
      required = false,
      defaultValue = "ALL"
    )
    status: Status,
    @PageableDefault(sort = ["Person.lastName", "Person.firstName"], direction = Sort.Direction.ASC) pageable: Pageable,
    @Parameter(hidden = true) authentication: Authentication
  ): Page<ExternalUser> =
    userService.findAuthUsers(
      name,
      roles,
      groups,
      pageable,
      authentication.name,
      authentication.authorities,
      status
    )

  data class ExternalUser(
    @Schema(
      required = true,
      description = "User ID",
      example = "91229A16-B5F4-4784-942E-A484A97AC865"
    )
    val userId: String? = null,

    @Schema(required = true, description = "Username", example = "externaluser")
    val username: String? = null,

    @Schema(
      required = true,
      description = "Email address",
      example = "external.user@someagency.justice.gov.uk"
    )
    val email: String? = null,

    @Schema(required = true, description = "First name", example = "External")
    val firstName: String? = null,

    @Schema(required = true, description = "Last name", example = "User")
    val lastName: String? = null,

    @Schema(
      required = true,
      description = "Account is locked due to incorrect password attempts",
      example = "true"
    )
    val locked: Boolean = false,

    @Schema(required = true, description = "Account is enabled", example = "false")
    val enabled: Boolean = false,

    @Schema(required = true, description = "Email address has been verified", example = "false")
    val verified: Boolean = false,

    @Schema(required = true, description = "Last time user logged in", example = "01/01/2001")
    val lastLoggedIn: LocalDateTime? = null,

    @Schema(required = true, description = "Inactive reason", example = "Left department")
    val inactiveReason: String? = null
  )
}
