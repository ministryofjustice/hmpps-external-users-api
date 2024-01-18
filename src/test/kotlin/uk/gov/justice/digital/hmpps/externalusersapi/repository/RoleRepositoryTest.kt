package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType

@SpringBootTest
@AutoConfigureDataR2dbc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class RoleRepositoryTest {
  @Autowired
  private lateinit var repository: RoleRepository

  @Nested
  inner class FindAllRoles {
    @Test
    fun `findByAdminTypeContainingOrderByRoleName EXT_ADM`(): Unit = runBlocking {
      assertThat(
        repository.findByAdminTypeContainingOrderByRoleName(AdminType.EXT_ADM.adminTypeCode).toList(),
      )
        .extracting<String>
        { obj: Authority -> obj.roleCode }
        .contains("GLOBAL_SEARCH", "PECS_POLICE")
    }

    @Test
    fun `findByAdminTypeLikeOrderByRoleName DPS_ADM`(): Unit = runBlocking {
      assertThat(repository.findByAdminTypeContainingOrderByRoleName(AdminType.DPS_ADM.adminTypeCode).toList())
        .extracting<String>
        { obj: Authority -> obj.roleCode }
        .contains("GLOBAL_SEARCH", "UNIT_TEST_DPS_ROLE")
    }

    @Test
    fun `findByAdminTypeLikeOrderByRoleName EXT_ADM does not contain DPS Roles`(): Unit = runBlocking {
      assertThat(repository.findByAdminTypeContainingOrderByRoleName(AdminType.EXT_ADM.adminTypeCode).toList())
        .extracting<String>
        { obj: Authority -> obj.roleCode }
        .doesNotContain("TEST_DPS")
    }
  }

  @Nested
  inner class FindAutomaticGroupRolesByGroupCode {
    @Test
    fun givenAnExistingRoleTheyCanBeRetrieved(): Unit = runBlocking {
      assertThat(repository.findAutomaticGroupRolesByGroupCode("SITE_1_GROUP_2").toList()).size()
        .isEqualTo(2)
    }

    @Test
    fun givenAnNonAutomaticGroupRoleTheyCantBeRetrieved(): Unit = runBlocking {
      // SITE_1_GROUP_1 (have total 3 roles associated) have 2 automatic set to 'true' and 1 set to 'false'
      assertThat(repository.findAutomaticGroupRolesByGroupCode("SITE_1_GROUP_1").toList()).size()
        .isEqualTo(2)
    }

    @Test
    fun givenAnUnassociatedRoleToGroupTheyCantBeRetrieved(): Unit = runBlocking {
      // LICENCE_DM role not associated with any group
      assertThat(repository.findAutomaticGroupRolesByGroupCode("LICENCE_DM").toList()).isEmpty()
    }

    @Test
    fun givenANonExistentRoleTheyCantBeRetrieved(): Unit = runBlocking {
      assertThat(repository.findAutomaticGroupRolesByGroupCode("DOES_NOT_EXIST").toList()).isEmpty()
    }
  }

  @Nested
  inner class FindByRoleCode {
    @Test
    fun givenAnExistingRoleTheyCanBeRetrieved(): Unit = runBlocking {
      assertThat(repository.findByRoleCode("GLOBAL_SEARCH")).extracting("roleName")
        .isEqualTo("Global Search")
    }

    @Test
    fun givenANonExistentRoleTheyCantBeRetrieved(): Unit = runBlocking {
      assertThat(repository.findByRoleCode("DOES_NOT_EXIST")).isNull()
    }
  }

  /*
  @Nested
  inner class FindByGroupAssignableRoles() {

    @Test
    fun findByGroupAssignableRolesForUsername() {
      assertThat(repository.findByGroupAssignableRolesForUsername("AUTH_RO_VARY_USER")).extracting<String> { obj: Authority -> obj.roleCode }
        .containsExactly("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY")
    }

    @Test
    fun findByGroupAssignableRolesForUserId() {
      assertThat(
        repository.findByGroupAssignableRolesForUserId(
          UUID.fromString("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8")
        )
      ).extracting<String> { obj: Authority -> obj.roleCode }
        .containsExactly("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY")
    }
  }


  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = Authority(roleCode = transientEntity.authority, roleName = transientEntity.roleName, adminType = transientEntity.adminType)
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.authority).isNotNull
    TestTransaction.start()
    val retrievedEntity = repository.findByRoleCode(entity.roleCode) ?: throw
    AuthUserRoleException("role", "role.notfound")

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.authority).isEqualTo(transientEntity.authority)
    assertThat(retrievedEntity.roleName).isEqualTo(transientEntity.roleName)
    assertThat(retrievedEntity.adminType).isEqualTo(transientEntity.adminType)

    // clear up to prevent subsequent tests failing
    repository.delete(retrievedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
  }
  private fun transientEntity() = Authority(roleCode = "hdc", roleName = "Licences", adminType = listOf(AdminType.EXT_ADM))

   */
}
