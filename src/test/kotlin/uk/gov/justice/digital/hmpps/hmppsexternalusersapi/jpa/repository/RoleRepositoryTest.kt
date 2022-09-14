package uk.gov.justice.digital.hmpps.hmppsexternalusersapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.hmppsexternalusersapi.model.Authority

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional
class RoleRepositoryTest {
  @Autowired
  private lateinit var repository: RoleRepository

  @Test
  fun `findAllByOrderByRoleNameLike EXT_ADM`() {
    assertThat(repository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
      .contains("ROLE_GLOBAL_SEARCH", "ROLE_PECS_POLICE")
  }

  @Test
  fun `findAllByOrderByRoleNameLike DPS_ADM`() {
    assertThat(repository.findAllByOrderByRoleNameLike(AdminType.DPS_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
      .contains("ROLE_GLOBAL_SEARCH", "ROLE_UNIT_TEST_DPS_ROLE")
  }

  @Test
  fun `findAllByOrderByRoleNameLike EXT_ADM does not contain DPS Roles`() {
    assertThat(repository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
      .doesNotContain("ROLES_TEST_DPS")
  }
}
