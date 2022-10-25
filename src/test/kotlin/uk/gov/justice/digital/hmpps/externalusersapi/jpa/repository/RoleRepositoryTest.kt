package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@DataR2dbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Transactional
class RoleRepositoryTest {
  @Autowired
  private lateinit var repository: RoleRepository

  // @Nested
  // inner class FindAllRoles {
  //   @Test
  //   fun `findAllByOrderByRoleNameLike EXT_ADM`() {
  //     assertThat(repository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
  //       .contains("ROLE_GLOBAL_SEARCH", "ROLE_PECS_POLICE")
  //   }
  //
  //   @Test
  //   fun `findAllByOrderByRoleNameLike DPS_ADM`() {
  //     assertThat(repository.findAllByOrderByRoleNameLike(AdminType.DPS_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
  //       .contains("ROLE_GLOBAL_SEARCH", "ROLE_UNIT_TEST_DPS_ROLE")
  //   }
  //
  //   @Test
  //   fun `findAllByOrderByRoleNameLike EXT_ADM does not contain DPS Roles`() {
  //     assertThat(repository.findAllByOrderByRoleNameLike(AdminType.EXT_ADM.adminTypeCode)).extracting<String> { obj: Authority -> obj.authority }
  //       .doesNotContain("ROLES_TEST_DPS")
  //   }
  // }
/*
  @Nested
  inner class FindByRoleCode {
    @Test
    fun givenAnExistingRoleTheyCanBeRetrieved() {
      assertThat(repository.findByRoleCode("GLOBAL_SEARCH")).extracting("roleName")
        .isEqualTo("Global Search")
    }
    @Test
    fun givenANonExistentRoleTheyCantBeRetrieved() {
      assertThat(repository.findByRoleCode("DOES_NOT_EXIST")).isNull()
    }
  }

 */
}
