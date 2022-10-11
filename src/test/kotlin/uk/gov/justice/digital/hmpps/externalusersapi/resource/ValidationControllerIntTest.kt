package uk.gov.justice.digital.hmpps.externalusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalusersapi.integration.IntegrationTestBase

class ValidationControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class EmailDomain {

    @Test
    fun `Should fail for invalid email domain`() {
      val isValid = webTestClient
        .get().uri("/validate/email-domain?emailDomain=invaliddomain.com")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_CREATE_EMAIL_TOKEN")))
        .exchange()
        .expectStatus().isOk
        .expectBody<Boolean>()
        .returnResult().responseBody

      if (isValid != null) {
        assertThat(isValid).isFalse
      }
    }
  }

  @Test
  fun `Validate email matching domain`() {
    val isValid = webTestClient
      .get().uri("/validate/email-domain?emailDomain=careuk.com")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_CREATE_EMAIL_TOKEN")))
      .exchange()
      .expectStatus().isOk
      .expectBody<Boolean>()
      .returnResult().responseBody

    if (isValid != null) {
      assertThat(isValid).isTrue
    }
  }

  @Test
  fun `Validate email domain matching subdomain`() {
    val isValid = webTestClient
      .get().uri("/validate/email-domain?emailDomain=subdomain.careuk.com")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_CREATE_EMAIL_TOKEN")))
      .exchange()
      .expectStatus().isOk
      .expectBody<Boolean>()
      .returnResult().responseBody

    if (isValid != null) {
      assertThat(isValid).isTrue
    }
  }
}
