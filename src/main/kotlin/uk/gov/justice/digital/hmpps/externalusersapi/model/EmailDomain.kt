package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.hibernate.annotations.GenericGenerator
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "EMAIL_DOMAIN")
data class EmailDomain(
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "email_domain_id", updatable = false, nullable = false)
  val id: UUID? = null,

  @Column(nullable = false)
  val name: String,

  val description: String? = null,
)
