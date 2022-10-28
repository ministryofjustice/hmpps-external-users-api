package uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Table(name = "USERS")
class User(
  @Column(value = "username")
  private var username: String,

  /**
   * Source of last login information
   */
  @Column(value = "source")
  @Enumerated(EnumType.STRING)
  var source: AuthSource,

  authorities: Set<Authority> = emptySet(),

  groups: Set<Group> = emptySet(),

) : CredentialsContainer {
  @Id
  // @GeneratedValue(generator = "UUID")
  // @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @org.springframework.data.relational.core.mapping.Column(value = "user_id")
  var id: UUID? = null

 /* @OneToMany
  @JoinTable(
    name = "user_role",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "role_id")]
  )*/
  private val authorities: MutableSet<Authority> = authorities.toMutableSet()

  @Column(value = "password")
  private var password: String? = null

  /*@OneToMany
  @JoinTable(
    name = "user_group",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")]
  )*/

  val groups: MutableSet<Group> = groups.toMutableSet()

  val name: String
    get() = username

  override fun eraseCredentials() {
    password = null
  }

  fun getUserName() = username

  override fun toString(): String =
    "User(username='$username',  source=$source, id=$id)"
}
