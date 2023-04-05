package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "user_role")
data class UserRole(
  var userId: UUID?,
  var roleId: UUID?,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UserRole) return false

    if (userId != other.userId) return false
    if (roleId != other.roleId) return false

    return true
  }

  override fun hashCode(): Int = userId.hashCode()
}
