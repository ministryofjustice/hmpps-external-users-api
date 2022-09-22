package uk.gov.justice.digital.hmpps.externalusersapi.security

interface AuthenticationFacade {
  val currentUsername: String?
}
