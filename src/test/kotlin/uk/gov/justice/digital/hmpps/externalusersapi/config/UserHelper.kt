package uk.gov.justice.digital.hmpps.externalusersapi.config

import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID

class UserHelper {

  companion object {
    fun createSampleUser(
      username: String = "firstlast",
      authorities: Set<Authority> = emptySet(),
      groups: Set<Group> = emptySet(),
      source: AuthSource = AuthSource.auth,
      id: UUID? = null,
    ): User {

      val user = User(
        username = username,
        authorities = authorities,
        groups = groups,
        source = source,
      )
      user.id = id
      return user
    }
  }
}
