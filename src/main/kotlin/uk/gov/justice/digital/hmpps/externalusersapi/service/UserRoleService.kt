package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserRoleService(
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val authenticationFacade: AuthenticationFacade,
  private val roleRepository: RoleRepository,
) {
  suspend fun getUserRoles(userId: UUID) =
    userRepository.findById(userId)?.let { user: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(
        authenticationFacade.getUsername(),
        authenticationFacade.getAuthentication().authorities,
        user
      )
      roleRepository.findRolesByUserId(userId).toList()
    }
}
