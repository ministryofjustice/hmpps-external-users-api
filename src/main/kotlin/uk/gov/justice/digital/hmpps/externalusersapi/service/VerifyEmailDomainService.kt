package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.EmailDomainRepository

@Service
@Transactional(readOnly = true)
class VerifyEmailDomainService(
  private val emailDomainRepository: EmailDomainRepository
) {
  suspend fun isValidEmailDomain(domain: String): Boolean = emailDomainRepository.countMatching(domain.lowercase()) > 0
}
