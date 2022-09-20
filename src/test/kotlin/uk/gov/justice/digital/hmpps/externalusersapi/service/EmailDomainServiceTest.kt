package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.EmailDomainExclusions
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.EmailDomainRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailDomainDto
import java.util.Optional
import java.util.UUID

class EmailDomainServiceTest {
  private val emailDomainRepository: EmailDomainRepository = mock()
  private val emailDomainExclusions: EmailDomainExclusions = mock()
  private val newDomain = CreateEmailDomainDto("123.co.uk", "test")

  private val service = EmailDomainService(emailDomainRepository, emailDomainExclusions)

  @Test
  fun shouldRetrieveEmailDomainList() {
    val randomUUID = UUID.randomUUID()
    val randomUUID2 = UUID.randomUUID()
    val randomUUID3 = UUID.randomUUID()

    whenever(emailDomainRepository.findAll()).thenReturn(
      listOf(
        EmailDomain(id = randomUUID, name = "acc.com", description = "description"),
        EmailDomain(id = randomUUID2, name = "%adc.com", description = "description"),
        EmailDomain(id = randomUUID3, name = "%.abc.com", description = "description"),
      )
    )

    val actualEmailDomainList = service.domainList()

    val expectedDomainList = listOf(
      EmailDomainDto(randomUUID3.toString(), "abc.com", "description"),
      EmailDomainDto(randomUUID.toString(), "acc.com", "description"),
      EmailDomainDto(randomUUID2.toString(), "adc.com", "description"),
    )

    assertEquals(expectedDomainList, actualEmailDomainList)
  }

  @Test
  fun shouldNotAddDomainWhenAlreadyPresent() {
    whenever(emailDomainRepository.findByName("%" + newDomain.name)).thenReturn(
      EmailDomain(
        name = newDomain.name,
        description = newDomain.description
      )
    )

    assertThatThrownBy { service.addDomain(newDomain) }
      .isInstanceOf(EmailDomainAdditionBarredException::class.java)
      .hasMessage("Unable to add email domain: ${newDomain.name} to allowed list with reason: domain already present in allowed list")

    verify(emailDomainRepository, never()).save(any())
    verifyNoInteractions(emailDomainExclusions)
  }

  @Test
  fun shouldNotAddDomainWhenExcluded() {
    whenever(emailDomainExclusions.contains(newDomain.name)).thenReturn(true)
    assertThatThrownBy { service.addDomain(newDomain) }
      .isInstanceOf(EmailDomainAdditionBarredException::class.java)
      .hasMessage("Unable to add email domain: ${newDomain.name} to allowed list with reason: domain present in excluded list")

    verify(emailDomainRepository, never()).save(any())
  }

  @Test
  fun shouldPersistDomainWithAddedPercentPrefixWhenDomainNotAlreadyPresentOrExcluded() {

    service.addDomain(newDomain)

    val emailDomainCaptor = argumentCaptor<EmailDomain>()
    verify(emailDomainRepository).save(emailDomainCaptor.capture())
    val actualEmailDomain = emailDomainCaptor.firstValue

    assertEquals("%" + newDomain.name, actualEmailDomain.name)
    assertEquals(newDomain.description, actualEmailDomain.description)
  }

  @Test
  fun shouldNotAddPercentPrefixWhenDomainNameAlreadyHasPercentPrefix() {

    val domain = CreateEmailDomainDto("%123.co.uk", "test")
    service.addDomain(domain)

    val emailDomainCaptor = argumentCaptor<EmailDomain>()
    verify(emailDomainRepository).save(emailDomainCaptor.capture())
    val actualEmailDomain = emailDomainCaptor.firstValue

    assertEquals(domain.name, actualEmailDomain.name)
    assertEquals(domain.description, actualEmailDomain.description)
  }

  @Test
  fun shouldNotRemoveDomainWhenDomainNotPresent() {
    val id = UUID.randomUUID()
    whenever(emailDomainRepository.findById(id)).thenReturn(Optional.empty())

    assertThatThrownBy { service.removeDomain(id) }
      .isInstanceOf(EmailDomainNotFoundException::class.java)
      .hasMessage("Unable to delete email domain id: $id with reason: notfound")

    verify(emailDomainRepository, never()).deleteById(id)
  }

  @Test
  fun shouldRemoveDomainWhenDomainPresent() {
    val randomUUID = UUID.randomUUID()
    val emailDomain = EmailDomain(randomUUID, "abc.com")
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(Optional.of(emailDomain))

    service.removeDomain(randomUUID)

    verify(emailDomainRepository).delete(emailDomain)
  }

  @Test
  fun shouldRetrieveDomain() {
    val randomUUID = UUID.randomUUID()
    val id = randomUUID.toString()
    val emailDomain = EmailDomain(randomUUID, "%.abc.com", "Description")
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(Optional.of(emailDomain))

    val actualDomain = service.domain(randomUUID)
    val expectedDomain = EmailDomainDto(id, "abc.com", "Description")

    assertEquals(expectedDomain, actualDomain)
  }

  @Test
  fun shouldThrowEmailDomainNotFoundExceptionWhenDomainNotFound() {
    val randomUUID = UUID.randomUUID()
    val id = randomUUID.toString()
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(Optional.empty())

    assertThatThrownBy { service.domain(randomUUID) }
      .isInstanceOf(EmailDomainNotFoundException::class.java)
      .hasMessage("Unable to retrieve email domain id: $id with reason: notfound")
  }
}
