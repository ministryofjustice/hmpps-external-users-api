package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

@Service
@Transactional(readOnly = true)
class UserService(
  private val roleRepository: RoleRepository,
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
) {
  @Transactional
  suspend fun getUser(username: String?): User {

    var user = userRepository.findUserByUserNameAndSource(username, AuthSource.auth).awaitSingle()

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
}