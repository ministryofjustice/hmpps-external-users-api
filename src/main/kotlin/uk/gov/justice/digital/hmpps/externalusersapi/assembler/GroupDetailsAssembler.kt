package uk.gov.justice.digital.hmpps.externalusersapi.assembler

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupAssignableRoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.GroupDetails
import uk.gov.justice.digital.hmpps.externalusersapi.service.GroupNotFoundException

@Component
@Transactional(readOnly = true)
class GroupDetailsAssembler(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val groupAssignableRoleRepository: GroupAssignableRoleRepository,
) {

  @Throws(GroupNotFoundException::class)
  suspend fun assembleGroupDetail(groupCode: String): GroupDetails =
    groupRepository.findByGroupCode(groupCode)
      ?.let {
        val children = childGroupRepository.findAllByGroup(it.groupId).toList()
        val assignableRole = groupAssignableRoleRepository.findGroupAssignableRoleByGroupCode(it.groupCode).toList()
        GroupDetails(it, children, assignableRole)
      }
      ?: throw GroupNotFoundException("get", groupCode, "notfound")
}
