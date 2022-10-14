package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.GroupRelationshipException
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.UserGroupRelationshipException
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.ChildGroupNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainAdditionBarredException
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupHasChildGroupException
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleExistsException
import uk.gov.justice.digital.hmpps.externalusersapi.service.RoleService.RoleNotFoundException
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupException
import uk.gov.justice.digital.hmpps.externalusersapi.service.UserGroupManagerException
import javax.validation.ValidationException

@RestControllerAdvice
class HmppsExternalUsersApiExceptionHandler {

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = e.message,
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(RoleExistsException::class)
  fun handleRoleExistsException(e: RoleExistsException): ResponseEntity<ErrorResponse?>? {
    log.error("Unable to add role", e)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = "Unable to add role: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(EmailDomainAdditionBarredException::class)
  fun handleEmailDomainAdditionBarredException(e: EmailDomainAdditionBarredException): ResponseEntity<ErrorResponse> {
    log.error("Unable to add email domain", e)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = "Unable to add email domain: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(EmailDomainNotFoundException::class)
  fun handleEmailDomainNotFoundException(e: EmailDomainNotFoundException): ResponseEntity<ErrorResponse> {
    log.error("Unable to find email domain", e)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Unable to find email domain: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(GroupNotFoundException::class)
  fun handleGroupNotFoundException(e: GroupNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Group Not found: ${e.message}",
          developerMessage = e.message ?: "Error message not set"
        )
      )
  }

  @ExceptionHandler(ChildGroupNotFoundException::class)
  fun handleChildGroupNotFoundException(e: ChildGroupNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug("Child group not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Child group not found: ${e.message}",
          developerMessage = e.message ?: "Error message not set"
        )
      )
  }

  @ExceptionHandler(UserGroupRelationshipException::class)
  fun handleUserGroupRelationshipException(e: UserGroupRelationshipException): ResponseEntity<ErrorResponse> {
    log.debug("User group relationship exception caught: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "User group relationship exception: ${e.message}",
          developerMessage = e.message ?: "Error message not set"
        )
      )
  }

  @ExceptionHandler(UsernameNotFoundException::class)
  fun handleUsernameNotFoundException(e: UsernameNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "User not found: ${e.message}",
          developerMessage = e.message ?: "Error message not set"
        )
      )
  }

  @ExceptionHandler(GroupRelationshipException::class)
  fun handleGroupRelationshipException(e: GroupRelationshipException): ResponseEntity<ErrorResponse> {
    log.debug("Maintain group relationship exception caught: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Maintain group relationship exception: ${e.message}",
          developerMessage = e.message ?: "Error message not set"
        )
      )
  }

  @ExceptionHandler(RoleNotFoundException::class)
  fun handleRoleNotFoundException(e: RoleNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug("Role not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Unable to find role: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(HttpMessageConversionException::class)
  fun handleMismatchedInputException(e: HttpMessageConversionException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(GroupExistsException::class)
  fun handleGroupExistsException(e: GroupExistsException): ResponseEntity<ErrorResponse> {
    log.debug("Group exists exception caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = "Group already exists: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(GroupHasChildGroupException::class)
  fun handleGroupHasChildGroupException(e: GroupHasChildGroupException): ResponseEntity<ErrorResponse> {
    log.debug("Group has children exception caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = e.message,
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(ChildGroupExistsException::class)
  fun handleChildGroupExistsException(e: ChildGroupExistsException): ResponseEntity<ErrorResponse> {
    log.debug("Child group exists exception caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = e.message,
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(UserGroupException::class)
  fun handleUserGroupException(e: UserGroupException): ResponseEntity<ErrorResponse> {
    log.debug("User group exception caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = e.message,
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(UserGroupManagerException::class)
  fun handleUserGroupManagerException(e: UserGroupManagerException): ResponseEntity<ErrorResponse> {
    log.debug("User group exception caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = e.message,
          developerMessage = e.message
        )
      )
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
