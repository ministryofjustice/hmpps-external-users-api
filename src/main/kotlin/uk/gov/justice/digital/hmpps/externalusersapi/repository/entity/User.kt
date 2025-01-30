package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.time.LocalDateTime
import java.util.UUID

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

  @Column(value = "email")
  var email: String? = null

  @Column(value = "first_name")
  private var firstName: String? = null

  @Column(value = "last_name")
  var lastName: String? = null

  @Column(value = "enabled")
  private var enabled: Boolean = false

  @Column(value = "locked")
  var locked = false

  @Column(value = "verified")
  var verified: Boolean = false

  @Column(value = "last_logged_in")
  var lastLoggedIn: LocalDateTime = LocalDateTime.now()

  @Column(value = "inactive_reason")
  var inactiveReason: String? = null

  override fun eraseCredentials() {
    password = null
  }

  val name: String
    get() = username

  fun getUserName() = username

  fun setUsername(username: String) {
    this.username = username.uppercase()
  }

  fun setFirstName(firstName: String) {
    this.firstName = firstName
  }

  fun getFirstName() = firstName ?: username

  fun isEnabled(): Boolean = enabled

  fun setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }

  fun setDisabled(disabled: Boolean) {
    this.enabled = disabled
  }

  fun hasPassword() = password != null

  override fun toString(): String = "User(username='$username',  source=$source, id=$id, enabled='$enabled', lastLoggedIn=$lastLoggedIn, inactiveReason=$inactiveReason, email=$email)"
}
