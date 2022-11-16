package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.EmailDomainExclusions
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.externalusersapi.repository.EmailDomainRepository
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailDomainDto
import java.util.UUID

@Service
@Transactional
class EmailDomainService(
  private val emailDomainRepository: EmailDomainRepository,
  private val emailDomainExclusions: EmailDomainExclusions,
) {
  private companion object {
    private const val PERCENT = "%"
  }

  @Transactional(readOnly = true)
  suspend fun domainList(): List<EmailDomainDto> {
    val allEmailDomains = emailDomainRepository.findAll()
    val emailDomainDtoList = allEmailDomains.map { emailDomain ->
      EmailDomainDto(
        emailDomain.id.toString(),
        cleanDomainNameForDisplay(emailDomain.name),
        emailDomain.description.toString()
      )
    }
    return emailDomainDtoList.toList().sortedWith(compareBy { it.domain })
  }

  @Throws(EmailDomainNotFoundException::class)
  suspend fun domain(id: UUID): EmailDomainDto {
    return toDto(retrieveDomain(id, "retrieve"))
  }

  @Throws(EmailDomainAdditionBarredException::class)
  suspend fun addDomain(newDomain: CreateEmailDomainDto): EmailDomainDto {
    val domainNameInternal = if (newDomain.name.startsWith(PERCENT)) newDomain.name else PERCENT + newDomain.name
    val existingDomain = emailDomainRepository.findByName(domainNameInternal)

    if (existingDomain != null) {
      throw EmailDomainAdditionBarredException(newDomain.name, "domain already present in allowed list")
    }

    if (emailDomainExclusions.contains(newDomain.name)) {
      throw EmailDomainAdditionBarredException(newDomain.name, "domain present in excluded list")
    }
    return toDto(emailDomainRepository.save(EmailDomain(name = domainNameInternal, description = newDomain.description)))
  }

  @Throws(EmailDomainNotFoundException::class)
  suspend fun removeDomain(id: UUID) {
    val emailDomain = retrieveDomain(id, "delete")
    emailDomainRepository.delete(emailDomain)
  }

  private fun toDto(emailDomain: EmailDomain): EmailDomainDto = EmailDomainDto(
    emailDomain.id.toString(),
    cleanDomainNameForDisplay(emailDomain.name),
    emailDomain.description.toString()
  )

  private suspend fun retrieveDomain(uuid: UUID, action: String): EmailDomain =
    emailDomainRepository.findById(uuid)
      ?: throw EmailDomainNotFoundException(action, uuid, "notfound")

  private fun cleanDomainNameForDisplay(persistedDomainName: String): String =
    persistedDomainName.removePrefix(PERCENT).removePrefix(".")
}

class EmailDomainAdditionBarredException(domain: String, errorCode: String) :
  Exception("Unable to add email domain: $domain to allowed list with reason: $errorCode")

class EmailDomainNotFoundException(action: String, id: UUID, errorCode: String) :
  Exception("Unable to $action email domain id: $id with reason: $errorCode")
