package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.externalusersapi.resource.EmailDomainDto

@DataR2dbcTest
@ActiveProfiles("test")
class EmailDomainRepositoryTest {
  @Autowired
  private lateinit var repository: EmailDomainRepository

  @Test
  fun canBePersisted(): Unit = runBlocking {
    val persistedEntity = repository.save(EmailDomain(id = null, name = "anyolddomain.uk", description = "some description"))
    val retrievedEntity = repository.findById(persistedEntity.id!!) ?: throw RuntimeException("${persistedEntity.id} not found")

    assertThat(retrievedEntity.name).isEqualTo(persistedEntity.name)
    assertThat(retrievedEntity.description).isEqualTo(persistedEntity.description)
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved(): Unit = runBlocking {
    val allEmailDomains = repository.findAll()
    val emailDomainDtoList = allEmailDomains.map { emailDomain ->
      EmailDomainDto(
        emailDomain.id.toString(),
        emailDomain.name,
        emailDomain.description.toString()
      )
    }
    val sortedList = emailDomainDtoList.toList()
    assertTrue(sortedList.any { dto -> dto.domain == "%advancecharity.org.uk" })
  }

  @Test
  fun shouldRetrieveDomainByName(): Unit = runBlocking {
    val retrievedEntity = repository.findByName("%advancecharity.org.uk")

    assertTrue(retrievedEntity != null)
    assertThat(retrievedEntity?.name).isEqualTo("%advancecharity.org.uk")
  }

  @Test
  fun shouldCountDomainMatchesWithWildcard(): Unit = runBlocking {
    val count = repository.countMatching("%advancecharity.org.uk")

    assertTrue(count == 1)
  }

  @Test
  fun shouldCountDomainMatchesForSubdomain(): Unit = runBlocking {
    val count = repository.countMatching("subdomain.advancecharity.org.uk")

    assertTrue(count == 1)
  }

  @Test
  fun shouldCountDomainByNameLikeWithMultipleMatches(): Unit = runBlocking {
    val count = repository.countMatching("digital.justice.gov.uk")

    assertTrue(count == 2)
  }
}
