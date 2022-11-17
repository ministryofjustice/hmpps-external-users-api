package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.toList
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
    assertThat(repository.findByUsernameAndSource("AUTH_TEST", AuthSource.auth)).isNotNull
  }

  @Test
  fun findByUsernameIsFalse(): Unit = runBlocking {
    assertThat(repository.findByUsernameAndSource("DOES_NOT_EXIST", AuthSource.auth)).isNull()
  }

  @Test
  fun findAllByGroupCodeNoMatch(): Unit = runBlocking {
    assertThat(repository.findAllByGroupCode("SITE_9_GROUP_1").toList()).isEmpty()
  }

  @Test
  fun findAllByGroupCode(): Unit = runBlocking {
    assertThat(repository.findAllByGroupCode("SITE_1_GROUP_1").toList()).isNotEmpty
  }
}
