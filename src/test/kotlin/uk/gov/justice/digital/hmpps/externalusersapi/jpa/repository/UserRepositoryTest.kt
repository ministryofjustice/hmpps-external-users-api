package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

@ActiveProfiles("test")
@DataR2dbcTest
@WithMockUser
class UserRepositoryTest {
  @Autowired
  private lateinit var repository: UserRepository

  @Test
  fun findByUsernameIsTrue(): Unit = runBlocking {
    assertThat(repository.findByUsername("AUTH_TEST", AuthSource.auth).awaitSingle()).isNotNull
  }

  @Test
  fun findByUsernameIsFalse(): Unit = runBlocking {
    assertThat(repository.findByUsername("DOES_NOT_EXIST", AuthSource.auth).awaitSingleOrNull()).isNull()
  }
}
