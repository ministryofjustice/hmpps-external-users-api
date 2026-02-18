package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.AutoConfigureDataR2dbc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import java.util.Random
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureDataR2dbc
@WithMockUser
class GroupsRepositoryTest {

  @Autowired
  private lateinit var repository: GroupRepository

  @Nested
  inner class FindGroup {

    @Test
    fun givenATransientEntityItCanBePersisted(): Unit = runBlocking {
      val groupCode = "groupCode" + Random().nextInt()
      val groupName = "groupName" + Random().nextInt()

      val entity = Group(groupCode = groupCode, groupName = groupName)
      val persistedEntity = repository.save(entity)

      assertThat(persistedEntity.groupCode).isNotNull()

      val retrievedEntity = repository.findByGroupCode(entity.groupCode)

      // equals only compares the business key columns
      assertThat(retrievedEntity?.groupCode).isEqualTo(groupCode)
      assertThat(retrievedEntity?.groupName).isEqualTo(groupName)
    }

    @Test
    fun givenAnExistingGroupTheyCanBeRetrieved(): Unit = runBlocking {
      val group = repository.findByGroupCode("SITE_1_GROUP_2")
      assertThat(group?.groupCode).isEqualTo("SITE_1_GROUP_2")
      assertThat(group?.groupName).isEqualTo("Site 1 - Group 2")
      assertThat(group?.groupId).isNotNull()
    }

    @Test
    fun findAllByOrderByGroupName(): Unit = runBlocking {
      assertThat(repository.findAllByOrderByGroupName().toList()).extracting<String> { it.groupCode }
        .containsSequence("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_1_GROUP_3", "SITE_2_GROUP_1", "SITE_3_GROUP_1")
    }

    @Test
    fun findGroupsByUserId(): Unit = runBlocking {
      val uuid = UUID.fromString("1f650f15-0993-4db7-9a32-5b930ff86035")
      assertThat(repository.findGroupsByUserId(uuid).toList()).extracting<String> { it.groupCode }
        .contains("SITE_1_GROUP_1", "SITE_1_GROUP_2")
    }

    @Test
    fun findGroupsByUserName(): Unit = runBlocking {
      assertThat(repository.findGroupsByUsername("AUTH_GROUP_MANAGER").toList()).extracting<String> { it.groupCode }
        .containsSequence("SITE_1_GROUP_1", "SITE_1_GROUP_2")
    }
  }
}
