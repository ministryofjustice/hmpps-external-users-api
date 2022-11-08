package uk.gov.justice.digital.hmpps.externalusersapi.model.or

import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.ChildGroup
import java.util.UUID

class GroupORModel(
  val groupCode: String,
  var groupName: String,
  val id: UUID? = null,

) {
  var children: MutableSet<ChildGroup> = mutableSetOf()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GroupORModel

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()

  constructor(g: Group) : this(g.groupCode, g.groupName, g.groupId)
  constructor(g: Group, children: MutableSet<ChildGroup>) : this(g.groupCode, g.groupName, g.groupId) {
    this.children = children
  }
}
