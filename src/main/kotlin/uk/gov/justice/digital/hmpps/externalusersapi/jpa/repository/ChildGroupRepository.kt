package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup

interface ChildGroupRepository : CrudRepository<ChildGroup, String> {

  fun findByGroupCode(groupCode: String?): ChildGroup?

  fun deleteByGroupCode(groupCode: String?)
}
