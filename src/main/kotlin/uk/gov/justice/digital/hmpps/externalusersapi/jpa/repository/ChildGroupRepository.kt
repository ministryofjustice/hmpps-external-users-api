package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup

interface ChildGroupRepository : CoroutineCrudRepository<ChildGroup, String> {

  suspend fun findByGroupCode(groupCode: String?): ChildGroup?

  suspend fun deleteByGroupCode(groupCode: String?)
}
