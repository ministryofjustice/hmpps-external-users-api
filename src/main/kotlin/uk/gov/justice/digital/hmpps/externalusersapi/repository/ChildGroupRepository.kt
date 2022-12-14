package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import java.util.UUID

interface ChildGroupRepository : CoroutineCrudRepository<ChildGroup, String> {

  suspend fun findByGroupCode(groupCode: String?): ChildGroup?

  suspend fun deleteByGroupCode(groupCode: String)

  fun findAllByGroup(groupId: UUID?): Flow<ChildGroup>
}
