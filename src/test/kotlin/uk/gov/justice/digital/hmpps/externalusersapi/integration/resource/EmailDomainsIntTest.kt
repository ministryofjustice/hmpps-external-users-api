package uk.gov.justice.digital.hmpps.externalusersapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.EmailDomainRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import java.util.Optional
import java.util.UUID

class EmailDomainsIntTest : IntegrationTestBase() {

  @MockBean
  private lateinit var emailDomainRepository: EmailDomainRepository

  @Test
  fun `should retrieve email domain list`() {

    whenever(emailDomainRepository.findAll()).thenReturn(
      listOf(
        emailDomain(UUID.randomUUID(), "%advancecharity.org.uk", "ADVANCE"),
        emailDomain(UUID.randomUUID(), "%bidvestnoonan.com", "BIDVESTNOONA"),
      )
    )

    webTestClient
      .get().uri("/email-domains")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("[*].domain").value<List<String>> {
        assertThat(it).containsExactlyElementsOf(listOf("advancecharity.org.uk", "bidvestnoonan.com"))
      }
  }

  @Test
  fun `should retrieve single email domain`() {
    val id = UUID.randomUUID()
    whenever(emailDomainRepository.findById(id)).thenReturn(
      Optional.of(
        emailDomain(
          id,
          "%advancecharity.org.uk",
          "ADVANCE"
        )
      )
    )
    webTestClient
      .get().uri("/email-domains/$id")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("domain").value<String> {
        assertThat(it).isEqualTo("advancecharity.org.uk")
      }
      .jsonPath("description").value<String> {
        assertThat(it).isEqualTo("ADVANCE")
      }
  }

  @Test
  fun `should add email domain`() {
    val newDomain = EmailDomain(UUID.randomUUID(), "%bsigroup.com", "BSIGROUP")
    whenever(emailDomainRepository.save(any())).thenReturn(newDomain)
    webTestClient.post()
      .uri("/email-domains")
      .accept(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS"),
          scopes = listOf("write"),
        )
      )
      .body(
        BodyInserters.fromValue(
          mapOf(
            "name" to "%bsigroup.com",
            "description" to "BSIGROUP",
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("id").value<String> {
        assertThat(it).isEqualTo(newDomain.id.toString())
      }
      .jsonPath("domain").value<String> {
        assertThat(it).isEqualTo("bsigroup.com")
      }
      .jsonPath("description").value<String> {
        assertThat(it).isEqualTo("BSIGROUP")
      }
  }

  @Test
  fun `should respond with bad request on attempt to add invalid email domain`() {
    webTestClient.post()
      .uri("/email-domains")
      .accept(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS"),
          scopes = listOf("write"),
        )
      )
      .body(
        BodyInserters.fromValue(
          mapOf(
            "name" to "%.com",
            "description" to "BSIGROUP",
          )
        )
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `should respond with bad request on attempt to retrieve email domain with invalid identifier`() {
    val invalidId = 3
    webTestClient
      .get().uri("/email-domains/$invalidId")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it["status"] as Int).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(it["userMessage"] as String).startsWith("Validation failure: Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")
        assertThat(it["developerMessage"] as String).startsWith("Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")
      }
  }

  @Test
  fun `should respond with conflict on attempt to add email domain already present`() {
    val existingDomain = EmailDomain(UUID.randomUUID(), "bsigroup.com", "BSIGROUP")
    whenever(emailDomainRepository.findByName("%bsigroup.com")).thenReturn(existingDomain)
    webTestClient.post()
      .uri("/email-domains")
      .accept(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS"),
          scopes = listOf("write"),
        )
      )
      .body(
        BodyInserters.fromValue(
          mapOf(
            "name" to "%bsigroup.com",
            "description" to "BSIGROUP",
          )
        )
      )
      .exchange()
      .expectStatus().isEqualTo(409)
  }

  @Test
  fun `should delete email domain`() {
    val randomUUID = UUID.randomUUID()
    val emailDomain = EmailDomain(randomUUID, "%test.com", "TEST")
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(Optional.of(emailDomain))

    webTestClient.delete()
      .uri("/email-domains/$randomUUID")
      .headers(
        setAuthorisation(
          roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS"),
          scopes = listOf("write"),
        )
      )
      .exchange()
      .expectStatus().isOk

    verify(emailDomainRepository).delete(emailDomain)
  }

  @Test
  fun `should respond with not found on attempt to delete email domain that is not present`() {
    val randomUUID = UUID.randomUUID()
    whenever(emailDomainRepository.findById(randomUUID)).thenReturn(Optional.empty())

    webTestClient.delete()
      .uri("/email-domains/$randomUUID")
      .headers(
        setAuthorisation(
          roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS"),
          scopes = listOf("write"),
        )
      )
      .exchange()
      .expectStatus().isNotFound

    verify(emailDomainRepository, never()).delete(any())
  }

  private fun emailDomain(id: UUID, name: String, description: String): EmailDomain {
    return EmailDomain(id, name, description)
  }
}
