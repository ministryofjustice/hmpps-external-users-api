package uk.gov.justice.digital.hmpps.externalusersapi.security

import com.google.common.collect.Sets
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User

@Service
class MaintainUserCheck(
  private val authenticationFacade: AuthenticationFacade,
  private val groupRepository: GroupRepository
) {
  companion object {
    fun canMaintainUsers(authorities: Collection<GrantedAuthority>): Boolean =
      authorities.map { it.authority }
        .any { it == "ROLE_MAINTAIN_OAUTH_USERS" }
  }

  @Throws(GroupRelationshipException::class)
  suspend fun ensureMaintainerGroupRelationship(
    maintainerName: String,
    groupCode: String,
  ) = coroutineScope {

    // All good if user holds maintain privilege
    if (authenticationFacade.hasRoles("ROLE_MAINTAIN_OAUTH_USERS")) {
      return@coroutineScope
    }

    // Otherwise, group managers must have a group in common for maintenance
    val groupsByUsername = async { groupRepository.findGroupsByUsername(maintainerName) }
    if (groupsByUsername.await().toList().none { it.groupCode == groupCode }) {
      // No group in common, so disallow
      throw GroupRelationshipException(groupCode, "Group not with your groups")
    }
  }

  @Throws(UserGroupRelationshipException::class)
  suspend fun ensureUserLoggedInUserRelationship(loggedInUser: String, authorities: Collection<GrantedAuthority>, user: User) = coroutineScope {

    // All good if user holds maintain privilege
    if (canMaintainUsers(authorities)) {
      return@coroutineScope
    }
    // Otherwise, group managers must have a group in common for maintenance
    val loggedInUserGroups = async { groupRepository.findGroupsByUsername(loggedInUser) }
    val userGroups = async { groupRepository.findGroupsByUsername(user.getUserName()) }

    if (Sets.intersection(loggedInUserGroups.await().toSet(), userGroups.await().toSet()).isEmpty()) {
      // No group in common, so disallow
      throw UserGroupRelationshipException(user.name, "User not with your groups")
    }
  }
}

class UserGroupRelationshipException(username: String, errorCode: String) :
  Exception("Unable to maintain user: $username with reason: $errorCode")

class GroupRelationshipException(group: String, errorCode: String) :
  Exception("Unable to maintain group: $group with reason: $errorCode")
