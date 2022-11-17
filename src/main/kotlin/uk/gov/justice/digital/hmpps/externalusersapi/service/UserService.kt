package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
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

    var user = userRepository.findByUsernameAndSource(username)
    Optional.of(user!!).orElseThrow()
    val groups = user.id?.let {
      groupRepository.findGroupsByUserId(it)
    }?.toList()

    val roles = user.id?.let { roleRepository.findRolesByUserId(it) }?.toList()

    groups?.toList()
      ?.let {
        roles?.toList()
          ?.let { it1 ->
            user = User(
              username = username,
              authorities = it1.toSet(),
              groups = it.toSet(),
              source = AuthSource.auth
            )
          }
      }
    return user
  }

  @Transactional
  suspend fun getUserAndGroupByUserName(username: String): User? {

    var user = userRepository.findByUsernameAndSource(username)
    Optional.of(user!!).orElseThrow()
    val groups = user.id?.let {
      groupRepository.findGroupsByUserId(it)
    }?.toList()
    groups?.toList()
      ?.let { it ->
        user = User(
          username = username,
          groups = it.toSet(),
          source = AuthSource.auth
        )
      }
    return user
  }
}
