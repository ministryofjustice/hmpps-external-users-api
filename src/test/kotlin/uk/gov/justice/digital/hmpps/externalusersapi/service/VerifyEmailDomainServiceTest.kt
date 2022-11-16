package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.repository.EmailDomainRepository

class VerifyEmailDomainServiceTest {
  private val emailDomainRepository: EmailDomainRepository = mock()
  private val verifyService = VerifyEmailDomainService(emailDomainRepository)

  @Test
  fun shouldBeValidIfDomainMatches(): Unit = runBlocking {
    whenever(emailDomainRepository.countMatching("validdomain.com")).thenReturn(1)

    assertEquals(true, verifyService.isValidEmailDomain("validDomain.com"))
  }

  @Test
  fun shouldBeValidWhenMoreThanOneMatch(): Unit = runBlocking {
    whenever(emailDomainRepository.countMatching("validdomain.com")).thenReturn(2)

    assertEquals(true, verifyService.isValidEmailDomain("validDomain.com"))
  }

  @Test
  fun shouldNotBeValidIfDomainDoesntMatch(): Unit = runBlocking {
    whenever(emailDomainRepository.countMatching("invaliddomain.com")).thenReturn(0)

    assertEquals(false, verifyService.isValidEmailDomain("invalidDomain.com"))
  }
}
