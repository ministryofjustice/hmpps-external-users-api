package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.hibernate.annotations.GenericGenerator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.util.UUID
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Table(name = "CHILD_GROUP")
// @IdClass(ChildGroup.GroupChildGroupId::class)
class ChildGroup(
  @Column(value = "child_group_code")
  val groupCode: String,
  @Column(value = "child_group_name")
  var groupName: String,
) : Serializable {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(value = "child_group_id")
  var id: UUID? = null

  /*
  @Id
  @ManyToOne
  @JoinColumn(name = "group_id")
  var group: Group? = null

  class GroupChildGroupId : Serializable {
    private val id: UUID? = null
    private val group: UUID? = null
  }
*/
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChildGroup) return false

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}
