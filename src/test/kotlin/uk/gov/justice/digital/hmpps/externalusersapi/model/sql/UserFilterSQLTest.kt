package uk.gov.justice.digital.hmpps.externalusersapi.model.sql

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserFilterSQLTest {

  @Test
  fun generateSelectNoFilters() {
    val userFilterSQL = UserFilterSQL()

    assertEquals("select distinct usr.user_id, usr.username, usr.email from users usr", userFilterSQL.generateSelect())
  }

  @Test
  fun generateSelectFirstNameOnlyFilter() {
    val userFilterSQL = UserFilterSQL(name = "alan")

    // assertEquals("select distinct usr.user_id, usr.username, usr.email from users usr", userFilterSQL.generateSelect())
  }
}
