package uk.gov.justice.digital.hmpps.externalusersapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalusersapi.config.SecurityUserContext
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainService
import java.util.UUID

class EmailDomainControllerTest {
  private val securityUserContext: SecurityUserContext = mock()
  private val emailDomainService: EmailDomainService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val emailDomains: List<EmailDomainDto> = mock()
  private val controller = EmailDomainController(emailDomainService, telemetryClient, securityUserContext)

  @Test
  fun shouldRespondWithEmailDomainsRetrieved() {
    whenever(emailDomainService.domainList()).thenReturn(emailDomains)

    val emailDomains = controller.domainList()

    assertEquals(emailDomains, this.emailDomains)
    verifyNoInteractions(this.emailDomains)
  }

  @Test
  fun shouldAddEmailDomain() {
    val newEmailDomain = CreateEmailDomainDto("%123.co.uk", "test")
    controller.addEmailDomain(newEmailDomain)
    verify(emailDomainService).addDomain(newEmailDomain)
  }

  @Test
  fun shouldDeleteEmailDomain() {
    val uuid = UUID.randomUUID()

    controller.deleteEmailDomain(uuid)

    verify(emailDomainService).removeDomain(uuid)
  }

  @Test
  fun shouldRecordEmailDomainCreateSuccessEvent() {
    whenever(securityUserContext.principal).thenReturn("Fred")
    val eventDetails = argumentCaptor<Map<String, String>>()
    val newEmailDomain = CreateEmailDomainDto("%123.co.uk", "test")

    controller.addEmailDomain(newEmailDomain)

    verify(telemetryClient).trackEvent(eq("EmailDomainCreateSuccess"), eventDetails.capture(), anyOrNull())
    assertEquals("Fred", eventDetails.firstValue.getValue("username"))
    assertEquals("%123.co.uk", eventDetails.firstValue.getValue("domain"))
  }

  @Test
  fun shouldRecordEmailDomainDeleteSuccessEvent() {
    whenever(securityUserContext.principal).thenReturn("Fred")
    val eventDetails = argumentCaptor<Map<String, String>>()
    val id = UUID.randomUUID()

    controller.deleteEmailDomain(id)

    verify(telemetryClient).trackEvent(eq("EmailDomainDeleteSuccess"), eventDetails.capture(), anyOrNull())
    assertEquals("Fred", eventDetails.firstValue.getValue("username"))
    assertEquals(id.toString(), eventDetails.firstValue.getValue("id"))
  }
}
