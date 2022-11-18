package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.toList
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

@Service
@Transactional(readOnly = true)
class UserService(
  private val roleRepository: RoleRepository,
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
) {

  @Transactional(readOnly = true)
  suspend fun findUser(username: String) = userRepository.findByUsernameAndSource(username)
    ?: throw UsernameNotFoundException("User with username $username not found")

  @Transactional(readOnly = true)
  suspend fun getUser(username: String): User {
    val user = findUser(username)
    val groups = groupRepository.findGroupsByUserId(user.id!!).toList()
    val roles = roleRepository.findRolesByUserId(user.id!!).toList()

    return User(
      username = username,
      authorities = roles.toSet(),
      groups = groups.toSet(),
      source = AuthSource.auth
    )
  }

  @Transactional
  suspend fun getUserAndGroupByUserName(username: String): User {
    val user = findUser(username)
    val groups = groupRepository.findGroupsByUserId(user.id!!).toList()
    return User(
      username = username,
      groups = groups.toSet(),
      source = AuthSource.auth
    )
  }
}
