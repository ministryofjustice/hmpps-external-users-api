package uk.gov.justice.digital.hmpps.externalusersapi.security

import com.google.common.collect.Sets
import kotlinx.coroutines.flow.toSet
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService
import java.util.Optional

@Service
class MaintainUserCheck(
  private val authenticationFacade: AuthenticationFacade,
  private val userService: UserService,
  private val groupRepository: GroupRepository
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
    val maintainer = userService.getUserAndGroupByUserName(userName)
    // otherwise group managers must have a group in common for maintenance
    if (maintainer != null) {
      if (maintainer.groups.none { it.groupCode == groupCode }) {
        // no group in common, so disallow
        throw GroupRelationshipException(groupCode, "Group not with your groups")
      }
    }
  }

  @Throws(UserGroupRelationshipException::class)
  suspend fun ensureUserLoggedInUserRelationship(loggedInUser: String?, authorities: Collection<GrantedAuthority>, user: User): User? {
    // if they have maintain privileges then all good
    if (canMaintainUsers(authorities)) {
      return null
    }
    // otherwise group managers must have a group in common for maintenance
    val loggedInUserEmail = userService.getUser(loggedInUser)
    Optional.of(loggedInUserEmail!!).orElseThrow()
    val userGroups = groupRepository.findGroupsByUsername(user.getUserName()).toSet()

    if (Sets.intersection(loggedInUserEmail.groups, userGroups).isEmpty()) {
      // no group in common, so disallow
      throw UserGroupRelationshipException(user.name, "User not with your groups")
    }

    return loggedInUserEmail
  }
}

class UserGroupRelationshipException(username: String, errorCode: String) :
  Exception("Unable to maintain user: $username with reason: $errorCode")

class GroupRelationshipException(group: String, errorCode: String) :
  Exception("Unable to maintain group: $group with reason: $errorCode")
