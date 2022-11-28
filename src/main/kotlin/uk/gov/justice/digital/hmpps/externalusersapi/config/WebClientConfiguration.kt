package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.manage-users}") val manageUsersBaseUri: String,
) {

  @Bean
  fun manageUsersWebClient(): WebClient {
    val httpClient = HttpClient.create().responseTimeout(Duration.ofMinutes(2))
    return WebClient.builder()
      .baseUrl(manageUsersBaseUri)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .filter(AuthTokenFilterFunction())
      .build()
  }
}
