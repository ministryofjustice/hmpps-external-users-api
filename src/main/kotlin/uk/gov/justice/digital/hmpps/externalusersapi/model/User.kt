package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.hibernate.annotations.GenericGenerator
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "USERS")
class User(
  @Column(name = "username", nullable = false)
  private var username: String,

  /**
   * Source of last login information
   */
  @Column(name = "source", nullable = false)
  @Enumerated(EnumType.STRING)
  var source: AuthSource,

  authorities: Set<Authority> = emptySet(),

  groups: Set<Group> = emptySet(),

) : CredentialsContainer {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "user_id", updatable = false, nullable = false)
  var id: UUID? = null

  @OneToMany
  @JoinTable(
    name = "user_role",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "role_id")]
  )
  private val authorities: MutableSet<Authority> = authorities.toMutableSet()

  @Column(name = "password")
  private var password: String? = null

  @OneToMany
  @JoinTable(
    name = "user_group",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")]
  )

  val groups: MutableSet<Group> = groups.toMutableSet()

  val name: String
    get() = username

  override fun eraseCredentials() {
    password = null
  }

  val isMaster: Boolean
    get() = source === AuthSource.auth

  override fun toString(): String =
    "User(username='$username',  source=$source, id=$id)"
}
