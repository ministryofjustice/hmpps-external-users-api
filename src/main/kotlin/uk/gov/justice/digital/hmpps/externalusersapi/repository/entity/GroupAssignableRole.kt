package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "group_assignable_role")
class GroupAssignableRole(
  @Column(value = "group_id")
  val groupId: UUID?,
  @Column(value = "role_id")
  val roleId: UUID?,
  @Column(value = "automatic")
  val automatic: Boolean = true,
  @Column(value = "create_datetime")
  val createDateTime: LocalDateTime = LocalDateTime.now(),
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GroupAssignableRole) return false

    if (groupId != other.groupId) return false

    return true
  }

  override fun hashCode(): Int = groupId.hashCode()
}
