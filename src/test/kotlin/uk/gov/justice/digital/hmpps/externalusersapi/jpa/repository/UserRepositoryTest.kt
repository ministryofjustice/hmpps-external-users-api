package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthDbConfig

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional
class UserRepositoryTest {
  @Autowired
  private lateinit var repository: UserRepository

  @Test
  fun findByUsernameIsTrue() {
    assertThat(repository.findByUsername("AUTH_TEST")).isPresent
  }

  @Test
  fun findByUsernameIsFalse() {
    assertThat(repository.findByUsername("DOES_NOT_EXIST")).isNotPresent
  }
}
