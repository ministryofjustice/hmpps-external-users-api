package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.Optional

@Service
@Transactional(readOnly = true)
class UserService(
  private val roleRepository: RoleRepository,
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
) {
  @Transactional
  suspend fun getUser(username: String): User? {

    val user = userRepository.findByUsernameAndSource(username)
    Optional.of(user!!).orElseThrow()
    var userWithGroupAndRoles = User(username = user.getUserName(), source = user.source)
    val groups = user.id?.let {
      groupRepository.findGroupsByUserId(it)
    }?.toList()

    val roles = user.id?.let { roleRepository.findRolesByUserId(it) }?.toList()

    groups?.toList()
      ?.let {
        roles?.toList()
          ?.let { it1 ->
            userWithGroupAndRoles = User(
              username = username,
              authorities = it1.toSet(),
              groups = it.toSet(),
              source = AuthSource.auth
            )
          }
      }
    return userWithGroupAndRoles
  }

  @Transactional
  suspend fun getUserAndGroupByUserName(username: String): User? {

    val user = userRepository.findByUsernameAndSource(username)
    Optional.of(user!!).orElseThrow()
    var userWithGroup = User(username = user.getUserName(), source = user.source)
    val groups = user.id?.let {
      groupRepository.findGroupsByUserId(it)
    }?.toList()
    groups?.toList()
      ?.let { it ->
        userWithGroup = User(
          username = username,
          groups = it.toSet(),
          source = AuthSource.auth
        )
      }
    return userWithGroup
  }
}
