package uk.gov.justice.digital.hmpps.externalusersapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.externalusersapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.externalusersapi.integration.wiremock.HmppsAuthMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(JwtAuthHelper::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper

  companion object {

    @JvmField
    internal val hmppsAuthMockServer = HmppsAuthMockServer()

    @Suppress("unused")
    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()
    }

    @Suppress("unused")
    @AfterAll
    @JvmStatic
    fun stopMocks() {
      hmppsAuthMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }
  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)
}
