package uk.gov.justice.digital.hmpps.externalusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.externalusersapi.service.VerifyEmailDomainService

@RestController
@Validated
@Tag(name = "/validate", description = "Validation Controller")
class ValidationController(
  private val verifyEmailDomainService: VerifyEmailDomainService
) {
  @GetMapping("/validate/email-domain")
  @Operation(
    summary = "Validates Email domain",
    description = "Validates Email domain."
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
  suspend fun isValidEmailDomain(@RequestParam(value = "emailDomain", required = true) emailDomain: String): Boolean =
    verifyEmailDomainService.isValidEmailDomain(emailDomain)
}
