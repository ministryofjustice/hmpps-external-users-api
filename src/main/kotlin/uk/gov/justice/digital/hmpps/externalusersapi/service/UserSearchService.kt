package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isNotEmpty
import org.apache.commons.lang3.StringUtils.replace
import org.apache.commons.lang3.StringUtils.upperCase
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserFilter.Status
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserDto
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.util.EmailHelper
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserSearchService(
  private val userGroupService: UserGroupService,
  private val userSearchRepository: UserSearchRepository,
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val authenticationFacade: AuthenticationFacade,
) {

  suspend fun findUsers(
    name: String?,
    roleCodes: List<String>?,
    groupCodes: List<String>?,
    pageable: Pageable,
    status: Status,
  ): Page<UserDto> = coroutineScope {
    val userFilter = UserFilter(
      name = name?.let { formatName(it) },
      roleCodes = roleCodes,
      groupCodes = limitGroupSearchCodesByUserAuthority(groupCodes),
      status = status,
      pageable = pageable,
    )

    val users = async { userSearchRepository.searchForUsers(userFilter) }
    val count = async { userSearchRepository.countAllBy(userFilter) }

    PageImpl(
      users.await().toList(),
      PageRequest.of(
        pageable.pageNumber,
        pageable.pageSize,
        Sort.by(defaultSortOrder()),
      ),
      count.await(),
    )
  }

  suspend fun findUsersByEmail(email: String?): Flow<User> {
    val cleanEmail = EmailHelper.format(email)
    if (isNotEmpty(cleanEmail)) {
      return userRepository.findByEmailAndSourceOrderByUsername(cleanEmail)
    }
    return flowOf()
  }

  suspend fun getUserByUsername(username: String): User = userRepository.findByUsernameAndSource(upperCase(StringUtils.trim(username))) ?: throw UserNotFoundException("Account for username $username not found")

  suspend fun getUserByUserId(userId: UUID): User {
    val user = userRepository.findById(userId) ?: throw UserNotFoundException("User with id $userId not found")
    maintainUserCheck.ensureUserLoggedInUserRelationship(user.getUserName())
    return user
  }

  // NOTE: this function ensures that the response is flagged as sorted, matching the hard coded order by clause in the SQL in UserFilter.
  // The property names do not appear in the response.
  private fun defaultSortOrder(): List<Sort.Order> = listOf(
    Sort.Order.asc("last_name"),
    Sort.Order.asc("first_name"),
  )

  private suspend fun formatName(emailInput: String): String = // Single quotes need to be replaced with 2x single quotes to prevent SQLGrammarExceptions. The first single quote is an escape char.
    replace(replace(StringUtils.lowerCase(StringUtils.trim(emailInput)), "'", "''"), "’", "''")

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

class UserNotFoundException(message: String) : Exception(message)
