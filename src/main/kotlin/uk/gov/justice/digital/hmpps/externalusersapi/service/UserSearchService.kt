package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils.isNotEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter.Status
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserDto
import uk.gov.justice.digital.hmpps.externalusersapi.util.EmailHelper

@Service
@Transactional(readOnly = true)
class UserSearchService(
  private val userGroupService: UserGroupService,
  private val userSearchRepository: UserSearchRepository,
  private val userRepository: UserRepository,
  private val authenticationFacade: AuthenticationFacade
) {

  suspend fun findUsers(
    name: String?,
    roleCodes: List<String>?,
    groupCodes: List<String>?,
    pageable: Pageable,
    status: Status
  ): Page<UserDto> = coroutineScope {

    val userFilter = UserFilter(
      name = name,
      roleCodes = roleCodes,
      groupCodes = limitGroupSearchCodesByUserAuthority(groupCodes),
      status = status,
      pageable = pageable
    )

    val users = async { userSearchRepository.searchForUsers(userFilter) }
    val count = async { userSearchRepository.countAllBy(userFilter) }

    PageImpl(
      users.await().toList(),
      pageable,
      count.await()
    )
  }

  suspend fun findUsersByEmail(email: String?): Flow<User> {
    val cleanEmail = EmailHelper.format(email)
    if (isNotEmpty(cleanEmail)) {
      return userRepository.findByEmailAndSourceOrderByUsername(cleanEmail)
    }
    return flowOf()
  }

  suspend fun getUserByUsername(username: String): User =
    userRepository.findByUsernameAndSource(StringUtils.upperCase(StringUtils.trim(username))) ?: throw UsernameNotFoundException("Account for username $username not found")

  private suspend fun limitGroupSearchCodesByUserAuthority(groupCodes: List<String>?): List<String>? {
    val authorities = authenticationFacade.getAuthentication().authorities
    return if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
      groupCodes
    } else if (authorities.any { it.authority == "ROLE_AUTH_GROUP_MANAGER" }) {
      val assignableGroupCodes = userGroupService.getAssignableGroups(authenticationFacade.getUsername(), authorities).map { it.groupCode }
      if (groupCodes.isNullOrEmpty()) assignableGroupCodes else groupCodes.filter { g -> assignableGroupCodes.any { it == g } }
    } else {
      emptyList()
    }
  }
}
