package uk.gov.justice.digital.hmpps.externalusersapi.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.EmailDomain
import java.util.UUID

@Repository
interface EmailDomainRepository : CoroutineCrudRepository<EmailDomain, UUID> {
  suspend fun findByName(name: String): EmailDomain?

  @Query(value = "SELECT COUNT(*) FROM email_domain WHERE :domainName like name")
  suspend fun countMatching(domainName: String): Int
}
