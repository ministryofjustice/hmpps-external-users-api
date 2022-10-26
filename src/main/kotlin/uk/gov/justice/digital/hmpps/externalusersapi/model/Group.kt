package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "GROUPS")
data class Group(

  val groupCode: String,
  var groupName: String,
) {
  @Id
  var groupId: UUID? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Group

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}
