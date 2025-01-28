package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.apache.commons.lang3.StringUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "ROLES")
data class Authority(
  @Id
  @Column(value = "role_id")
  val id: UUID? = null,

  val roleCode: String,
  var roleName: String,
  var roleDescription: String? = null,
  var adminType: String,
) {

  companion object {
    const val ROLE_PREFIX = "ROLE_"
    fun removeRolePrefixIfNecessary(role: String): String = if (StringUtils.startsWith(role, ROLE_PREFIX)) StringUtils.substring(role, ROLE_PREFIX.length) else role
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Authority) return false

    if (roleCode != other.roleCode) return false

    return true
  }

  override fun hashCode(): Int = roleCode.hashCode()
}
