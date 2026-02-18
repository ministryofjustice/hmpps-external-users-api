package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "groups")
data class Group(

  @Column(value = "group_code")
  override val groupCode: String,
  @Column(value = "group_name")
  override var groupName: String,
  @Id
  @Column(value = "group_id")
  var groupId: UUID? = null,
) : GroupIdentity {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Group

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}
