package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.GenericGenerator
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.GrantedAuthority
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Converter

@Table(name = "ROLES")
class Authority(
  roleCode: String,
  roleName: String,
  roleDescription: String? = null,
  adminType: List<AdminType> = listOf(),
) : GrantedAuthority {
  @Id
  // @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(value = "role_id") // , updatable = false, nullable = false)
  val id: UUID? = null

  @Column(value = "role_code") // , nullable = false)
  val roleCode: String

  @Column(value = "role_name") // , nullable = false)
  var roleName: String
  override fun getAuthority(): String = "$ROLE_PREFIX$roleCode"

  @Column(value = "role_description")
  var roleDescription: String?

  @Column(value = "admin_type") // , nullable = false)
  @Convert(converter = EnumListConverter::class)
  var adminType: List<AdminType>

  @Column(value = "admin_type") // , nullable = false, updatable = false, insertable = false)
  lateinit var adminTypesAsString: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Authority

    if (roleCode != other.roleCode) return false

    return true
  }

  override fun hashCode(): Int = roleCode.hashCode()

  companion object {
    const val ROLE_PREFIX = "ROLE_"
    fun removeRolePrefixIfNecessary(role: String): String =
      if (StringUtils.startsWith(role, ROLE_PREFIX)) StringUtils.substring(role, ROLE_PREFIX.length) else role
  }

  init {
    this.roleCode = removeRolePrefixIfNecessary(roleCode)
    this.roleName = roleName
    this.roleDescription = roleDescription
    this.adminType = adminType
  }
}

@Converter
class EnumListConverter : AttributeConverter<List<AdminType>, String> {
  override fun convertToDatabaseColumn(stringList: List<AdminType>): String =
    stringList.joinToString(",") { it.adminTypeCode.trim() }

  override fun convertToEntityAttribute(string: String): List<AdminType> {
    return string.split(",").map {
      it.trim()
      AdminType.valueOf(it)
    }
  }
}
