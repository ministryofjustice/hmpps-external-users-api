package uk.gov.justice.digital.hmpps.externalusersapi.security

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository

@Service
class MaintainUserCheck(
  private val userRepository: UserRepository,
) {
  @Throws(AuthGroupRelationshipException::class)
  fun ensureMaintainerGroupRelationship(
    userName: String?,
    groupCode: String,
  ) {
    // if they have maintain privileges then all good
    if (AuthenticationFacade.hasRoles("ROLE_MAINTAIN_OAUTH_USERS")) {
      return
    }
    val maintainer =
      userRepository.findByUsername(userName).orElseThrow()
    // otherwise group managers must have a group in common for maintenance
    if (maintainer.groups.none { it.groupCode == groupCode }) {
      // no group in common, so disallow
      throw AuthGroupRelationshipException(groupCode, "Group not with your groups")
    }
  }

  class AuthUserGroupRelationshipException(val username: String, val errorCode: String) :
    Exception("Unable to maintain user: $username with reason: $errorCode")

  class AuthGroupRelationshipException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")
}
