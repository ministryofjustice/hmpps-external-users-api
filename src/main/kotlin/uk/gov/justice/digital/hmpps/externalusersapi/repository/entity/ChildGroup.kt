package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.util.UUID

@Table(name = "child_group")
class ChildGroup(
  @Column(value = "child_group_code")
  override val groupCode: String,
  @Column(value = "child_group_name")
  override var groupName: String,
  @Column(value = "group_id")
  var group: UUID?,
) : GroupIdentity,
  Serializable {
  @Id
  @Column(value = "child_group_id")
  var id: UUID? = null
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChildGroup) return false

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}
