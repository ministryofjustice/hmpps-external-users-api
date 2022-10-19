package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.hibernate.annotations.GenericGenerator
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.util.UUID
import javax.persistence.GeneratedValue

@Table(name = "GROUPS")
class Group(
  @Column(value = "group_code") val groupCode: String,
  @Column(value = "group_name") var groupName: String,
) : Serializable {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(value = "group_id")
  var id: UUID? = null

/*
  @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
  val assignableRoles: MutableSet<GroupAssignableRole> = mutableSetOf()

  @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
  val children: MutableSet<ChildGroup> = mutableSetOf()
*/
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Group

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}
