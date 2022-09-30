package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group

interface GroupRepository : CrudRepository<Group, String> {

  fun findAllByOrderByGroupName(): List<Group>
  fun findByGroupCode(groupCode: String?): Group?
}
