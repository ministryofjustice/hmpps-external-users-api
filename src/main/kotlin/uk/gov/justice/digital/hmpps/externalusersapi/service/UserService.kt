package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailNotificationDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailUpdateDto
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
  @Value("\${application.authentication.disable.login-days}") private val loginDaysTrigger: Int,
) {

  suspend fun hasPassword(userId: UUID) = getUserForUpdate(userId).hasPassword()

  @Transactional
  suspend fun updateUserEmailAndUsername(userId: UUID, emailUpdateDto: EmailUpdateDto) {
    val user = getUserForUpdate(userId)
    user.email = emailUpdateDto.email
    user.setUsername(emailUpdateDto.username)
    user.verified = false
    userRepository.save(user)
  }

  @Transactional
  @Throws(UserGroupRelationshipException::class)
  suspend fun enableUserByUserId(userId: UUID): EmailNotificationDto {
    val user = getUserForUpdate(userId)
    user.setEnabled(true)
    user.inactiveReason = null
    // give user 7 days grace if last logged in more than x days ago
    if (user.lastLoggedIn.isBefore(LocalDateTime.now().minusDays(loginDaysTrigger.toLong()))) {
      user.lastLoggedIn = LocalDateTime.now().minusDays(loginDaysTrigger - 7L)
    }
    userRepository.save(user)
    telemetryClient.trackEvent("ExternalUserEnabled", mapOf("username" to user.name, "admin" to authenticationFacade.getUsername()), null)
    log.debug("User {} enabled and saved", user)
    return EmailNotificationDto(
      firstName = user.getFirstName(),
      username = user.getUserName(),
      email = user.email,
      admin = authenticationFacade.getUsername()
    )
  }

  @Transactional
  @Throws(UserGroupRelationshipException::class)
  suspend fun disableUserByUserId(userId: UUID, inactiveReason: String) {
    val user = getUserForUpdate(userId)
    user.setDisabled(false)
    user.inactiveReason = inactiveReason
    userRepository.save(user)
    telemetryClient.trackEvent("ExternalUserDisabled", mapOf("username" to user.name, "admin" to authenticationFacade.getUsername()), null)
  }

  private suspend fun getUserForUpdate(userId: UUID): User {
    userRepository.findById(userId)?.let { user ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)
      return user
    } ?: throw UserNotFoundException("User $userId not found")
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
