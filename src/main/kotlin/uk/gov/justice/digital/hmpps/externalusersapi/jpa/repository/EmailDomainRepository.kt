package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.EmailDomain
import java.util.UUID

interface EmailDomainRepository : CrudRepository<EmailDomain, UUID> {
  fun findByName(name: String): EmailDomain?
}