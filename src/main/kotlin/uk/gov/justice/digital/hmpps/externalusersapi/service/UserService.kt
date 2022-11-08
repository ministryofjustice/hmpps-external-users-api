package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserFilterSQL
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.ExternalUserController.ExternalUser
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.Optional

@Service
@Transactional(readOnly = true)
class UserService(
  private val roleRepository: RoleRepository,
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
  private val userGroupService: UserGroupService,
  private val userSearchRepository: UserSearchRepository
) {
  @Transactional
  suspend fun getUser(username: String?): User? {

    var user = userRepository.findByUsernameAndSource(username).awaitSingleOrNull()
    Optional.of(user!!).orElseThrow()
    val groups = user.id?.let {
      groupRepository.findGroupByUserId(it)
    }?.toList()

    val roles = user.id?.let { roleRepository.findRoleByUserId(it) }?.toList()

    groups?.toList()
      ?.let {
        roles?.toList()
          ?.let { it1 ->
            if (username != null) {
              user = User(
                username = username,
                authorities = it1.toSet(),
                groups = it.toSet(),
                source = AuthSource.auth
              )
            }
          }
      }
    return user
  }

  @Transactional
  suspend fun getUserAndGroupByUserName(username: String?): User? {

    var user = userRepository.findByUsernameAndSource(username).awaitSingleOrNull()
    Optional.of(user!!).orElseThrow()
    val groups = user.id?.let {
      groupRepository.findGroupByUserId(it)
    }?.toList()
    groups?.toList()
      ?.let { it ->

        if (username != null) {
          user = User(
            username = username,
            groups = it.toSet(),
            source = AuthSource.auth
          )
        }
      }
    return user
  }

  suspend fun findAuthUsers(
    name: String?,
    roleCodes: List<String>?,
    groupCodes: List<String>?,
    pageable: Pageable,
    searcher: String,
    authorities: Collection<GrantedAuthority>,
    status: UserFilterSQL.Status,
    authSources: List<AuthSource> = listOf(AuthSource.auth),
  ): Page<ExternalUser> = coroutineScope {
    val groupSearchCodes = if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
      groupCodes
    } else if (authorities.any { it.authority == "ROLE_AUTH_GROUP_MANAGER" }) {
      val assignableGroupCodes = userGroupService.getAssignableGroups(searcher, authorities).map { it.groupCode }
      if (groupCodes.isNullOrEmpty()) assignableGroupCodes else groupCodes.filter { g -> assignableGroupCodes.any { it == g } }
    } else {
      emptyList()
    }
    val userFilter = UserFilterSQL(
      name = name,
      roleCodes = roleCodes,
      groupCodes = groupSearchCodes,
      status = status,
      pageable = pageable
    )

    val externalUsers = async { userSearchRepository.searchForUsers(userFilter) }
    val count = async { userSearchRepository.countAllBy(userFilter) }

    PageImpl(
      externalUsers.await().toList(),
      pageable,
      count.await().awaitSingle()
    )
  }
}
