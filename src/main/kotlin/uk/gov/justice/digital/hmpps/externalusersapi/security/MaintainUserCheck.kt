package uk.gov.justice.digital.hmpps.externalusersapi.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository

@Service
class MaintainUserCheck(
  private val userRepository: UserRepository,
) {

  companion object {
    fun canMaintainAuthUsers(authorities: Collection<GrantedAuthority>): Boolean =
      authorities.map { it.authority }
        .any { it == "ROLE_MAINTAIN_OAUTH_USERS" }
  }
  @Throws(GroupRelationshipException::class)
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
      throw GroupRelationshipException(groupCode, "Group not with your groups")
    }
  }

  class UserGroupRelationshipException(val username: String, val errorCode: String) :
    Exception("Unable to maintain user: $username with reason: $errorCode")

  class GroupRelationshipException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")
}
