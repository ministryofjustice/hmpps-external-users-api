package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(name = "EMAIL_DOMAIN")
data class EmailDomain(
  @Id
  @Column(value = "email_domain_id")
  val id: UUID? = null,

  val name: String,

  val description: String? = null,
)
