package uk.gov.justice.digital.hmpps.externalusersapi.resource.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User

@JsonInclude(NON_NULL)
@Schema(description = "User email details")
data class EmailAddressDto(
  @Schema(required = true, description = "Username", example = "DEMO_USER1")
  val username: String,

  @Schema(description = "Email", example = "john.smith@digital.justice.gov.uk")
  val email: String?,

  @Schema(required = true, description = "Verified email", example = "true")
  val verified: Boolean,
) {

  constructor(u: User) : this(u.getUserName(), u.email, u.verified)
}
