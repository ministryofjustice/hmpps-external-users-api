package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional
class GroupsRepositoryTest {

  @Autowired
  private lateinit var repository: GroupRepository

  @Nested
  inner class FindGroup {

    @Test
    fun givenATransientEntityItCanBePersisted() {
      val transientEntity = transientEntity()
      val entity = Group(transientEntity.groupCode, transientEntity.groupName)
      val persistedEntity = repository.save(entity)
      TestTransaction.flagForCommit()
      TestTransaction.end()
      assertThat(persistedEntity.groupCode).isNotNull()
      TestTransaction.start()
      val retrievedEntity = repository.findByGroupCode(entity.groupCode)

      // equals only compares the business key columns
      assertThat(retrievedEntity).isEqualTo(transientEntity)
      assertThat(retrievedEntity?.groupCode).isEqualTo(transientEntity.groupCode)
      assertThat(retrievedEntity?.groupName).isEqualTo(transientEntity.groupName)
    }

    @Test
    fun givenAnExistingGroupTheyCanBeRetrieved() {
      val group = repository.findByGroupCode("SITE_1_GROUP_2")
      assertThat(group?.groupCode).isEqualTo("SITE_1_GROUP_2")
      assertThat(group?.groupName).isEqualTo("Site 1 - Group 2")
      assertThat(group?.assignableRoles).extracting<String> { it.role.roleCode }
        .containsOnly("GLOBAL_SEARCH", "LICENCE_RO")
      assertThat(group?.children).extracting<String> { it.groupCode }
        .containsOnly("CHILD_1")
    }
    @Test
    fun findAllByOrderByGroupName() {
      assertThat(repository.findAllByOrderByGroupName()).extracting<String> { it.groupCode }
        .containsSequence("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_2_GROUP_1", "SITE_3_GROUP_1")
    }

    private fun transientEntity(): Group = Group("hdc", "Licences")
  }
}
