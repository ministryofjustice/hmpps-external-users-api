package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserFilter
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserFilter.Status
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.resource.ExternalUserController
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

@Service
@Transactional(readOnly = true)
class UserSearchService(
  private val userGroupService: UserGroupService,
  private val userSearchRepository: UserSearchRepository
) {

  suspend fun findAuthUsers(
    name: String?,
    roleCodes: List<String>?,
    groupCodes: List<String>?,
    pageable: Pageable,
    searcher: String,
    authorities: Collection<GrantedAuthority>,
    status: Status,
    authSources: List<AuthSource> = listOf(AuthSource.auth),
  ): Page<ExternalUserController.ExternalUser> = coroutineScope {
    val groupSearchCodes = if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
      groupCodes
    } else if (authorities.any { it.authority == "ROLE_AUTH_GROUP_MANAGER" }) {
      val assignableGroupCodes = userGroupService.getAssignableGroups(searcher, authorities).map { it.groupCode }
      if (groupCodes.isNullOrEmpty()) assignableGroupCodes else groupCodes.filter { g -> assignableGroupCodes.any { it == g } }
    } else {
      emptyList()
    }
    val userFilter = UserFilter(
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
