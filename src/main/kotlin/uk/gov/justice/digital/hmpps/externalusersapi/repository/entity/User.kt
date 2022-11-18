package uk.gov.justice.digital.hmpps.externalusersapi.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
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

  val name: String
    get() = username

  override fun eraseCredentials() {
    password = null
  }

  fun getUserName() = username

  override fun toString(): String =
    "User(username='$username',  source=$source, id=$id)"
}
