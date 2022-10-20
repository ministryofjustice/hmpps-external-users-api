package uk.gov.justice.digital.hmpps.externalusersapi.security

import com.google.common.collect.Sets
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.User

@Service
class MaintainUserCheck(
  private val userRepository: UserRepository,
  private val authenticationFacade: AuthenticationFacade,
) {
  companion object {
    fun canMaintainUsers(authorities: Collection<GrantedAuthority>): Boolean =
      authorities.map { it.authority }
        .any { it == "ROLE_MAINTAIN_OAUTH_USERS" }
  }

  @Throws(GroupRelationshipException::class)
  suspend fun ensureMaintainerGroupRelationship(
    userName: String?,
    groupCode: String,
  ) {
    // if they have maintain privileges then all good
    if (authenticationFacade.hasRoles("ROLE_MAINTAIN_OAUTH_USERS")) {
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

  @Throws(UserGroupRelationshipException::class)
  suspend fun ensureUserLoggedInUserRelationship(loggedInUser: String?, authorities: Collection<GrantedAuthority>, user: User) {
    // if they have maintain privileges then all good
    if (canMaintainUsers(authorities)) {
      return
    }
    // otherwise group managers must have a group in common for maintenance
    val loggedInUserEmail = userRepository.findByUsername(loggedInUser).orElseThrow()
    if (Sets.intersection(loggedInUserEmail.groups, user.groups).isEmpty()) {
      // no group in common, so disallow
      throw UserGroupRelationshipException(user.name, "User not with your groups")
    }
  }

  class UserGroupRelationshipException(val username: String, val errorCode: String) :
    Exception("Unable to maintain user: $username with reason: $errorCode")

  class GroupRelationshipException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")
}
