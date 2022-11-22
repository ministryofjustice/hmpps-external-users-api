package uk.gov.justice.digital.hmpps.externalusersapi.assembler.model

import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import java.util.UUID

class GroupDto(
  val groupCode: String,
  var groupName: String,
  val id: UUID? = null,

) {
  var children: MutableSet<ChildGroup> = mutableSetOf()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GroupDto

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()

  constructor(g: Group, children: MutableSet<ChildGroup>) : this(g.groupCode, g.groupName, g.groupId) {
    this.children = children
  }
}
