package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.EmailDomainRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import java.util.UUID

class VerifyEmailDomainServiceTest {
  private val emailDomainRepository: EmailDomainRepository = mock()

  private val verifyService = VerifyEmailDomainService(emailDomainRepository)

  @Test
  fun shouldBeValidIfDomainMatches() {
    val emailDomain = EmailDomain(id = UUID.randomUUID(), name = "%validdomain.com", description = "description")
    whenever(emailDomainRepository.findByNameLike("validdomain.com")).thenReturn(emailDomain)

    assertEquals(true, verifyService.isValidEmailDomain("validDomain.com"))
  }

  @Test
  fun shouldNotBeValidIfDomainDoesntMatch() {
    whenever(emailDomainRepository.findByNameLike("validdomain.com")).thenReturn(null)

    assertEquals(false, verifyService.isValidEmailDomain("validDomain.com"))
  }
}
