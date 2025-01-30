package uk.gov.justice.digital.hmpps.externalusersapi.security

import com.fasterxml.jackson.annotation.JsonValue

enum class AuthSource(val description: String) {
  @Suppress("ktlint:standard:enum-entry-name-case")
  auth("External"),

  @Suppress("ktlint:standard:enum-entry-name-case")
  azuread("Microsoft Azure"),

  @Suppress("ktlint:standard:enum-entry-name-case")
  delius("Delius"),

  @Suppress("ktlint:standard:enum-entry-name-case")
  nomis("DPS"),

  @Suppress("ktlint:standard:enum-entry-name-case")
  none("None"),
  ;

  @JsonValue
  val source: String = name

  companion object {
    @JvmStatic
    fun fromNullableString(source: String?): AuthSource = source?.let { valueOf(source.lowercase()) } ?: none

    fun getSourceLegacyName(source: String?): String? = when (fromNullableString(source)) {
      delius -> "nDelius"
      nomis -> "NOMIS"
      else -> null
    }
  }
}
