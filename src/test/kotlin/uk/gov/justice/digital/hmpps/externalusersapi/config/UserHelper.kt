package uk.gov.justice.digital.hmpps.externalusersapi.config

import uk.gov.justice.digital.hmpps.externalusersapi.assembler.model.UserDto
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
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

    fun createSampleUserWithGroupAndAuthority(
      username: String = "firstlast",
      authorities: Set<Authority> = emptySet(),
      groups: Set<Group> = emptySet(),
      source: AuthSource = AuthSource.auth,
      id: UUID? = null,
    ): UserDto {

      val user = UserDto(
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
