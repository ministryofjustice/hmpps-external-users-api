package uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class ErrorDetail(
  @Schema(required = true, description = "Error", example = "Not Found")
  val error: String,

  @Schema(required = true, description = "Error description", example = "User not found.")
  val error_description: String,

  @Schema(description = "Field in error", example = "username")
  val field: String? = null,
)
