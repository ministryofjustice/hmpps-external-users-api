package uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "user_group")
data class UserGroup(
  @Column(value = "user_id")
  var userId: UUID?,
  @Column(value = "group_id")
  var groupId: UUID?
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UserGroup) return false

    if (userId != other.userId) return false

    return true
  }

  override fun hashCode(): Int = userId.hashCode()
}
