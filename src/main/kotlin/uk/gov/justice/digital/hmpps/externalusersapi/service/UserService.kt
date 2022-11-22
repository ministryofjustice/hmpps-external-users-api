package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository

@Service
@Transactional(readOnly = true)
class UserService(
  private val userRepository: UserRepository
) {

  @Transactional(readOnly = true)
  suspend fun findUser(username: String) = userRepository.findByUsernameAndSource(username)
    ?: throw UsernameNotFoundException("User with username $username not found")
}
