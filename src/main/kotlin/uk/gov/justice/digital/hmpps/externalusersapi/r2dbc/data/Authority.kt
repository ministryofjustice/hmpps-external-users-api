package uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Authority) return false

    if (roleCode != other.roleCode) return false

    return true
  }

  override fun hashCode(): Int = roleCode.hashCode()
}
