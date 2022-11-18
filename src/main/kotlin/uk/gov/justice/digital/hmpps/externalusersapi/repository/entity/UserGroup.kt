package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "user_group")
data class UserGroup(
  var userId: UUID?,
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
