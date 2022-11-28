package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Table(name = "USERS")
class User(
  private var username: String,

  /**
   * Source of last login information
   */
  @Enumerated(EnumType.STRING)
  var source: AuthSource,

) : CredentialsContainer {
  @Id
  @Column(value = "user_id")
  var id: UUID? = null

  @Column(value = "password")
  private var password: String? = null

  @Column(value = "enabled")
  private var enabled: Boolean = false

  @Column(value = "inactive_reason")
  var inactiveReason: String? = null

  @Column(value = "email")
  var email: String? = null

  @Column(value = "first_name")
  var first_Name: String? = null

  @Column(value = "last_logged_in")
  var lastLoggedIn: LocalDateTime = LocalDateTime.now()

  val name: String
    get() = username

  val firstName: String
    get() = first_Name ?: username

  override fun eraseCredentials() {
    password = null
  }

  fun getUsername(): String = username

  fun isEnabled(): Boolean = enabled

  fun setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }
  override fun toString(): String =
    "User(username='$username',  source=$source, id=$id, enabled='$enabled')"
}
