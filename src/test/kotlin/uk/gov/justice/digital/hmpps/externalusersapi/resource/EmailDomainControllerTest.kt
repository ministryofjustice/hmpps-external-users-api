package uk.gov.justice.digital.hmpps.externalusersapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.validation.BindingResult
import uk.gov.justice.digital.hmpps.externalusersapi.config.SecurityUserContext
import uk.gov.justice.digital.hmpps.externalusersapi.service.EmailDomainService
import java.util.UUID

class EmailDomainControllerTest {
  private val securityUserContext: SecurityUserContext = mock()
  private val emailDomainService: EmailDomainService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val result: BindingResult = mock()
  private val emailDomains: List<EmailDomainDto> = mock()
  private val id1 = UUID.randomUUID()
  private val controller = EmailDomainController(emailDomainService, telemetryClient, securityUserContext)

  @Test
  fun shouldRespondWithEmailDomainsRetrieved() {
    whenever(emailDomainService.domainList()).thenReturn(emailDomains)

    val modelAndView = controller.domainList()

    assertTrue(modelAndView.hasView())
    assertEquals(modelAndView.viewName, "ui/emailDomains")
    assertEquals(modelAndView.model["emailDomains"], emailDomains)
    verifyNoInteractions(emailDomains)
  }

  @Test
  fun shouldAddEmailDomain() {
    val newEmailDomain = CreateEmailDomainDto("%123.co.uk", "test")
    controller.addEmailDomain(newEmailDomain, result)
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

    controller.addEmailDomain(newEmailDomain, result)

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

  @Test
  fun shouldRedirectToDomainListOnSuccessfulAdd() {
    val newEmailDomain = CreateEmailDomainDto("%123.co.uk", "test")

    val modelAndView = controller.addEmailDomain(newEmailDomain, result)

    assertTrue(modelAndView.hasView())
    assertEquals(modelAndView.viewName, "redirect:/email-domains")
  }

  @Test
  fun shouldReturnToAddEmailDomainFormOnValidationErrors() {
    whenever(result.hasErrors()).thenReturn(true)
    val newEmailDomain = CreateEmailDomainDto("%123.co.uk", "test")

    val modelAndView = controller.addEmailDomain(newEmailDomain, result)

    assertTrue(modelAndView.hasView())
    assertEquals(modelAndView.viewName, "ui/newEmailDomainForm")
    assertEquals(modelAndView.model["createEmailDomainDto"], newEmailDomain)
  }

  @Test
  fun shouldRedirectToDomainListOnSuccessfulDelete() {
    val id = UUID.randomUUID()

    val modelAndView = controller.deleteEmailDomain(id)

    assertTrue(modelAndView.hasView())
    assertEquals(modelAndView.viewName, "redirect:/email-domains")
  }

  @Test
  fun shouldRouteToDeleteConfirm() {
    whenever(emailDomainService.domain(id1)).thenReturn(EmailDomainDto(id1.toString(), "advancecharity.org.uk", "Description"))

    val modelAndView = controller.deleteConfirm(id1)

    assertTrue(modelAndView.hasView())
    assertEquals(modelAndView.viewName, "ui/deleteEmailDomainConfirm")
    assertEquals(EmailDomainDto(id1.toString(), "advancecharity.org.uk", "Description"), modelAndView.model["emailDomain"])
  }
}