package uk.gov.justice.digital.hmpps.externalusersapi.assembler

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.assembler.model.UserDto
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserService

@Component
@Transactional(readOnly = true)
class UserDtoAssembler(
  private val userService: UserService,
  private val groupRepository: GroupRepository,
  private val roleRepository: RoleRepository
) {

  suspend fun assembleUserWithGroupsAndAuthorities(username: String): UserDto {
    val user = userService.findUser(username)
    val groups = groupRepository.findGroupsByUserId(user.id!!).toList()
    val roles = roleRepository.findRolesByUserId(user.id!!).toList()

    return UserDto(
      username = username,
      authorities = roles.toSet(),
      groups = groups.toSet(),
      source = AuthSource.auth
    )
  }

  suspend fun assembleUserWithGroups(username: String): UserDto {
    val user = userService.findUser(username)
    val groups = groupRepository.findGroupsByUserId(user.id!!).toList()
    return UserDto(
      username = username,
      groups = groups.toSet(),
      source = AuthSource.auth
    )
  }
}
