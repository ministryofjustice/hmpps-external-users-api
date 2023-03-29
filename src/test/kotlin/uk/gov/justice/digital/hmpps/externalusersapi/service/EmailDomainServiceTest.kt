package uk.gov.justice.digital.hmpps.externalusersapi.service

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.externalusersapi.repository.EmailDomainRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.EmailDomain
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailDomainDto
import java.util.UUID

class EmailDomainServiceTest {
  private val emailDomainRepository: EmailDomainRepository = mock()
  private val emailDomainExclusions: EmailDomainExclusions = mock()
  private val newDomain = CreateEmailDomainDto("123.co.uk", "test")

  private val service = EmailDomainService(emailDomainRepository, emailDomainExclusions)

  @Test
  fun shouldRetrieveEmailDomainList(): Unit = runBlocking {
    val randomUUID = UUID.randomUUID()
    val randomUUID2 = UUID.randomUUID()
    val randomUUID3 = UUID.randomUUID()

    whenever(emailDomainRepository.findAll()).thenReturn(
      flowOf(
        EmailDomain(id = randomUUID, name = "acc.com", description = "description"),
        EmailDomain(id = randomUUID2, name = "%adc.com", description = "description"),
        EmailDomain(id = randomUUID3, name = "%.abc.com", description = "description"),
      ),
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
  fun shouldNotAddDomainWhenAlreadyPresent(): Unit = runBlocking {
    whenever(emailDomainRepository.findByName("%" + newDomain.name)).thenReturn(
      EmailDomain(
        name = newDomain.name,
        description = newDomain.description,
      ),
    )

    assertThatThrownBy { runBlocking { service.addDomain(newDomain) } }
      .isInstanceOf(EmailDomainAdditionBarredException::class.java)
      .hasMessage("Unable to add email domain: Email domain ${newDomain.name} already exists")

    verify(emailDomainRepository, never()).save(any())
    verifyNoInteractions(emailDomainExclusions)
  }

  @Test
  fun shouldNotAddDomainWhenExcluded(): Unit = runBlocking {
    whenever(emailDomainExclusions.contains(newDomain.name)).thenReturn(true)
    assertThatThrownBy { runBlocking { service.addDomain(newDomain) } }
      .isInstanceOf(EmailDomainAdditionBarredException::class.java)
      .hasMessage("Unable to add email domain: Email domain ${newDomain.name} is present in the excluded list")

    verify(emailDomainRepository, never()).save(any())
  }

  @Test
  fun shouldPersistDomainWithAddedPercentPrefixWhenDomainNotAlreadyPresentOrExcluded(): Unit = runBlocking {
    whenever(emailDomainRepository.save(any())).thenReturn(EmailDomain(UUID.randomUUID(), newDomain.name, newDomain.description))
    service.addDomain(newDomain)

    val emailDomainCaptor = argumentCaptor<EmailDomain>()
    verify(emailDomainRepository).save(emailDomainCaptor.capture())
    val actualEmailDomain = emailDomainCaptor.firstValue

    assertEquals("%" + newDomain.name, actualEmailDomain.name)
    assertEquals(newDomain.description, actualEmailDomain.description)
  }

  @Test
  fun shouldNotAddPercentPrefixWhenDomainNameAlreadyHasPercentPrefix(): Unit = runBlocking {
    val domain = CreateEmailDomainDto("%123.co.uk", "test")
    whenever(emailDomainRepository.save(any())).thenReturn(EmailDomain(UUID.randomUUID(), domain.name, domain.description))
    service.addDomain(domain)

    val emailDomainCaptor = argumentCaptor<EmailDomain>()
    verify(emailDomainRepository).save(emailDomainCaptor.capture())
    val actualEmailDomain = emailDomainCaptor.firstValue

    assertEquals(domain.name, actualEmailDomain.name)
    assertEquals(domain.description, actualEmailDomain.description)
  }

  @Test
  fun shouldNotRemoveDomainWhenDomainNotPresent(): Unit = runBlocking {
    val id = UUID.randomUUID()
    whenever(emailDomainRepository.findById(id)).thenReturn(null)

    assertThatThrownBy { runBlocking { service.removeDomain(id) } }
      .isInstanceOf(EmailDomainNotFoundException::class.java)
      .hasMessage("Unable to delete email domain id: $id with reason: notfound")

    verify(emailDomainRepository, never()).deleteById(id)
  }

  @Test
  fun shouldRemoveDomainWhenDomainPresent(): Unit = runBlocking {
    val randomUUID = UUID.randomUUID()
    val emailDomain = EmailDomain(randomUUID, "abc.com")
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(emailDomain)

    service.removeDomain(randomUUID)

    verify(emailDomainRepository).delete(emailDomain)
  }

  @Test
  fun shouldRetrieveDomain(): Unit = runBlocking {
    val randomUUID = UUID.randomUUID()
    val id = randomUUID.toString()
    val emailDomain = EmailDomain(randomUUID, "%.abc.com", "Description")
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(emailDomain)

    val actualDomain = service.domain(randomUUID)
    val expectedDomain = EmailDomainDto(id, "abc.com", "Description")

    assertEquals(expectedDomain, actualDomain)
  }

  @Test
  fun shouldThrowEmailDomainNotFoundExceptionWhenDomainNotFound(): Unit = runBlocking {
    val randomUUID = UUID.randomUUID()
    val id = randomUUID.toString()
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(null)

    assertThatThrownBy { runBlocking { service.domain(randomUUID) } }
      .isInstanceOf(EmailDomainNotFoundException::class.java)
      .hasMessage("Unable to retrieve email domain id: $id with reason: notfound")
  }
}
