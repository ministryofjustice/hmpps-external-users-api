package uk.gov.justice.digital.hmpps.externalusersapi.security

import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.userdetails.UserDetails
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.User

interface UserPersonDetails : UserDetails, CredentialsContainer {
  val userId: String
  val name: String
  val firstName: String
  val isAdmin: Boolean
  val authSource: String
  fun toUser(): User
}
