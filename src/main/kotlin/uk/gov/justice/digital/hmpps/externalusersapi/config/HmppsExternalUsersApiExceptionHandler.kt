package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainAdditionBarredException
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainNotFoundException
import javax.validation.ValidationException

@RestControllerAdvice
class HmppsExternalUsersApiExceptionHandler {

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
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
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

  @ExceptionHandler(EmailDomainAdditionBarredException::class)
  fun handleEmailDomainAdditionBarredException(e: EmailDomainAdditionBarredException): ResponseEntity<ErrorResponse?>? {
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
  fun handleEmailDomainNotFoundException(e: EmailDomainNotFoundException): ResponseEntity<ErrorResponse?>? {
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
