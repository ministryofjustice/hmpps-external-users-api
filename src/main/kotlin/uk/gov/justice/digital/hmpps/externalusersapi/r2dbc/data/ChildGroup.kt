package uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.util.UUID

@Table(name = "CHILD_GROUP")
class ChildGroup(
  @Column(value = "child_group_code")
  val groupCode: String,
  @Column(value = "child_group_name")
  var groupName: String,
  @Column(value = "group_id")
  var group: UUID?
) : Serializable {
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
