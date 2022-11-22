package uk.gov.justice.digital.hmpps.externalusersapi.security

import com.google.common.collect.Sets
import kotlinx.coroutines.flow.toSet
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalusersapi.assembler.UserDtoAssembler
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User

@Service
class MaintainUserCheck(
  private val authenticationFacade: AuthenticationFacade,
  private val groupRepository: GroupRepository,
  private val userDtoAssembler: UserDtoAssembler
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
  ) {
    // All good if the user is allowed to maintain
    if (authenticationFacade.hasRoles("ROLE_MAINTAIN_OAUTH_USERS")) {
      return
    }
    val maintainer = userDtoAssembler.assembleUser(maintainerName)
    // Otherwise, group managers must have a group in common for maintenance
    if (maintainer.groups.none { it.groupCode == groupCode }) {
      // No group in common, so disallow
      throw GroupRelationshipException(groupCode, "Group not with your groups")
    }
  }

  @Throws(UserGroupRelationshipException::class)
  suspend fun ensureUserLoggedInUserRelationship(loggedInUser: String, authorities: Collection<GrantedAuthority>, user: User) {
    // All good if the user is allowed to maintain
    if (canMaintainUsers(authorities)) {
      return
    }
    // Otherwise, group managers must have a group in common for maintenance
    val loggedInUserEmail = userDtoAssembler.assembleUserWithAuthorities(loggedInUser)
    val userGroups = groupRepository.findGroupsByUsername(user.getUserName()).toSet()

    if (Sets.intersection(loggedInUserEmail.groups, userGroups).isEmpty()) {
      // No group in common, so disallow
      throw UserGroupRelationshipException(user.name, "User not with your groups")
    }
  }
}

class UserGroupRelationshipException(username: String, errorCode: String) :
  Exception("Unable to maintain user: $username with reason: $errorCode")

class GroupRelationshipException(group: String, errorCode: String) :
  Exception("Unable to maintain group: $group with reason: $errorCode")
