package uk.gov.justice.digital.hmpps.externalusersapi.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

abstract class HealthCheck(private val webClient: WebClient, private val timeout: Duration) : HealthIndicator {
  override fun health(): Health = try {
    val responseEntity = webClient.get()
      .uri("/health/ping")
      .retrieve()
      .toEntity(String::class.java)
      .block(timeout)
    Health.up().withDetail("HttpStatus", responseEntity?.statusCode ?: "UNKNOWN").build()
  } catch (e: WebClientResponseException) {
    Health.down(e).withDetail("body", e.responseBodyAsString).build()
  } catch (e: Exception) {
    Health.down(e).build()
  }
}
