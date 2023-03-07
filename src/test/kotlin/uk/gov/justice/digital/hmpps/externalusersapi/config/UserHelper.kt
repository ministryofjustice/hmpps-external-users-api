package uk.gov.justice.digital.hmpps.externalusersapi.config

import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID

class UserHelper {

  companion object {
    fun createSampleUser(
      username: String = "firstlast",
      source: AuthSource = AuthSource.auth,
      id: UUID? = null,
    ): uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User {
      val user = uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User(
        username = username,
        source = source,
      )
      user.id = id
      return user
    }
  }
}
