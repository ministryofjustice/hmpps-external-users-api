package uk.gov.justice.digital.hmpps.externalusersapi.assembler

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.assembler.model.GroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import java.util.UUID

@Component
@Transactional(readOnly = true)
class GroupDtoAssembler(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository
) {

  suspend fun assembleGroupDtoList(userId: UUID): MutableList<GroupDto> {
    val groups = groupRepository.findGroupsByUserId(userId).toList().toSet()
    val groupWithChildren: MutableList<GroupDto> = mutableListOf()
    groups.forEach { group ->
      groupWithChildren.add(
        GroupDto(
          group,
          childGroupRepository.findAllByGroup(group.groupId).toList().toMutableSet()
        )
      )
    }
    return groupWithChildren
  }
}
